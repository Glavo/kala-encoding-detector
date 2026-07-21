// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/// Creates normalized views over arrays and byte buffers.
///
/// Every returned buffer has position zero, a limit equal to its logical
/// length, and shared underlying storage. Returned views preserve the source's
/// write accessibility; [UnmodifiableView] is the internal contract not to
/// mutate their content, rather than a runtime read-only wrapper.
@NotNullByDefault
public final class ByteBufferSupport {
    /// Prevents instantiation of this utility class.
    private ByteBufferSupport() {
    }

    /// Returns a buffer view over a complete byte array.
    ///
    /// The returned buffer is writable because the source array is writable,
    /// but internal callers must treat the annotated view as unmodifiable.
    ///
    /// @param data source array
    /// @return normalized view sharing `data`
    /// @throws NullPointerException if `data` is `null`
    public static @UnmodifiableView ByteBuffer wrap(byte[] data) {
        return ByteBuffer.wrap(data);
    }

    /// Returns a view of a byte buffer's remaining bytes.
    ///
    /// The source buffer's position, limit, and mark are not changed. The view
    /// captures the current position and limit but continues to share the same
    /// underlying byte storage.
    ///
    /// @param buffer source buffer
    /// @return normalized remaining-byte view preserving source write accessibility
    /// @throws NullPointerException if `buffer` is `null`
    public static @UnmodifiableView ByteBuffer view(ByteBuffer buffer) {
        return buffer.slice();
    }

    /// Returns a view of a subrange of a buffer's remaining bytes.
    ///
    /// @param buffer source buffer
    /// @param offset first logical index relative to the source position
    /// @param length number of bytes in the view
    /// @return normalized subrange view preserving source write accessibility
    /// @throws NullPointerException if `buffer` is `null`
    /// @throws IndexOutOfBoundsException if the range is outside the remaining bytes
    public static @UnmodifiableView ByteBuffer slice(
            ByteBuffer buffer,
            int offset,
            int length
    ) {
        Objects.checkFromIndexSize(offset, length, buffer.remaining());
        int start = buffer.position() + offset;
        return buffer.slice(start, length);
    }

    /// Returns a leading view bounded by a maximum length.
    ///
    /// @param buffer source buffer
    /// @param maximumLength nonnegative maximum byte count
    /// @return normalized prefix view preserving source write accessibility
    /// @throws NullPointerException if `buffer` is `null`
    /// @throws IllegalArgumentException if `maximumLength` is negative
    public static @UnmodifiableView ByteBuffer prefix(ByteBuffer buffer, int maximumLength) {
        if (maximumLength < 0) {
            throw new IllegalArgumentException("maximumLength must not be negative");
        }
        return slice(buffer, 0, Math.min(buffer.remaining(), maximumLength));
    }

    /// Decodes a range by mapping each unsigned byte to the same Unicode value.
    ///
    /// This operation does not change the source buffer's position, limit, or
    /// mark.
    ///
    /// @param buffer source buffer
    /// @param offset first logical index relative to the source position
    /// @param length number of bytes to decode
    /// @return ISO-8859-1-equivalent text
    /// @throws NullPointerException if `buffer` is `null`
    /// @throws IndexOutOfBoundsException if the range is outside the remaining bytes
    public static String latin1String(ByteBuffer buffer, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, buffer.remaining());
        int start = buffer.position() + offset;
        if (buffer.hasArray()) {
            return new String(
                    buffer.array(),
                    buffer.arrayOffset() + start,
                    length,
                    StandardCharsets.ISO_8859_1
            );
        }
        byte[] bytes = new byte[length];
        buffer.get(start, bytes, 0, length);
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }
}
