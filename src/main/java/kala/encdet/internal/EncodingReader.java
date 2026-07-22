// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.EncodingDetector;
import kala.encdet.EncodingDetector.Encoding;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;

/// Detects and decodes bytes from a channel.
///
/// Construction does not read from the channel. A read with a nonempty target
/// obtains the detection prefix before producing characters.
///
/// Bytes obtained before a channel read fails remain buffered. A later read
/// resumes from those bytes and continues reading the prefix.
///
/// Instances are not safe for concurrent use.
@NotNullByDefault
public final class EncodingReader extends Reader {
    /// Maximum number of new bytes requested by one post-detection refill.
    private static final int REFILL_SIZE = 8192;

    /// Detector whose immutable configuration selects the encoding and charset mapping.
    private final EncodingDetector detector;

    /// Owned source channel, or `null` after closure.
    private @Nullable ReadableByteChannel channel;

    /// Stateful decoder, or `null` until an encoding has been selected.
    private @Nullable CharsetDecoder decoder;

    /// Encoded input shared by detection and decoding.
    private final ByteBuffer bytes;

    /// Decoded characters that did not fit in a caller's target.
    private CharBuffer pendingCharacters;

    /// Encoding selected from the detection bytes, or `null` when none matched.
    private @Nullable Encoding detectedEncoding;

    /// Whether the leading bytes have been read and detection has completed.
    private boolean detectionComplete;

    /// Whether the source channel has reached end of input.
    private boolean endOfInput;

    /// Whether the decoder has consumed the final input.
    private boolean decodingComplete;

    /// Whether the decoder has been flushed completely.
    private boolean flushed;

    /// Creates a reader that takes ownership of a channel.
    ///
    /// This constructor does not read from `channel` or perform detection. The
    /// caller must not access the channel after this constructor returns.
    ///
    /// @param detector detector used to select an encoding and charset mapping
    /// @param channel  source owned by the reader
    /// @throws IllegalBlockingModeException if `channel` is a selectable channel
    ///                                      configured in non-blocking mode
    /// @throws NullPointerException if either argument is `null`
    public EncodingReader(
            EncodingDetector detector,
            ReadableByteChannel channel
    ) {
        this.detector = Objects.requireNonNull(detector, "detector");
        this.channel = Objects.requireNonNull(channel, "channel");
        if (channel instanceof SelectableChannel selectableChannel
                && !selectableChannel.isBlocking()) {
            throw new IllegalBlockingModeException();
        }
        this.bytes = ByteBuffer.allocate(Math.max(REFILL_SIZE, detector.maxBytes()));
        this.bytes.limit(detector.maxBytes());
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
    /// @throws UnsupportedEncodingException if detection selects an encoding
    ///                                      without a suitable charset
    /// @throws IOException             if detection selects no encoding, the
    ///                                 channel cannot be read, the selected
    ///                                 encoding has no decoder, or the reader is
    ///                                 closed
    /// @throws NullPointerException     if `target` is `null`
    /// @throws ReadOnlyBufferException if `target` is read-only
    @Override
    public int read(CharBuffer target) throws IOException {
        if (target.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        ReadableByteChannel source = requireOpen();
        if (!target.hasRemaining()) {
            return 0;
        }
        CharsetDecoder activeDecoder = ensureDecoder(source);
        return decode(target, source, activeDecoder);
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
    /// @throws UnsupportedEncodingException if detection selects an encoding
    ///                                      without a suitable charset
    /// @throws IOException               if detection selects no encoding, the
    ///                                   channel cannot be read, the selected
    ///                                   encoding has no decoder, or the reader is
    ///                                   closed
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
        @Nullable ReadableByteChannel source = channel;
        if (source == null) {
            return;
        }
        channel = null;
        source.close();
    }

    /// Ensures that the leading bytes have selected a decoder.
    ///
    /// @param source open source channel
    /// @return selected decoder
    /// @throws IOException if input cannot be read, no encoding is selected, or
    ///                     the selected encoding has no suitable charset
    private CharsetDecoder ensureDecoder(ReadableByteChannel source) throws IOException {
        @Nullable CharsetDecoder currentDecoder = decoder;
        if (currentDecoder != null) {
            return currentDecoder;
        }

        if (!detectionComplete) {
            while (bytes.hasRemaining() && !endOfInput) {
                int count = readNonZero(source, bytes);
                if (count < 0) {
                    endOfInput = true;
                }
            }
            ByteBuffer detectionInput = bytes.duplicate();
            detectionInput.flip();
            detectedEncoding = detector.detect(detectionInput).bestEncoding();
            bytes.flip();
            detectionComplete = true;
        }

        @Nullable Encoding encoding = detectedEncoding;
        if (encoding == null) {
            throw new IOException("No character encoding could be selected");
        }

        @Nullable Charset charset = detector.useApproximateCharset()
                ? encoding.approximateCharset()
                : encoding.charset();
        if (charset == null) {
            throw new UnsupportedEncodingException(encoding.canonicalName());
        }

        currentDecoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);

        int start = bytes.position();
        if (encoding == Encoding.UTF_8_SIG
                && bytes.remaining() >= 3
                && bytes.get(start) == (byte) 0xef
                && bytes.get(start + 1) == (byte) 0xbb
                && bytes.get(start + 2) == (byte) 0xbf) {
            bytes.position(start + 3);
        }

        decoder = currentDecoder;
        return currentDecoder;
    }

    /// Decodes until output is available or the decoder is exhausted.
    ///
    /// @param output target character buffer with remaining capacity
    /// @param source open source channel
    /// @param decoder initialized decoder
    /// @return positive produced-character count, or `-1` at end of input
    /// @throws IOException if reading or decoding fails
    private int decode(
            CharBuffer output,
            ReadableByteChannel source,
            CharsetDecoder decoder
    ) throws IOException {
        int initialPosition = output.position();
        drainPendingCharacters(output);
        if (output.position() != initialPosition) {
            return output.position() - initialPosition;
        }

        while (true) {
            if (flushed) {
                return -1;
            }

            if (decodingComplete) {
                CoderResult result = decoder.flush(output);
                if (result.isError()) {
                    result.throwException();
                }
                if (result.isUnderflow()) {
                    flushed = true;
                    int count = output.position() - initialPosition;
                    return count == 0 ? -1 : count;
                }
                if (output.position() != initialPosition) {
                    return output.position() - initialPosition;
                }

                result = flushPendingCharacters(decoder);
                if (result.isUnderflow()) {
                    flushed = true;
                }
                drainPendingCharacters(output);
                int count = output.position() - initialPosition;
                return count == 0 ? -1 : count;
            }

            CoderResult result = decoder.decode(bytes, output, endOfInput);
            if (result.isError()) {
                result.throwException();
            }
            if (result.isOverflow()) {
                if (output.position() != initialPosition) {
                    return output.position() - initialPosition;
                }
                result = decodePendingCharacters(bytes, decoder);
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
    /// @param decoder initialized decoder
    /// @return decoder result associated with the pending characters
    /// @throws IOException if decoding fails or the pending buffer cannot grow
    private CoderResult decodePendingCharacters(
            ByteBuffer input,
            CharsetDecoder decoder
    ) throws IOException {
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
    /// @param decoder initialized decoder
    /// @return decoder result associated with the pending characters
    /// @throws IOException if flushing fails or the pending buffer cannot grow
    private CoderResult flushPendingCharacters(CharsetDecoder decoder) throws IOException {
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

    /// Compacts residual input and reads another bounded block.
    ///
    /// @param source open source channel
    /// @throws IOException if reading fails or the decoder cannot make progress
    private void refill(ReadableByteChannel source) throws IOException {
        bytes.compact();
        if (!bytes.hasRemaining()) {
            throw new IOException("Charset decoder made no progress");
        }

        int refillLength = Math.min(bytes.remaining(), REFILL_SIZE);
        bytes.limit(bytes.position() + refillLength);
        int count;
        try {
            count = readNonZero(source, bytes);
        } finally {
            bytes.flip();
        }
        if (count < 0) {
            endOfInput = true;
        }
    }

    /// Reads until the channel reports progress or end of input.
    ///
    /// The target must have remaining capacity. Selectable channels are required
    /// to be in blocking mode by the constructor.
    ///
    /// @param source open source channel
    /// @param target buffer receiving bytes
    /// @return a positive byte count, or `-1` at end of input
    /// @throws IOException if the channel cannot be read
    private static int readNonZero(
            ReadableByteChannel source,
            ByteBuffer target
    ) throws IOException {
        int count;
        do {
            count = source.read(target);
        } while (count == 0);
        return count;
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
