// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.EncodingDetector.Encoding;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/// Provides the strict valid-byte masks for supported single-byte codecs.
@NotNullByDefault
final class SingleByteValidity {
    /// Number of codecs represented by the embedded masks.
    private static final int EXPECTED_MASK_COUNT = 64;

    /// Valid-byte masks indexed by encoding identity.
    private static final @Unmodifiable Map<Encoding, ByteSet> MASKS = createMasks();

    /// Prevents instantiation of this static table.
    private SingleByteValidity() {
    }

    /// Returns the valid-byte mask for an encoding.
    ///
    /// @param encoding encoding identity
    /// @return immutable mask, or `null` when the encoding is not single-byte
    static @Nullable ByteSet lookup(Encoding encoding) {
        return MASKS.get(encoding);
    }

    /// Creates all valid-byte masks.
    ///
    /// Each word stores 64 consecutive byte values in little-bit order.
    ///
    /// @return immutable masks indexed by encoding identity
    private static @Unmodifiable Map<Encoding, ByteSet> createMasks() {
        EnumMap<Encoding, ByteSet> result = new EnumMap<>(Encoding.class);
        ByteSet allBytes = ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xffffffffffffffffL
        );
        put(
                result,
                allBytes,
                Encoding.CP1256,
                Encoding.KOI8_R,
                Encoding.KOI8_U,
                Encoding.ISO_8859_1,
                Encoding.ISO_8859_2,
                Encoding.ISO_8859_4,
                Encoding.ISO_8859_5,
                Encoding.ISO_8859_9,
                Encoding.ISO_8859_10,
                Encoding.ISO_8859_13,
                Encoding.ISO_8859_14,
                Encoding.ISO_8859_15,
                Encoding.ISO_8859_16,
                Encoding.MAC_CYRILLIC,
                Encoding.MAC_GREEK,
                Encoding.MAC_ICELAND,
                Encoding.MAC_LATIN2,
                Encoding.MAC_ROMAN,
                Encoding.MAC_TURKISH,
                Encoding.CP720,
                Encoding.CP1006,
                Encoding.CP1125,
                Encoding.PTCP154,
                Encoding.CP437,
                Encoding.CP737,
                Encoding.CP775,
                Encoding.CP850,
                Encoding.CP852,
                Encoding.CP855,
                Encoding.CP858,
                Encoding.CP860,
                Encoding.CP861,
                Encoding.CP862,
                Encoding.CP863,
                Encoding.CP865,
                Encoding.CP866,
                Encoding.CP1140,
                Encoding.CP500,
                Encoding.CP875,
                Encoding.CP1026,
                Encoding.CP273
        );
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xfffffffffeffffffL,
                0xffffffffffffffffL
        ), Encoding.CP1251, Encoding.KZ1048);

        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0x0000000000000000L,
                0x0000000000000000L
        ), Encoding.ASCII);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xffffffff00fe0021L,
                0x0fffffff87ffffffL
        ), Encoding.CP874);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xfffffffffefefef5L,
                0xffffffffffffffffL
        ), Encoding.CP1250);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xffffffffdffe5ffdL,
                0xffffffffffffffffL
        ), Encoding.CP1252);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xfffffbff0afe0afdL,
                0x7ffffffffffbffffL
        ), Encoding.CP1253);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xffffffff9ffe1ffdL,
                0xffffffffffffffffL
        ), Encoding.CP1254);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xffffffff0bfe0bfdL,
                0x67ffffff01fffbffL
        ), Encoding.CP1255);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xffffffdd6afeeaf5L,
                0xffffffffffffffffL
        ), Encoding.CP1257);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xffffffff9bfe1bfdL,
                0xffffffffffffffffL
        ), Encoding.CP1258);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xfffffffeffffffffL,
                0x0fffffff87ffffffL
        ), Encoding.TIS_620);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xbfffbfdfffffffffL,
                0xfffefff7fffefff7L
        ), Encoding.ISO_8859_3);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0x88003011ffffffffL,
                0x0007ffff07fffffeL
        ), Encoding.ISO_8859_6);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xffffbfffffffffffL,
                0x7ffffffffffbffffL
        ), Encoding.ISO_8859_7);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0x7ffffffdffffffffL,
                0x67ffffff80000000L
        ), Encoding.ISO_8859_8);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0x8aef78fe0aff7effL,
                0xffffffffffffffffL
        ), Encoding.KOI8_T);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0x7fffffffffffffffL
        ), Encoding.HP_ROMAN8);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xff1fde0057ffffffL,
                0xffffc040be00ff3fL
        ), Encoding.CP856);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xfffbff7fffdfffffL
        ), Encoding.CP857);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xffffff3f67ffffffL,
                0x7fffffffffffffffL
        ), Encoding.CP864);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xffffffffffffffffL,
                0xffffffffffe7ff40L,
                0xffffffffffffffffL
        ), Encoding.CP869);
        put(result, ByteSet.fromWords(
                0xffffffffffffffffL,
                0xff12ffffffffffffL,
                0xffff83ffa3ff8ffeL,
                0x87ff07ff07ff07ffL
        ), Encoding.CP424);

        if (result.size() != EXPECTED_MASK_COUNT) {
            throw new IllegalStateException(
                    "Expected " + EXPECTED_MASK_COUNT + " single-byte masks but found "
                            + result.size()
            );
        }
        return Collections.unmodifiableMap(result);
    }

    /// Associates one mask with one or more encodings.
    ///
    /// @param result destination map
    /// @param mask valid-byte mask
    /// @param encodings encoding identities receiving the mask
    private static void put(
            Map<Encoding, ByteSet> result,
            ByteSet mask,
            Encoding @Unmodifiable ... encodings
    ) {
        for (Encoding encoding : encodings) {
            if (result.putIfAbsent(encoding, mask) != null) {
                throw new IllegalStateException(
                        "Duplicate single-byte mask for " + encoding.canonicalName()
                );
            }
        }
    }
}
