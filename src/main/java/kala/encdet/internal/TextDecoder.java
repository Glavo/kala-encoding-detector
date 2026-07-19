// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// Decodes model-language inputs without relying on installed charset providers.
@NotNullByDefault
final class TextDecoder {
    /// Generated single-byte decode-map resource.
    private static final String RESOURCE = "/kala/encdet/internal/single-byte-decode.bin";

    /// Decode-map resource magic.
    private static final int MAGIC = 0x4b444d31;

    /// Expected generated single-byte table count.
    private static final int TABLE_COUNT = 64;

    /// Number of entries in one single-byte table.
    private static final int TABLE_SIZE = 256;

    /// UTF-7 Base64 lookup; `-1` denotes a non-Base64 byte.
    private static final int @Unmodifiable [] BASE64_VALUES = createBase64Values();

    /// Prevents instantiation of this static decoder.
    private TextDecoder() {
    }

    /// Decodes bytes and re-encodes them as UTF-8 for language scoring.
    ///
    /// Invalid residual sequences are ignored. UTF-8 input is returned as-is,
    /// matching the reference language-scoring shortcut.
    ///
    /// @param data     bytes to decode
    /// @param encoding canonical source encoding
    /// @return UTF-8 bytes, or `null` when no decoder is implemented
    static byte @Nullable [] toUtf8(byte @Unmodifiable [] data, String encoding) {
        if (encoding.equals("utf-8")) {
            return data;
        }
        int @Nullable [] table = TablesHolder.TABLES.get(encoding);
        if (table != null) {
            return decodeSingleByte(data, table).getBytes(StandardCharsets.UTF_8);
        }
        String text = switch (encoding) {
            case "utf-8-sig" -> decodeUtf8(data, startsWith(data, 0xef, 0xbb, 0xbf) ? 3 : 0);
            case "utf-16" -> decodeUtf16WithBom(data);
            case "utf-16-be" -> decodeUtf16(data, false, 0);
            case "utf-16-le" -> decodeUtf16(data, true, 0);
            case "utf-32" -> decodeUtf32WithBom(data);
            case "utf-32-be" -> decodeUtf32(data, false, 0);
            case "utf-32-le" -> decodeUtf32(data, true, 0);
            case "utf-7" -> decodeUtf7(data);
            default -> null;
        };
        return text == null ? null : text.getBytes(StandardCharsets.UTF_8);
    }

    /// Decodes one provider-independent single-byte table.
    ///
    /// @param data  bytes to decode
    /// @param table byte-to-code-point map, using `-1` for undefined bytes
    /// @return decoded text with undefined bytes omitted
    private static String decodeSingleByte(byte @Unmodifiable [] data, int @Unmodifiable [] table) {
        StringBuilder result = new StringBuilder(data.length);
        for (byte value : data) {
            int codePoint = table[Byte.toUnsignedInt(value)];
            if (codePoint >= 0) {
                result.appendCodePoint(codePoint);
            }
        }
        return result.toString();
    }

    /// Decodes UTF-8 while ignoring malformed sequences.
    ///
    /// @param data   bytes to decode
    /// @param offset first byte after an optional signature
    /// @return decoded text
    private static String decodeUtf8(byte @Unmodifiable [] data, int offset) {
        StringBuilder result = new StringBuilder(data.length - offset);
        for (int index = offset; index < data.length; ) {
            int first = Byte.toUnsignedInt(data[index]);
            if (first < 0x80) {
                result.append((char) first);
                index++;
                continue;
            }
            int length;
            int codePoint;
            if (between(first, 0xc2, 0xdf)) {
                length = 2;
                codePoint = first & 0x1f;
            } else if (between(first, 0xe0, 0xef)) {
                length = 3;
                codePoint = first & 0x0f;
            } else if (between(first, 0xf0, 0xf4)) {
                length = 4;
                codePoint = first & 0x07;
            } else {
                index++;
                continue;
            }
            if (index + length > data.length) {
                break;
            }
            boolean valid = true;
            for (int continuation = 1; continuation < length; continuation++) {
                int value = Byte.toUnsignedInt(data[index + continuation]);
                if (!between(value, 0x80, 0xbf)) {
                    valid = false;
                    break;
                }
                codePoint = (codePoint << 6) | (value & 0x3f);
            }
            int second = Byte.toUnsignedInt(data[index + 1]);
            if (!valid
                    || (first == 0xe0 && second < 0xa0)
                    || (first == 0xed && second > 0x9f)
                    || (first == 0xf0 && second < 0x90)
                    || (first == 0xf4 && second > 0x8f)) {
                index++;
                continue;
            }
            result.appendCodePoint(codePoint);
            index += length;
        }
        return result.toString();
    }

    /// Decodes BOM-sensitive UTF-16, returning an empty string without a BOM.
    ///
    /// @param data bytes to decode
    /// @return decoded text
    private static String decodeUtf16WithBom(byte @Unmodifiable [] data) {
        if (startsWith(data, 0xfe, 0xff)) {
            return decodeUtf16(data, false, 2);
        }
        if (startsWith(data, 0xff, 0xfe)) {
            return decodeUtf16(data, true, 2);
        }
        return "";
    }

    /// Decodes UTF-16 while omitting malformed surrogate units.
    ///
    /// @param data         bytes to decode
    /// @param littleEndian whether code units are little-endian
    /// @param offset       first payload byte
    /// @return decoded text
    private static String decodeUtf16(
            byte @Unmodifiable [] data,
            boolean littleEndian,
            int offset
    ) {
        StringBuilder result = new StringBuilder((data.length - offset) / 2);
        for (int index = offset; index + 1 < data.length; index += 2) {
            int unit = readUnsignedShort(data, index, littleEndian);
            if (between(unit, 0xd800, 0xdbff)) {
                if (index + 3 < data.length) {
                    int low = readUnsignedShort(data, index + 2, littleEndian);
                    if (between(low, 0xdc00, 0xdfff)) {
                        result.appendCodePoint(
                                0x10000 + ((unit - 0xd800) << 10) + (low - 0xdc00)
                        );
                        index += 2;
                    }
                }
            } else if (!between(unit, 0xdc00, 0xdfff)) {
                result.append((char) unit);
            }
        }
        return result.toString();
    }

    /// Decodes BOM-sensitive UTF-32, returning an empty string without a BOM.
    ///
    /// @param data bytes to decode
    /// @return decoded text
    private static String decodeUtf32WithBom(byte @Unmodifiable [] data) {
        if (startsWith(data, 0x00, 0x00, 0xfe, 0xff)) {
            return decodeUtf32(data, false, 4);
        }
        if (startsWith(data, 0xff, 0xfe, 0x00, 0x00)) {
            return decodeUtf32(data, true, 4);
        }
        return "";
    }

    /// Decodes UTF-32 while omitting non-scalar values and trailing bytes.
    ///
    /// @param data         bytes to decode
    /// @param littleEndian whether units are little-endian
    /// @param offset       first payload byte
    /// @return decoded text
    private static String decodeUtf32(
            byte @Unmodifiable [] data,
            boolean littleEndian,
            int offset
    ) {
        StringBuilder result = new StringBuilder((data.length - offset) / 4);
        for (int index = offset; index + 3 < data.length; index += 4) {
            long value = readUnsignedInt(data, index, littleEndian);
            if (value <= 0x10ffffL && !between(value, 0xd800L, 0xdfffL)) {
                result.appendCodePoint((int) value);
            }
        }
        return result.toString();
    }

    /// Decodes UTF-7 shifted Base64 regions while ignoring malformed shifts.
    ///
    /// @param data bytes to decode
    /// @return decoded text
    private static String decodeUtf7(byte @Unmodifiable [] data) {
        StringBuilder result = new StringBuilder(data.length);
        int index = 0;
        while (index < data.length) {
            int value = Byte.toUnsignedInt(data[index]);
            if (value != '+' || value > 0x7f) {
                if (value <= 0x7f) {
                    result.append((char) value);
                }
                index++;
                continue;
            }
            if (index + 1 < data.length && data[index + 1] == '-') {
                result.append('+');
                index += 2;
                continue;
            }
            int start = ++index;
            while (index < data.length && base64Value(data[index]) >= 0) {
                index++;
            }
            appendUtf7Payload(result, data, start, index);
            if (index < data.length && data[index] == '-') {
                index++;
            }
        }
        return result.toString();
    }

    /// Appends all complete well-formed UTF-16BE units in one UTF-7 payload.
    ///
    /// @param output destination text
    /// @param data   containing bytes
    /// @param start  first Base64 byte
    /// @param end    exclusive Base64 end
    private static void appendUtf7Payload(
            StringBuilder output,
            byte @Unmodifiable [] data,
            int start,
            int end
    ) {
        long accumulator = 0L;
        int accumulated = 0;
        int pendingHigh = -1;
        for (int index = start; index < end; index++) {
            accumulator = (accumulator << 6) | base64Value(data[index]);
            accumulated += 6;
            while (accumulated >= 16) {
                accumulated -= 16;
                int unit = (int) ((accumulator >>> accumulated) & 0xffffL);
                if (between(unit, 0xd800, 0xdbff)) {
                    pendingHigh = unit;
                } else if (between(unit, 0xdc00, 0xdfff)) {
                    if (pendingHigh >= 0) {
                        output.appendCodePoint(
                                0x10000 + ((pendingHigh - 0xd800) << 10) + (unit - 0xdc00)
                        );
                        pendingHigh = -1;
                    }
                } else {
                    pendingHigh = -1;
                    output.append((char) unit);
                }
                if (accumulated == 0) {
                    accumulator = 0L;
                } else {
                    accumulator &= (1L << accumulated) - 1L;
                }
            }
        }
    }

    /// Reads an unsigned UTF-16 code unit.
    ///
    /// @param data         source bytes
    /// @param offset       unit offset
    /// @param littleEndian byte order
    /// @return value in `[0, 65535]`
    private static int readUnsignedShort(
            byte @Unmodifiable [] data,
            int offset,
            boolean littleEndian
    ) {
        int first = Byte.toUnsignedInt(data[offset]);
        int second = Byte.toUnsignedInt(data[offset + 1]);
        return littleEndian ? first | (second << 8) : (first << 8) | second;
    }

    /// Reads an unsigned UTF-32 unit.
    ///
    /// @param data         source bytes
    /// @param offset       unit offset
    /// @param littleEndian byte order
    /// @return unsigned 32-bit value
    private static long readUnsignedInt(
            byte @Unmodifiable [] data,
            int offset,
            boolean littleEndian
    ) {
        int value;
        if (littleEndian) {
            value = Byte.toUnsignedInt(data[offset])
                    | (Byte.toUnsignedInt(data[offset + 1]) << 8)
                    | (Byte.toUnsignedInt(data[offset + 2]) << 16)
                    | (Byte.toUnsignedInt(data[offset + 3]) << 24);
        } else {
            value = (Byte.toUnsignedInt(data[offset]) << 24)
                    | (Byte.toUnsignedInt(data[offset + 1]) << 16)
                    | (Byte.toUnsignedInt(data[offset + 2]) << 8)
                    | Byte.toUnsignedInt(data[offset + 3]);
        }
        return Integer.toUnsignedLong(value);
    }

    /// Tests an unsigned prefix.
    ///
    /// @param data   bytes to inspect
    /// @param prefix unsigned prefix bytes
    /// @return whether the prefix matches
    private static boolean startsWith(byte @Unmodifiable [] data, int @Unmodifiable ... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (Byte.toUnsignedInt(data[index]) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    /// Tests an inclusive integer range.
    ///
    /// @param value   value to test
    /// @param minimum inclusive minimum
    /// @param maximum inclusive maximum
    /// @return whether the value lies in the range
    private static boolean between(int value, int minimum, int maximum) {
        return value >= minimum && value <= maximum;
    }

    /// Tests an inclusive long range.
    ///
    /// @param value   value to test
    /// @param minimum inclusive minimum
    /// @param maximum inclusive maximum
    /// @return whether the value lies in the range
    private static boolean between(long value, long minimum, long maximum) {
        return value >= minimum && value <= maximum;
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

    /// Loads generated single-byte decode tables.
    ///
    /// @return immutable canonical-name map
    private static @Unmodifiable Map<String, int @Unmodifiable []> loadTables() {
        @Nullable InputStream input = TextDecoder.class.getResourceAsStream(RESOURCE);
        if (input == null) {
            throw new IllegalStateException("Missing single-byte decode resource: " + RESOURCE);
        }
        byte[] raw;
        try (input) {
            raw = input.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read single-byte decode resource", exception);
        }
        ByteBuffer buffer = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);
        requireRemaining(buffer, Integer.BYTES + Short.BYTES);
        if (buffer.getInt() != MAGIC) {
            throw corrupt("missing KDM1 magic");
        }
        int count = Short.toUnsignedInt(buffer.getShort());
        if (count != TABLE_COUNT) {
            throw corrupt("expected " + TABLE_COUNT + " tables but found " + count);
        }
        LinkedHashMap<String, int @Unmodifiable []> result = new LinkedHashMap<>();
        for (int tableIndex = 0; tableIndex < count; tableIndex++) {
            requireRemaining(buffer, 1);
            int nameLength = Byte.toUnsignedInt(buffer.get());
            requireRemaining(buffer, nameLength + TABLE_SIZE * Integer.BYTES);
            byte[] nameBytes = new byte[nameLength];
            buffer.get(nameBytes);
            String name = new String(nameBytes, StandardCharsets.US_ASCII);
            int[] table = new int[TABLE_SIZE];
            for (int index = 0; index < TABLE_SIZE; index++) {
                int codePoint = buffer.getInt();
                if (codePoint < -1 || codePoint > Character.MAX_CODE_POINT) {
                    throw corrupt("invalid code point for " + name);
                }
                table[index] = codePoint;
            }
            if (result.putIfAbsent(name, table) != null) {
                throw corrupt("duplicate table " + name);
            }
        }
        if (buffer.hasRemaining()) {
            throw corrupt("unexpected trailing bytes");
        }
        return Collections.unmodifiableMap(result);
    }

    /// Checks remaining bytes while parsing the decode resource.
    ///
    /// @param buffer   resource buffer
    /// @param required minimum remaining bytes
    private static void requireRemaining(ByteBuffer buffer, int required) {
        if (required < 0 || buffer.remaining() < required) {
            throw corrupt("truncated table");
        }
    }

    /// Creates a decode-resource corruption exception.
    ///
    /// @param detail corruption detail
    /// @return exception
    private static IllegalStateException corrupt(String detail) {
        return new IllegalStateException("Malformed single-byte decode resource: " + detail);
    }

    /// Initialization-on-demand holder for generated decode maps.
    @NotNullByDefault
    private static final class TablesHolder {
        /// Canonical single-byte decode tables.
        private static final @Unmodifiable Map<String, int @Unmodifiable []> TABLES = loadTables();

        /// Prevents holder instantiation.
        private TablesHolder() {
        }
    }
}
