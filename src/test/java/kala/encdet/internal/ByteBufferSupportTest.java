// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.UnmodifiableView;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies normalized buffer views share storage without added read-only layers.
@NotNullByDefault
final class ByteBufferSupportTest {
    /// Verifies array wrapping is normalized, writable, and storage-sharing.
    @Test
    void arrayViewSharesStorage() {
        byte[] data = {1, 2, 3};
        @UnmodifiableView ByteBuffer view = ByteBufferSupport.wrap(data);

        assertFalse(view.isReadOnly());
        assertEquals(0, view.position());
        assertEquals(data.length, view.remaining());
        data[1] = 42;
        assertEquals(42, view.get(1));
    }

    /// Verifies a direct-buffer view shares only the remaining source region.
    @Test
    void directViewSharesRemainingStorageAndPreservesState() {
        ByteBuffer source = ByteBuffer.allocateDirect(6);
        source.put(new byte[]{10, 20, 30, 40, 50, 60});
        source.position(1).limit(5);
        source.mark();

        @UnmodifiableView ByteBuffer view = ByteBufferSupport.view(source);
        assertTrue(view.isDirect());
        assertFalse(view.isReadOnly());
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

    /// Verifies subranges share normalized input storage.
    @Test
    void sliceAndPrefixShareStorage() {
        byte[] data = {0, 1, 2, 3, 4};
        @UnmodifiableView ByteBuffer source = ByteBufferSupport.wrap(data);
        @UnmodifiableView ByteBuffer slice = ByteBufferSupport.slice(source, 1, 3);
        @UnmodifiableView ByteBuffer prefix = ByteBufferSupport.prefix(slice, 2);

        assertFalse(slice.isReadOnly());
        assertFalse(prefix.isReadOnly());
        data[2] = 77;
        assertEquals(77, slice.get(1));
        assertEquals(77, prefix.get(1));
        assertEquals(2, prefix.remaining());
    }

    /// Verifies Latin-1 decoding of an array-backed range preserves buffer state.
    @Test
    void latin1StringDecodesArrayRangeWithoutChangingState() {
        byte[] data = {0, 'A', (byte) 0xe9, 'Z', 0};
        ByteBuffer buffer = ByteBuffer.wrap(data, 1, 3).slice();
        buffer.position(1);
        buffer.mark();

        assertTrue(buffer.hasArray());
        assertEquals("éZ", ByteBufferSupport.latin1String(buffer, 0, 2));
        assertEquals(1, buffer.position());
        assertEquals(3, buffer.limit());
        buffer.reset();
        assertEquals(1, buffer.position());
    }

    /// Verifies Latin-1 decoding of a direct-buffer range preserves buffer state.
    @Test
    void latin1StringDecodesDirectRangeWithoutChangingState() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(4);
        buffer.put(new byte[]{'A', (byte) 0xe9, 'Z', 0}).flip();
        buffer.position(1).limit(3);
        buffer.mark();

        assertFalse(buffer.hasArray());
        assertEquals("éZ", ByteBufferSupport.latin1String(buffer, 0, 2));
        assertEquals(1, buffer.position());
        assertEquals(3, buffer.limit());
        buffer.reset();
        assertEquals(1, buffer.position());
    }
}
