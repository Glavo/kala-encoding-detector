// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores an immutable set of unsigned byte values in four 64-bit words.
///
/// Membership tests do not allocate or box the queried value. Signed `byte`
/// arguments are interpreted as their corresponding unsigned values in `0..255`.
@NotNullByDefault
final class ByteSet {
    /// Membership bits for values from 0 through 63.
    private final long word0;

    /// Membership bits for values from 64 through 127.
    private final long word1;

    /// Membership bits for values from 128 through 191.
    private final long word2;

    /// Membership bits for values from 192 through 255.
    private final long word3;

    /// Creates a set from its four membership words.
    ///
    /// @param word0 membership bits for values from 0 through 63
    /// @param word1 membership bits for values from 64 through 127
    /// @param word2 membership bits for values from 128 through 191
    /// @param word3 membership bits for values from 192 through 255
    private ByteSet(long word0, long word1, long word2, long word3) {
        this.word0 = word0;
        this.word1 = word1;
        this.word2 = word2;
        this.word3 = word3;
    }

    /// Creates a set containing the supplied unsigned byte values.
    ///
    /// Repeated values have no additional effect. The supplied array is read only
    /// during this call and is not retained.
    ///
    /// @param values values in the inclusive range `0..255`
    /// @return an immutable set containing every supplied value
    /// @throws IllegalArgumentException if a value is outside `0..255`
    /// @throws NullPointerException if `values` is `null`
    static ByteSet of(int... values) {
        Objects.requireNonNull(values, "values");

        long word0 = 0L;
        long word1 = 0L;
        long word2 = 0L;
        long word3 = 0L;
        for (int value : values) {
            if ((value & ~0xff) != 0) {
                throw new IllegalArgumentException("Byte value out of range: " + value);
            }

            long bit = 1L << (value & 0x3f);
            switch (value >>> 6) {
                case 0 -> word0 |= bit;
                case 1 -> word1 |= bit;
                case 2 -> word2 |= bit;
                case 3 -> word3 |= bit;
                default -> throw new AssertionError("Unreachable byte word index");
            }
        }
        return new ByteSet(word0, word1, word2, word3);
    }

    /// Tests whether an unsigned byte value belongs to this set.
    ///
    /// @param value value to query
    /// @return `true` if `value` is in `0..255` and belongs to this set
    boolean contains(int value) {
        return (value & ~0xff) == 0 && containsUnsigned(value);
    }

    /// Tests whether a byte's unsigned value belongs to this set.
    ///
    /// @param value byte to query
    /// @return `true` if the corresponding unsigned value belongs to this set
    boolean contains(byte value) {
        return containsUnsigned(Byte.toUnsignedInt(value));
    }

    /// Tests one value already known to be in the unsigned-byte range.
    ///
    /// @param value value in the inclusive range `0..255`
    /// @return whether the corresponding membership bit is set
    private boolean containsUnsigned(int value) {
        long word = switch (value >>> 6) {
            case 0 -> word0;
            case 1 -> word1;
            case 2 -> word2;
            case 3 -> word3;
            default -> throw new AssertionError("Unreachable byte word index");
        };
        return (word & (1L << (value & 0x3f))) != 0L;
    }
}
