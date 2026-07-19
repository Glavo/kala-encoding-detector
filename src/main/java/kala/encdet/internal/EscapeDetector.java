// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/// Detects ISO-2022, HZ-GB-2312, and UTF-7 escape structures.
@NotNullByDefault
final class EscapeDetector {
    /// Deterministic confidence used by escape-sequence detection.
    private static final double CONFIDENCE = 0.95;

    /// UTF-7 Base64 lookup; `-1` denotes a non-Base64 byte.
    private static final int @Unmodifiable [] BASE64_VALUES = createBase64Values();

    /// Prevents instantiation of this static stage.
    private EscapeDetector() {
    }

    /// Detects one escape-based encoding from distinctive shift sequences.
    ///
    /// @param data bytes to examine
    /// @return a deterministic result, or `null`
    static @Nullable PipelineResult detect(
            @UnmodifiableView ByteBuffer data
    ) {
        boolean hasEscape = containsByte(data, 0x1b);
        boolean hasTilde = containsByte(data, '~');
        boolean hasPlus = containsByte(data, '+');
        if (!hasEscape && !hasTilde && !hasPlus) {
            return null;
        }

        if (hasEscape) {
            if (containsAscii(data, "\u001b$(O")
                    || containsAscii(data, "\u001b$(P")
                    || containsAscii(data, "\u001b$(Q")) {
                return result("iso2022_jp_2004", "ja");
            }
            if (containsAscii(data, "\u001b(I")) {
                return result("iso2022_jp_ext", "ja");
            }
            if (containsAscii(data, "\u001b$B")
                    || containsAscii(data, "\u001b$@")
                    || containsAscii(data, "\u001b(J")
                    || containsAscii(data, "\u001b$(D")) {
                if (containsByte(data, 0x0e) && containsByte(data, 0x0f)) {
                    return result("iso2022_jp_ext", "ja");
                }
                return result("iso2022_jp_2", "ja");
            }
            if (containsAscii(data, "\u001b$)C")) {
                return result("iso2022_kr", "ko");
            }
        }

        if (hasTilde
                && containsAscii(data, "~{")
                && containsAscii(data, "~}")
                && hasValidHzRegion(data)) {
            return result("hz", "zh");
        }

        if (hasPlus && isSevenBit(data) && hasValidUtf7Sequence(data)) {
            return result("utf-7", null);
        }
        return null;
    }

    /// Creates a deterministic escape result.
    ///
    /// @param encoding canonical encoding
    /// @param language fixed language, or `null`
    /// @return result
    private static PipelineResult result(
            String encoding,
            @Nullable String language
    ) {
        return new PipelineResult(encoding, CONFIDENCE, language, null);
    }

    /// Finds a nonempty even HZ region containing only GB2312 graphic bytes.
    ///
    /// @param data bytes to scan
    /// @return whether a structurally valid region exists
    private static boolean hasValidHzRegion(@UnmodifiableView ByteBuffer data) {
        byte[] opening = asciiBytes("~{");
        byte[] closing = asciiBytes("~}");
        int start = 0;
        while (true) {
            int begin = indexOf(data, opening, start);
            if (begin < 0) {
                return false;
            }
            int end = indexOf(data, closing, begin + 2);
            if (end < 0) {
                return false;
            }
            int length = end - begin - 2;
            if (length >= 2 && (length & 1) == 0) {
                boolean valid = true;
                for (int index = begin + 2; index < end; index++) {
                    int value = Byte.toUnsignedInt(data.get(index));
                    if (value < 0x21 || value > 0x7e) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    return true;
                }
            }
            start = end + 2;
        }
    }

    /// Finds at least one heuristic-safe valid UTF-7 shifted sequence.
    ///
    /// @param data seven-bit bytes
    /// @return whether a valid shifted sequence exists
    private static boolean hasValidUtf7Sequence(@UnmodifiableView ByteBuffer data) {
        int start = 0;
        while (true) {
            int shift = indexOfByte(data, '+', start);
            if (shift < 0) {
                return false;
            }
            int position = shift + 1;
            if (position < data.remaining() && data.get(position) == '-') {
                start = position + 1;
                continue;
            }
            if (position < data.remaining() && data.get(position) == '+') {
                while (position < data.remaining() && data.get(position) == '+') {
                    position++;
                }
                start = position;
                continue;
            }
            if (isEmbeddedInBase64(data, shift)) {
                start = position;
                continue;
            }

            int end = position;
            while (end < data.remaining() && base64Value(data.get(end)) >= 0) {
                end++;
            }
            int length = end - position;
            boolean uppercase = false;
            for (int index = position; index < end; index++) {
                int value = Byte.toUnsignedInt(data.get(index));
                if (value >= 'A' && value <= 'Z') {
                    uppercase = true;
                    break;
                }
            }
            if (length >= 3 && !uppercase) {
                start = end;
                continue;
            }
            if (length >= 3 && isValidUtf7Base64(data, position, end)) {
                return true;
            }
            start = Math.max(position, end);
        }
    }

    /// Validates a UTF-7 Base64 block using the reference padding semantics.
    ///
    /// @param data  containing bytes
    /// @param start first Base64 byte
    /// @param end   exclusive Base64 end
    /// @return whether it decodes to well-formed UTF-16BE
    private static boolean isValidUtf7Base64(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end
    ) {
        int length = end - start;
        int totalBits = length * 6;
        int paddingBits = totalBits % 16;
        if (paddingBits > 0) {
            int last = base64Value(data.get(end - 1));
            int mask = (1 << paddingBits) - 1;
            if ((last & mask) != 0) {
                return false;
            }
        }

        int byteCount = totalBits / 8;
        byte[] raw = new byte[byteCount];
        int bitBuffer = 0;
        int bitCount = 0;
        int output = 0;
        for (int index = start; index < end; index++) {
            bitBuffer = (bitBuffer << 6) | base64Value(data.get(index));
            bitCount += 6;
            if (bitCount >= 8) {
                bitCount -= 8;
                if (output < raw.length) {
                    raw[output++] = (byte) ((bitBuffer >>> bitCount) & 0xff);
                }
                if (bitCount == 0) {
                    bitBuffer = 0;
                } else {
                    bitBuffer &= (1 << bitCount) - 1;
                }
            }
        }

        boolean previousHigh = false;
        for (int index = 0; index < byteCount - 1; index += 2) {
            int unit = (Byte.toUnsignedInt(raw[index]) << 8)
                    | Byte.toUnsignedInt(raw[index + 1]);
            if (unit >= 0xd800 && unit <= 0xdbff) {
                if (previousHigh) {
                    return false;
                }
                previousHigh = true;
            } else if (unit >= 0xdc00 && unit <= 0xdfff) {
                if (!previousHigh) {
                    return false;
                }
                previousHigh = false;
            } else {
                if (previousHigh) {
                    return false;
                }
            }
        }
        return !previousHigh;
    }

    /// Reports whether a plus is preceded by at least four Base64 characters.
    ///
    /// CR and LF are skipped while walking backward.
    ///
    /// @param data     source bytes
    /// @param position plus-sign index
    /// @return whether it appears embedded in Base64 data
    private static boolean isEmbeddedInBase64(
            @UnmodifiableView ByteBuffer data,
            int position
    ) {
        int count = 0;
        for (int index = position - 1; index >= 0; index--) {
            int value = Byte.toUnsignedInt(data.get(index));
            if (value == '\n' || value == '\r') {
                continue;
            }
            if (base64Value(data.get(index)) >= 0 || value == '=') {
                count++;
            } else {
                break;
            }
        }
        return count >= 4;
    }

    /// Tests whether every byte has its high bit clear.
    ///
    /// @param data bytes to test
    /// @return whether all bytes are seven-bit
    private static boolean isSevenBit(@UnmodifiableView ByteBuffer data) {
        for (int index = 0; index < data.remaining(); index++) {
            if (data.get(index) < 0) {
                return false;
            }
        }
        return true;
    }

    /// Tests whether a byte value occurs.
    ///
    /// @param data     source bytes
    /// @param expected unsigned byte
    /// @return whether it occurs
    private static boolean containsByte(@UnmodifiableView ByteBuffer data, int expected) {
        return indexOfByte(data, expected, 0) >= 0;
    }

    /// Finds an unsigned byte from a starting index.
    ///
    /// @param data     source bytes
    /// @param expected unsigned byte
    /// @param start    first index
    /// @return matching index, or `-1`
    private static int indexOfByte(
            @UnmodifiableView ByteBuffer data,
            int expected,
            int start
    ) {
        for (int index = Math.max(0, start); index < data.remaining(); index++) {
            if (Byte.toUnsignedInt(data.get(index)) == expected) {
                return index;
            }
        }
        return -1;
    }

    /// Tests whether byte-preserving ASCII text occurs.
    ///
    /// @param data     source bytes
    /// @param expected expected text
    /// @return whether it occurs
    private static boolean containsAscii(@UnmodifiableView ByteBuffer data, String expected) {
        return indexOf(data, asciiBytes(expected), 0) >= 0;
    }

    /// Finds a byte pattern.
    ///
    /// @param data    source bytes
    /// @param pattern pattern bytes
    /// @param start   first candidate index
    /// @return matching index, or `-1`
    private static int indexOf(
            @UnmodifiableView ByteBuffer data,
            byte @Unmodifiable [] pattern,
            int start
    ) {
        int last = data.remaining() - pattern.length;
        for (int index = Math.max(0, start); index <= last; index++) {
            boolean match = true;
            for (int patternIndex = 0; patternIndex < pattern.length; patternIndex++) {
                if (data.get(index + patternIndex) != pattern[patternIndex]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return index;
            }
        }
        return -1;
    }

    /// Encodes byte-preserving ASCII constants.
    ///
    /// @param value source text
    /// @return encoded bytes
    private static byte @Unmodifiable [] asciiBytes(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }

    /// Returns a UTF-7 Base64 sextet value.
    ///
    /// @param value encoded byte
    /// @return value in `[0, 63]`, or `-1`
    private static int base64Value(byte value) {
        return BASE64_VALUES[Byte.toUnsignedInt(value)];
    }

    /// Creates a UTF-7 Base64 lookup table.
    ///
    /// @return immutable lookup array
    private static int @Unmodifiable [] createBase64Values() {
        int[] values = new int[256];
        java.util.Arrays.fill(values, -1);
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        for (int index = 0; index < alphabet.length(); index++) {
            values[alphabet.charAt(index)] = index;
        }
        return values;
    }
}
