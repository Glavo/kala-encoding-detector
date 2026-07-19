// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.UnmodifiableView;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies normalized buffer views share their caller-owned byte storage.
@NotNullByDefault
final class ByteBufferSupportTest {
    /// Verifies array wrapping is read-only, normalized, and zero-copy.
    @Test
    void arrayViewSharesStorage() {
        byte[] data = {1, 2, 3};
        @UnmodifiableView ByteBuffer view = ByteBufferSupport.wrap(data);

        assertTrue(view.isReadOnly());
        assertEquals(0, view.position());
        assertEquals(data.length, view.remaining());
        data[1] = 42;
        assertEquals(42, view.get(1));
        assertThrows(java.nio.ReadOnlyBufferException.class, () -> view.put(0, (byte) 0));
    }

    /// Verifies a direct-buffer view captures only the remaining region without copying.
    @Test
    void directViewSharesRemainingStorageAndPreservesState() {
        ByteBuffer source = ByteBuffer.allocateDirect(6);
        source.put(new byte[]{10, 20, 30, 40, 50, 60});
        source.position(1).limit(5);
        source.mark();

        @UnmodifiableView ByteBuffer view = ByteBufferSupport.view(source);
        assertTrue(view.isDirect());
        assertTrue(view.isReadOnly());
        assertEquals(0, view.position());
        assertEquals(4, view.remaining());
        assertEquals(20, view.get(0));

        source.put(2, (byte) 99);
        assertEquals(99, view.get(1));
        assertEquals(1, source.position());
        assertEquals(5, source.limit());
        source.reset();
        assertEquals(1, source.position());
    }

    /// Verifies subranges are metadata-only views of normalized input.
    @Test
    void sliceAndPrefixShareStorage() {
        byte[] data = {0, 1, 2, 3, 4};
        @UnmodifiableView ByteBuffer source = ByteBufferSupport.wrap(data);
        @UnmodifiableView ByteBuffer slice = ByteBufferSupport.slice(source, 1, 3);
        @UnmodifiableView ByteBuffer prefix = ByteBufferSupport.prefix(slice, 2);

        data[2] = 77;
        assertEquals(77, slice.get(1));
        assertEquals(77, prefix.get(1));
        assertEquals(2, prefix.remaining());
    }
}
