// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.EncodingDetector.Encoding;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/// Reproduces strict codec byte-validity filtering without charset providers.
@NotNullByDefault
final class ByteValidity {
    /// Stateless multibyte validity table resource.
    private static final String MULTIBYTE_RESOURCE = "/kala/encdet/internal/multibyte-validity.bin";

    /// HZ shifted-pair validity resource.
    private static final String HZ_RESOURCE = "/kala/encdet/internal/hz-validity.bin";

    /// Multibyte resource magic.
    private static final int MULTIBYTE_MAGIC = 0x4b564d31;

    /// Expected number of stateless multibyte tables.
    private static final int MULTIBYTE_TABLE_COUNT = 8;

    /// Bytes in a 256-bit byte mask.
    private static final int BYTE_MASK_SIZE = 32;

    /// Bytes in a 65,536-bit pair mask.
    private static final int PAIR_MASK_SIZE = 8192;

    /// Number of syntactically possible GB18030 four-byte pointers.
    private static final int GB18030_POINTER_COUNT = 126 * 10 * 126 * 10;

    /// Prevents instantiation of this static stage.
    private ByteValidity() {
    }

    /// Filters candidates to those that strictly accept the complete input.
    ///
    /// @param data       bytes to validate
    /// @param candidates registry candidates in detection order
    /// @return immutable surviving candidates
    static @Unmodifiable List<EncodingRegistry.Info> filter(
            @UnmodifiableView ByteBuffer data,
            List<EncodingRegistry.Info> candidates
    ) {
        if (!data.hasRemaining()) {
            return candidates;
        }
        ArrayList<EncodingRegistry.Info> valid = new ArrayList<>(candidates.size());
        for (EncodingRegistry.Info candidate : candidates) {
            if (isValid(data, candidate.encoding())) {
                valid.add(candidate);
            }
        }
        return List.copyOf(valid);
    }

    /// Tests strict validity for one canonical encoding.
    ///
    /// @param data     complete bytes to validate
    /// @param encoding encoding to test
    /// @return whether the encoding accepts all bytes and final state
    static boolean isValid(@UnmodifiableView ByteBuffer data, Encoding encoding) {
        if (!data.hasRemaining()) {
            return true;
        }
        return switch (encoding) {
            case UTF_8 -> isValidUtf8(data, 0);
            case UTF_8_SIG -> isValidUtf8(data, startsWith(data, 0xef, 0xbb, 0xbf) ? 3 : 0);
            case UTF_16 -> isValidUtf16WithBom(data);
            case UTF_16_BE -> isValidUtf16(data, false, 0);
            case UTF_16_LE -> isValidUtf16(data, true, 0);
            case UTF_32 -> isValidUtf32WithBom(data);
            case UTF_32_BE -> isValidUtf32(data, false, 0);
            case UTF_32_LE -> isValidUtf32(data, true, 0);
            case UTF_7 -> isValidUtf7(data);
            case HZ -> isValidHz(data);
            case ISO_2022_JP_2, ISO_2022_JP_2004, ISO_2022_JP_EXT, ISO_2022_KR ->
                    isValidIso2022(data);
            default -> {
                @Nullable ByteSet singleMask = SingleByteValidity.lookup(encoding);
                if (singleMask != null) {
                    yield allBytesInSet(data, singleMask);
                }
                @Nullable MultibyteTable table = TablesHolder.MULTIBYTE_TABLES.get(encoding);
                yield table != null && isValidStatelessMultibyte(data, encoding, table);
            }
        };
    }

    /// Validates a table-driven stateless multibyte codec.
    ///
    /// @param data     bytes to validate
    /// @param encoding encoding being validated
    /// @param table    codec tables
    /// @return whether every byte is consumed by a valid sequence
    private static boolean isValidStatelessMultibyte(
            @UnmodifiableView ByteBuffer data,
            Encoding encoding,
            MultibyteTable table
    ) {
        int index = 0;
        while (index < data.remaining()) {
            int first = Byte.toUnsignedInt(data.get(index));
            if (contains(table.singles(), first)) {
                index++;
                continue;
            }

            if (encoding == Encoding.EUC_JIS_2004 && first == 0x8f) {
                if (index + 2 >= data.remaining() || table.extra() == null) {
                    return false;
                }
                int pair = (Byte.toUnsignedInt(data.get(index + 1)) << 8)
                        | Byte.toUnsignedInt(data.get(index + 2));
                if (!contains(table.extra(), pair)) {
                    return false;
                }
                index += 3;
                continue;
            }

            if (encoding == Encoding.GB18030
                    && index + 1 < data.remaining()
                    && between(Byte.toUnsignedInt(data.get(index + 1)), 0x30, 0x39)) {
                if (index + 3 >= data.remaining() || table.extra() == null) {
                    return false;
                }
                int second = Byte.toUnsignedInt(data.get(index + 1));
                int third = Byte.toUnsignedInt(data.get(index + 2));
                int fourth = Byte.toUnsignedInt(data.get(index + 3));
                if (!between(first, 0x81, 0xfe)
                        || !between(third, 0x81, 0xfe)
                        || !between(fourth, 0x30, 0x39)) {
                    return false;
                }
                int pointer = (((first - 0x81) * 10 + (second - 0x30)) * 126
                        + (third - 0x81)) * 10 + (fourth - 0x30);
                if (!contains(table.extra(), pointer)) {
                    return false;
                }
                index += 4;
                continue;
            }

            if (index + 1 >= data.remaining()) {
                return false;
            }
            int pair = (first << 8) | Byte.toUnsignedInt(data.get(index + 1));
            if (!contains(table.pairs(), pair)) {
                return false;
            }
            index += 2;
        }
        return true;
    }

    /// Validates complete UTF-8, rejecting truncated final sequences.
    ///
    /// @param data   bytes to validate
    /// @param offset first byte after an optional signature
    /// @return whether the suffix is well-formed UTF-8
    private static boolean isValidUtf8(@UnmodifiableView ByteBuffer data, int offset) {
        int index = offset;
        while (index < data.remaining()) {
            int first = Byte.toUnsignedInt(data.get(index));
            if (first < 0x80) {
                index++;
                continue;
            }
            int length;
            if (between(first, 0xc2, 0xdf)) {
                length = 2;
            } else if (between(first, 0xe0, 0xef)) {
                length = 3;
            } else if (between(first, 0xf0, 0xf4)) {
                length = 4;
            } else {
                return false;
            }
            if (index + length > data.remaining()) {
                return false;
            }
            for (int continuation = 1; continuation < length; continuation++) {
                if (!between(Byte.toUnsignedInt(data.get(index + continuation)), 0x80, 0xbf)) {
                    return false;
                }
            }
            int second = Byte.toUnsignedInt(data.get(index + 1));
            if ((first == 0xe0 && second < 0xa0)
                    || (first == 0xed && second > 0x9f)
                    || (first == 0xf0 && second < 0x90)
                    || (first == 0xf4 && second > 0x8f)) {
                return false;
            }
            index += length;
        }
        return true;
    }

    /// Validates BOM-sensitive UTF-16.
    ///
    /// @param data bytes to validate
    /// @return whether a BOM and valid payload are present
    private static boolean isValidUtf16WithBom(@UnmodifiableView ByteBuffer data) {
        if (startsWith(data, 0xfe, 0xff)) {
            return isValidUtf16(data, false, 2);
        }
        if (startsWith(data, 0xff, 0xfe)) {
            return isValidUtf16(data, true, 2);
        }
        return false;
    }

    /// Validates endian-specific UTF-16 code units and surrogate pairing.
    ///
    /// @param data         bytes to validate
    /// @param littleEndian whether code units are little-endian
    /// @param offset       first payload byte
    /// @return whether the payload is well-formed
    private static boolean isValidUtf16(
            @UnmodifiableView ByteBuffer data,
            boolean littleEndian,
            int offset
    ) {
        if (((data.remaining() - offset) & 1) != 0) {
            return false;
        }
        boolean pendingHigh = false;
        for (int index = offset; index < data.remaining(); index += 2) {
            int unit = littleEndian
                    ? Byte.toUnsignedInt(data.get(index))
                    | (Byte.toUnsignedInt(data.get(index + 1)) << 8)
                    : (Byte.toUnsignedInt(data.get(index)) << 8)
                    | Byte.toUnsignedInt(data.get(index + 1));
            if (between(unit, 0xd800, 0xdbff)) {
                if (pendingHigh) {
                    return false;
                }
                pendingHigh = true;
            } else if (between(unit, 0xdc00, 0xdfff)) {
                if (!pendingHigh) {
                    return false;
                }
                pendingHigh = false;
            } else if (pendingHigh) {
                return false;
            }
        }
        return !pendingHigh;
    }

    /// Validates BOM-sensitive UTF-32.
    ///
    /// @param data bytes to validate
    /// @return whether a BOM and valid payload are present
    private static boolean isValidUtf32WithBom(@UnmodifiableView ByteBuffer data) {
        if (startsWith(data, 0x00, 0x00, 0xfe, 0xff)) {
            return isValidUtf32(data, false, 4);
        }
        if (startsWith(data, 0xff, 0xfe, 0x00, 0x00)) {
            return isValidUtf32(data, true, 4);
        }
        return false;
    }

    /// Validates endian-specific UTF-32 scalar values.
    ///
    /// @param data         bytes to validate
    /// @param littleEndian whether code points are little-endian
    /// @param offset       first payload byte
    /// @return whether every unit is a Unicode scalar value
    private static boolean isValidUtf32(
            @UnmodifiableView ByteBuffer data,
            boolean littleEndian,
            int offset
    ) {
        if (((data.remaining() - offset) & 3) != 0) {
            return false;
        }
        for (int index = offset; index < data.remaining(); index += 4) {
            long value;
            if (littleEndian) {
                value = Integer.toUnsignedLong(
                        Byte.toUnsignedInt(data.get(index))
                                | (Byte.toUnsignedInt(data.get(index + 1)) << 8)
                                | (Byte.toUnsignedInt(data.get(index + 2)) << 16)
                                | (Byte.toUnsignedInt(data.get(index + 3)) << 24)
                );
            } else {
                value = Integer.toUnsignedLong(
                        (Byte.toUnsignedInt(data.get(index)) << 24)
                                | (Byte.toUnsignedInt(data.get(index + 1)) << 16)
                                | (Byte.toUnsignedInt(data.get(index + 2)) << 8)
                                | Byte.toUnsignedInt(data.get(index + 3))
                );
            }
            if (value > 0x10ffffL || between(value, 0xd800L, 0xdfffL)) {
                return false;
            }
        }
        return true;
    }

    /// Validates RFC 2152 UTF-7 shifts and UTF-16BE payloads.
    ///
    /// @param data bytes to validate
    /// @return whether the full sequence is valid UTF-7
    private static boolean isValidUtf7(@UnmodifiableView ByteBuffer data) {
        int index = 0;
        while (index < data.remaining()) {
            int value = Byte.toUnsignedInt(data.get(index));
            if (value > 0x7f) {
                return false;
            }
            if (value != '+') {
                index++;
                continue;
            }
            if (index + 1 < data.remaining() && data.get(index + 1) == '-') {
                index += 2;
                continue;
            }
            int start = ++index;
            while (index < data.remaining() && base64Value(data.get(index)) >= 0) {
                index++;
            }
            if (index == start || !isValidUtf7Payload(data, start, index)) {
                return false;
            }
            if (index < data.remaining() && data.get(index) == '-') {
                index++;
            }
        }
        return true;
    }

    /// Validates one UTF-7 Base64 payload as padded UTF-16BE.
    ///
    /// @param data  containing bytes
    /// @param start first Base64 byte
    /// @param end   exclusive Base64 end
    /// @return whether the payload contains valid UTF-16 code units
    private static boolean isValidUtf7Payload(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end
    ) {
        int sextets = end - start;
        int totalBits = sextets * 6;
        if (totalBits < 16) {
            return false;
        }
        int unitCount = totalBits / 16;
        int trailingBits = totalBits - unitCount * 16;
        long accumulator = 0L;
        int accumulated = 0;
        int unitIndex = 0;
        boolean pendingHigh = false;
        for (int index = start; index < end; index++) {
            accumulator = (accumulator << 6) | base64Value(data.get(index));
            accumulated += 6;
            while (accumulated >= 16 && unitIndex < unitCount) {
                accumulated -= 16;
                int unit = (int) ((accumulator >>> accumulated) & 0xffffL);
                unitIndex++;
                if (between(unit, 0xd800, 0xdbff)) {
                    if (pendingHigh) {
                        return false;
                    }
                    pendingHigh = true;
                } else if (between(unit, 0xdc00, 0xdfff)) {
                    if (!pendingHigh) {
                        return false;
                    }
                    pendingHigh = false;
                } else if (pendingHigh) {
                    return false;
                }
                if (accumulated == 0) {
                    accumulator = 0L;
                } else {
                    accumulator &= (1L << accumulated) - 1L;
                }
            }
        }
        long trailingMask = trailingBits == 0 ? 0L : (1L << trailingBits) - 1L;
        return !pendingHigh && unitIndex == unitCount && (accumulator & trailingMask) == 0L;
    }

    /// Validates HZ ASCII and GB2312 shift states.
    ///
    /// @param data bytes to validate
    /// @return whether the HZ stream ends in a valid state
    private static boolean isValidHz(@UnmodifiableView ByteBuffer data) {
        boolean shifted = false;
        int pending = -1;
        int index = 0;
        while (index < data.remaining()) {
            int value = Byte.toUnsignedInt(data.get(index));
            if (value > 0x7f) {
                return false;
            }
            if (value == '~') {
                if (pending >= 0 || index + 1 >= data.remaining()) {
                    return false;
                }
                int next = Byte.toUnsignedInt(data.get(index + 1));
                if (next == '{' && !shifted) {
                    shifted = true;
                } else if (next == '}' && shifted) {
                    shifted = false;
                } else if (next == '~') {
                    // Escaped literal tilde is valid in either state.
                } else if (next == '\n') {
                    // HZ line continuation retains the current state.
                } else if (next == '\r'
                        && index + 2 < data.remaining()
                        && data.get(index + 2) == '\n') {
                    index++;
                } else {
                    return false;
                }
                index += 2;
                continue;
            }
            if (!shifted) {
                index++;
                continue;
            }
            if (!between(value, 0x21, 0x7e)) {
                return false;
            }
            if (pending < 0) {
                pending = value;
            } else {
                int pair = (pending << 8) | value;
                if (!contains(TablesHolder.HZ_PAIRS, pair)) {
                    return false;
                }
                pending = -1;
            }
            index++;
        }
        return pending < 0;
    }

    /// Performs conservative seven-bit validation for ISO-2022 families.
    ///
    /// Definitive ISO-2022 detection occurs earlier from designation
    /// sequences. This check prevents high-bit data from entering a seven-bit
    /// candidate when validity filtering is reached.
    ///
    /// @param data bytes to validate
    /// @return whether all bytes are seven-bit
    private static boolean isValidIso2022(@UnmodifiableView ByteBuffer data) {
        for (int index = 0; index < data.remaining(); index++) {
            if (data.get(index) < 0) {
                return false;
            }
        }
        return true;
    }

    /// Tests whether every byte is present in a byte set.
    ///
    /// @param data bytes to test
    /// @param values valid byte values
    /// @return whether all bytes are valid
    private static boolean allBytesInSet(
            @UnmodifiableView ByteBuffer data,
            ByteSet values
    ) {
        for (int index = 0; index < data.remaining(); index++) {
            if (!values.contains(data.get(index))) {
                return false;
            }
        }
        return true;
    }

    /// Tests one bit in a little-bit-order packed mask.
    ///
    /// @param mask  packed mask
    /// @param value bit index
    /// @return whether the bit is set
    private static boolean contains(byte @Unmodifiable [] mask, int value) {
        return (Byte.toUnsignedInt(mask[value >>> 3]) & (1 << (value & 7))) != 0;
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

    /// Tests a byte-array prefix expressed as unsigned integers.
    ///
    /// @param data   bytes to inspect
    /// @param prefix unsigned prefix bytes
    /// @return whether the prefix matches
    private static boolean startsWith(
            @UnmodifiableView ByteBuffer data,
            int @Unmodifiable ... prefix
    ) {
        if (data.remaining() < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (Byte.toUnsignedInt(data.get(index)) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    /// Returns a Base64 sextet value for a byte.
    ///
    /// @param value encoded byte
    /// @return value in `[0, 63]`, or `-1`
    private static int base64Value(byte value) {
        return Constants.UTF7_BASE64_VALUES[Byte.toUnsignedInt(value)];
    }

    /// Loads exact stateless multibyte sequence maps.
    ///
    /// @return immutable encoding map
    private static @Unmodifiable Map<Encoding, MultibyteTable> loadMultibyteTables() {
        byte[] raw = requireResource(MULTIBYTE_RESOURCE);
        ByteBuffer buffer = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);
        requireRemaining(buffer, Integer.BYTES + Short.BYTES, "truncated header");
        if (buffer.getInt() != MULTIBYTE_MAGIC) {
            throw corruptMultibyte("missing KVM1 magic");
        }
        int count = Short.toUnsignedInt(buffer.getShort());
        if (count != MULTIBYTE_TABLE_COUNT) {
            throw corruptMultibyte("expected " + MULTIBYTE_TABLE_COUNT + " tables but found " + count);
        }
        EnumMap<Encoding, MultibyteTable> result = new EnumMap<>(Encoding.class);
        for (int tableIndex = 0; tableIndex < count; tableIndex++) {
            requireRemaining(buffer, 1, "truncated name length");
            int nameLength = Byte.toUnsignedInt(buffer.get());
            requireRemaining(buffer, nameLength + BYTE_MASK_SIZE + PAIR_MASK_SIZE + 1, "truncated table");
            byte[] encodedName = new byte[nameLength];
            buffer.get(encodedName);
            String name = new String(encodedName, StandardCharsets.US_ASCII);
            @Nullable Encoding encoding = EncodingRegistry.lookup(name);
            if (encoding == null) {
                throw corruptMultibyte("unknown encoding '" + name + "'");
            }
            byte[] singles = new byte[BYTE_MASK_SIZE];
            byte[] pairs = new byte[PAIR_MASK_SIZE];
            buffer.get(singles);
            buffer.get(pairs);
            int kind = Byte.toUnsignedInt(buffer.get());
            byte @Nullable [] extra = null;
            if (kind == 1) {
                requireRemaining(buffer, PAIR_MASK_SIZE, "truncated EUC-JIS-2004 triple map");
                extra = new byte[PAIR_MASK_SIZE];
                buffer.get(extra);
            } else if (kind == 2) {
                requireRemaining(buffer, Integer.BYTES, "truncated GB18030 map length");
                int bits = buffer.getInt();
                if (bits != GB18030_POINTER_COUNT) {
                    throw corruptMultibyte("invalid GB18030 pointer count " + bits);
                }
                int byteCount = (bits + 7) >>> 3;
                requireRemaining(buffer, byteCount, "truncated GB18030 four-byte map");
                extra = new byte[byteCount];
                buffer.get(extra);
            } else if (kind != 0) {
                throw corruptMultibyte("unknown table kind " + kind);
            }
            MultibyteTable table = new MultibyteTable(singles, pairs, extra);
            if (result.putIfAbsent(encoding, table) != null) {
                throw corruptMultibyte("duplicate table " + encoding.canonicalName());
            }
        }
        if (buffer.hasRemaining()) {
            throw corruptMultibyte("unexpected trailing bytes");
        }
        return Collections.unmodifiableMap(result);
    }

    /// Loads a fixed-size HZ shifted-pair mask.
    ///
    /// @return immutable pair mask
    private static byte @Unmodifiable [] loadHzPairs() {
        byte[] data = requireResource(HZ_RESOURCE);
        if (data.length != PAIR_MASK_SIZE) {
            throw new IllegalStateException(
                    "Malformed HZ validity resource: expected " + PAIR_MASK_SIZE
                            + " bytes but found " + data.length
            );
        }
        return data;
    }

    /// Reads a required classpath resource.
    ///
    /// @param path absolute resource path
    /// @return resource contents
    private static byte[] requireResource(String path) {
        @Nullable InputStream input = ByteValidity.class.getResourceAsStream(path);
        if (input == null) {
            throw new IllegalStateException("Missing byte-validity resource: " + path);
        }
        try (input) {
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read byte-validity resource: " + path, exception);
        }
    }

    /// Checks remaining bytes while parsing a binary table.
    ///
    /// @param buffer   source buffer
    /// @param required minimum remaining bytes
    /// @param detail   corruption detail
    private static void requireRemaining(ByteBuffer buffer, int required, String detail) {
        if (required < 0 || buffer.remaining() < required) {
            throw corruptMultibyte(detail);
        }
    }

    /// Creates a multibyte-table corruption exception.
    ///
    /// @param detail corruption detail
    /// @return exception
    private static IllegalStateException corruptMultibyte(String detail) {
        return new IllegalStateException("Malformed multibyte validity resource: " + detail);
    }

    /// Initialization-on-demand holder for binary validity resources.
    @NotNullByDefault
    private static final class TablesHolder {
        /// Canonical stateless multibyte validity tables.
        private static final @Unmodifiable Map<Encoding, MultibyteTable> MULTIBYTE_TABLES =
                loadMultibyteTables();

        /// Valid HZ GB2312 shifted pairs.
        private static final byte @Unmodifiable [] HZ_PAIRS = loadHzPairs();

        /// Prevents holder instantiation.
        private TablesHolder() {
        }
    }

    /// Stores immutable sequence masks for one stateless multibyte codec.
    ///
    /// @param singles valid standalone byte mask
    /// @param pairs   valid complete two-byte sequence mask
    /// @param extra   optional EUC triple or GB18030 four-byte map
    @NotNullByDefault
    private record MultibyteTable(
            byte @Unmodifiable [] singles,
            byte @Unmodifiable [] pairs,
            byte @Nullable @Unmodifiable [] extra
    ) {
        /// Creates immutable table references owned by the resource loader.
        private MultibyteTable {
        }
    }
}
