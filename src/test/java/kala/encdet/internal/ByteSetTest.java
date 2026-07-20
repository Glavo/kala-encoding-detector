// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies compact unsigned-byte set construction and membership queries.
@NotNullByDefault
final class ByteSetTest {
    /// Verifies membership across every boundary between the four words.
    @Test
    void storesValuesAcrossAllWords() {
        ByteSet set = ByteSet.of(0, 63, 64, 127, 128, 191, 192, 255, 255);

        for (int value : new int[]{0, 63, 64, 127, 128, 191, 192, 255}) {
            assertTrue(set.contains(value));
        }
        for (int value : new int[]{1, 62, 65, 126, 129, 190, 193, 254}) {
            assertFalse(set.contains(value));
        }
    }

    /// Verifies signed bytes are queried using their unsigned values.
    @Test
    void interpretsByteArgumentsAsUnsigned() {
        ByteSet set = ByteSet.of(0x7f, 0x80, 0xff);

        assertTrue(set.contains((byte) 0x7f));
        assertTrue(set.contains((byte) 0x80));
        assertTrue(set.contains((byte) 0xff));
        assertFalse(set.contains((byte) 0x81));
    }

    /// Verifies a set can represent the complete unsigned-byte domain.
    @Test
    void storesEveryUnsignedByte() {
        int[] values = IntStream.range(0, 256).toArray();
        ByteSet set = ByteSet.of(values);

        for (int value : values) {
            assertTrue(set.contains(value));
        }
    }

    /// Verifies construction rejects values outside the unsigned-byte domain.
    @Test
    void rejectsOutOfRangeValues() {
        assertThrows(IllegalArgumentException.class, () -> ByteSet.of(-1));
        assertThrows(IllegalArgumentException.class, () -> ByteSet.of(256));
        assertThrows(NullPointerException.class, () -> ByteSet.of((int[]) null));
    }

    /// Verifies out-of-range membership queries return `false`.
    @Test
    void excludesOutOfRangeQueries() {
        ByteSet set = ByteSet.of(0, 255);

        assertFalse(set.contains(-1));
        assertFalse(set.contains(256));
        assertFalse(set.contains(Integer.MIN_VALUE));
        assertFalse(set.contains(Integer.MAX_VALUE));
    }
}
