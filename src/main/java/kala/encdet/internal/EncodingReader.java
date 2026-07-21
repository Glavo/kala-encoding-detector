// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;

/// Decodes a retained detection prefix followed by a byte channel.
///
/// The prefix buffer and channel are owned by this reader. Prefix bytes are
/// decoded directly from their existing buffer; only an incomplete sequence at
/// the prefix boundary may be transferred into the streaming byte buffer.
@NotNullByDefault
public final class EncodingReader extends Reader {
    /// Capacity of the byte buffer used after the detection prefix.
    private static final int BYTE_BUFFER_SIZE = 8192;

    /// Owned source channel, or `null` after closure.
    private @Nullable ReadableByteChannel channel;

    /// Stateful decoder configured to replace malformed and unmappable input.
    private final CharsetDecoder decoder;

    /// Retained detection prefix, or `null` after it has been consumed.
    private @Nullable ByteBuffer prefix;

    /// Streaming input retained across decoder underflow.
    private final ByteBuffer bytes;

    /// Decoded characters that did not fit in a caller's target.
    private CharBuffer pendingCharacters;

    /// Whether the source channel has reached end of input.
    private boolean endOfInput;

    /// Whether the decoder has consumed the final input.
    private boolean decodingComplete;

    /// Whether the decoder has been flushed completely.
    private boolean flushed;

    /// Creates a reader that owns a detection prefix and its following channel.
    ///
    /// The current position and limit of `prefix` delimit the bytes replayed
    /// before bytes read from `channel`. The caller must not access either
    /// object after this constructor returns.
    ///
    /// @param channel source used after the prefix
    /// @param prefix  initial encoded bytes; its position will be advanced
    /// @param charset charset used to decode the bytes
    public EncodingReader(
            ReadableByteChannel channel,
            ByteBuffer prefix,
            Charset charset
    ) {
        this.channel = channel;
        this.prefix = prefix;
        this.decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        this.bytes = ByteBuffer.allocate(BYTE_BUFFER_SIZE);
        this.bytes.limit(0);
        this.pendingCharacters = CharBuffer.allocate(2);
        this.pendingCharacters.limit(0);
    }

    /// Reads decoded characters into a character buffer.
    ///
    /// The target's position is advanced by the number of characters produced;
    /// its limit is unchanged. A nonempty target will not receive a zero result:
    /// this method blocks until it produces a character, reaches end of input,
    /// or throws.
    ///
    /// @param target target receiving decoded characters
    /// @return number of characters produced, or `-1` after decoder exhaustion
    /// @throws IOException             if the channel cannot be read or the
    ///                                 reader is closed
    /// @throws NullPointerException     if `target` is `null`
    /// @throws ReadOnlyBufferException if `target` is read-only
    @Override
    public int read(CharBuffer target) throws IOException {
        synchronized (lock) {
            if (target.isReadOnly()) {
                throw new ReadOnlyBufferException();
            }
            ReadableByteChannel source = requireOpen();
            if (!target.hasRemaining()) {
                return 0;
            }
            return decode(target, source);
        }
    }

    /// Reads decoded characters into part of an array.
    ///
    /// @param target target array
    /// @param offset index of the first character to replace
    /// @param length maximum number of characters to read
    /// @return number of characters produced, `0` when `length` is zero, or
    /// `-1` after decoder exhaustion
    /// @throws IndexOutOfBoundsException if the requested range is outside
    ///                                   `target`
    /// @throws IOException               if the channel cannot be read or the
    ///                                   reader is closed
    /// @throws NullPointerException       if `target` is `null`
    @Override
    public int read(char[] target, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, target.length);
        return read(CharBuffer.wrap(target, offset, length));
    }

    /// Closes the owned channel.
    ///
    /// Repeated calls have no effect. The reader remains closed if channel
    /// closure throws.
    ///
    /// @throws IOException if the channel cannot be closed
    @Override
    public void close() throws IOException {
        synchronized (lock) {
            @Nullable ReadableByteChannel source = channel;
            if (source == null) {
                return;
            }
            channel = null;
            prefix = null;
            source.close();
        }
    }

    /// Decodes until output is available or the decoder is exhausted.
    ///
    /// @param output target character buffer with remaining capacity
    /// @param source open source channel
    /// @return positive produced-character count, or `-1` at end of input
    /// @throws IOException if reading or decoding fails
    private int decode(CharBuffer output, ReadableByteChannel source) throws IOException {
        int initialPosition = output.position();
        drainPendingCharacters(output);
        if (output.position() != initialPosition) {
            return output.position() - initialPosition;
        }
        if (!output.hasRemaining()) {
            return 0;
        }

        while (true) {
            if (flushed) {
                return produced(output, initialPosition);
            }

            if (decodingComplete) {
                CoderResult result = decoder.flush(output);
                if (result.isError()) {
                    result.throwException();
                }
                if (result.isUnderflow()) {
                    flushed = true;
                    return produced(output, initialPosition);
                }
                if (output.position() != initialPosition) {
                    return output.position() - initialPosition;
                }

                result = flushPendingCharacters();
                if (result.isUnderflow()) {
                    flushed = true;
                }
                drainPendingCharacters(output);
                return produced(output, initialPosition);
            }

            @Nullable ByteBuffer currentPrefix = prefix;
            ByteBuffer input = currentPrefix == null ? bytes : currentPrefix;
            CoderResult result = decoder.decode(input, output, endOfInput);
            if (result.isError()) {
                result.throwException();
            }
            if (result.isOverflow()) {
                if (output.position() != initialPosition) {
                    return output.position() - initialPosition;
                }
                result = decodePendingCharacters(input);
                drainPendingCharacters(output);
            }
            if (endOfInput) {
                if (result.isUnderflow()) {
                    decodingComplete = true;
                }
                if (output.position() != initialPosition) {
                    return output.position() - initialPosition;
                }
                continue;
            }
            if (output.position() != initialPosition) {
                return output.position() - initialPosition;
            }
            if (result.isOverflow()) {
                continue;
            }
            refill(source);
        }
    }

    /// Decodes into the pending-character buffer after caller-buffer overflow.
    ///
    /// The pending buffer is enlarged only when a decoder reports overflow
    /// without producing a character.
    ///
    /// @param input encoded bytes at the next undecoded sequence
    /// @return decoder result associated with the pending characters
    /// @throws IOException if decoding fails or the pending buffer cannot grow
    private CoderResult decodePendingCharacters(ByteBuffer input) throws IOException {
        pendingCharacters.clear();
        while (true) {
            CoderResult result = decoder.decode(input, pendingCharacters, endOfInput);
            if (result.isError()) {
                result.throwException();
            }
            if (pendingCharacters.position() != 0 || !result.isOverflow()) {
                pendingCharacters.flip();
                return result;
            }

            int currentCapacity = pendingCharacters.capacity();
            if (currentCapacity > Integer.MAX_VALUE / 2) {
                throw new IOException("Charset decoder produced an oversized character sequence");
            }
            pendingCharacters = CharBuffer.allocate(currentCapacity * 2);
        }
    }

    /// Flushes into the pending-character buffer after caller-buffer overflow.
    ///
    /// @return decoder result associated with the pending characters
    /// @throws IOException if flushing fails or the pending buffer cannot grow
    private CoderResult flushPendingCharacters() throws IOException {
        pendingCharacters.clear();
        while (true) {
            CoderResult result = decoder.flush(pendingCharacters);
            if (result.isError()) {
                result.throwException();
            }
            if (pendingCharacters.position() != 0 || !result.isOverflow()) {
                pendingCharacters.flip();
                return result;
            }

            int currentCapacity = pendingCharacters.capacity();
            if (currentCapacity > Integer.MAX_VALUE / 2) {
                throw new IOException("Charset decoder produced an oversized flush sequence");
            }
            pendingCharacters = CharBuffer.allocate(currentCapacity * 2);
        }
    }

    /// Transfers pending decoded characters into a caller buffer.
    ///
    /// @param output target receiving as many pending characters as fit
    private void drainPendingCharacters(CharBuffer output) {
        if (!pendingCharacters.hasRemaining()) {
            return;
        }

        int originalLimit = pendingCharacters.limit();
        int count = Math.min(output.remaining(), pendingCharacters.remaining());
        pendingCharacters.limit(pendingCharacters.position() + count);
        try {
            output.put(pendingCharacters);
        } finally {
            pendingCharacters.limit(originalLimit);
        }
    }

    /// Moves residual prefix bytes when necessary and reads more encoded input.
    ///
    /// @param source open source channel
    /// @throws IOException if reading fails or the decoder cannot make progress
    private void refill(ReadableByteChannel source) throws IOException {
        @Nullable ByteBuffer currentPrefix = prefix;
        if (currentPrefix != null) {
            bytes.clear();
            if (currentPrefix.remaining() > bytes.remaining()) {
                throw new IOException("Charset decoder retained an oversized input sequence");
            }
            bytes.put(currentPrefix);
            prefix = null;
        } else {
            bytes.compact();
        }

        if (!bytes.hasRemaining()) {
            throw new IOException("Charset decoder made no progress");
        }

        int count;
        do {
            count = source.read(bytes);
            if (count == 0) {
                Thread.onSpinWait();
            }
        } while (count == 0);
        if (count < 0) {
            endOfInput = true;
        }
        bytes.flip();
    }

    /// Returns a read result from the number of characters produced.
    ///
    /// @param output          output buffer after decoding
    /// @param initialPosition output position before decoding
    /// @return produced count, or `-1` when none was produced
    private static int produced(CharBuffer output, int initialPosition) {
        int count = output.position() - initialPosition;
        return count == 0 ? -1 : count;
    }

    /// Returns the source channel or reports that the reader is closed.
    ///
    /// @return open source channel
    /// @throws IOException if this reader is closed
    private ReadableByteChannel requireOpen() throws IOException {
        @Nullable ReadableByteChannel source = channel;
        if (source == null) {
            throw new IOException("Reader closed");
        }
        return source;
    }
}
