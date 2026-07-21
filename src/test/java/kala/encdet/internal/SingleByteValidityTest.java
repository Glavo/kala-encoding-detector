// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.EncodingDetector.Encoding;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies the embedded strict-validity masks for single-byte codecs.
@NotNullByDefault
final class SingleByteValidityTest {
    /// Verifies all masks against the pinned CPython-derived table identity.
    @Test
    void masksMatchPinnedTableDigest() {
        StringBuilder data = new StringBuilder(5_000);
        data.append("# Generated from Python codecs used by chardet ")
                .append("e3dfaa1c75256c9d2a06103b566ea92997844f70\n");
        data.append("# name\t256-bit-valid-byte-mask-little-bit-order\n");
        int maskCount = 0;
        for (Encoding encoding : Encoding.values()) {
            @Nullable ByteSet mask = SingleByteValidity.lookup(encoding);
            if (mask == null) {
                continue;
            }
            maskCount++;
            data.append(encoding.canonicalName()).append('\t')
                    .append(HexFormat.of().formatHex(pack(mask))).append('\n');
        }

        assertEquals(64, maskCount);
        assertEquals(
                "f03213c64ec130fc5c00520f8a69753235438e79f64ad4690b0e13d5d8183509",
                sha256(data.toString())
        );
    }

    /// Packs one byte set in little-bit order.
    ///
    /// @param values values to pack
    /// @return immutable 256-bit mask
    private static byte @Unmodifiable [] pack(ByteSet values) {
        byte[] result = new byte[32];
        for (int value = 0; value <= 0xff; value++) {
            if (values.contains(value)) {
                result[value >>> 3] |= (byte) (1 << (value & 7));
            }
        }
        return result;
    }

    /// Computes the SHA-256 digest of UTF-8 text.
    ///
    /// @param data text to hash
    /// @return lower-case hexadecimal digest
    private static String sha256(String data) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(data.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
