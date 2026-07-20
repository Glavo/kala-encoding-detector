// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

/// Holds immutable constants shared by encoding detector stages.
@NotNullByDefault
final class Constants {
    /// UTF-7 Base64 byte-to-sextet lookup; `-1` denotes a non-Base64 byte.
    static final byte @Unmodifiable [] UTF7_BASE64_VALUES = createUtf7Base64Values();

    /// Prevents instantiation of this constants container.
    private Constants() {
    }

    /// Creates the UTF-7 Base64 lookup table.
    ///
    /// @return immutable lookup array containing `-1` or a value in `0..63`
    private static byte @Unmodifiable [] createUtf7Base64Values() {
        byte[] values = new byte[256];
        java.util.Arrays.fill(values, (byte) -1);
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        for (int index = 0; index < alphabet.length(); index++) {
            values[alphabet.charAt(index)] = (byte) index;
        }
        return values;
    }
}
