// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

/// Incrementally buffers bytes and runs encoding detection when finished.
///
/// At most `options().maxBytes()` bytes are retained. Reaching that limit
/// makes [#isDone()] return `true` and causes later feeds to be ignored, but
/// detection is not computed until [#finish()] is called. Calling `feed`
/// after `finish` throws [IllegalStateException]. Instances are not
/// thread-safe and may be reused after [#reset()].
@NotNullByDefault
public final class StreamingEncodingDetector {
    /// Result exposed before detection has been finished.
    private static final DetectionResult NONE_RESULT = new DetectionResult(null, 0.0, null, null);

    /// Immutable options used for every lifecycle of this detector.
    private final DetectionOptions options;

    /// Accumulates a bounded copy of input bytes.
    private final ByteArrayOutputStream buffer;

    /// Whether the byte limit has been reached.
    private boolean saturated;

    /// Whether detection has been finalized.
    private boolean finished;

    /// Cached result after finalization.
    private DetectionResult result = NONE_RESULT;

    /// Creates a detector using [DetectionOptions#DEFAULT].
    public StreamingEncodingDetector() {
        this(DetectionOptions.DEFAULT);
    }

    /// Creates a detector using the supplied immutable options.
    ///
    /// @param options detection options retained by this instance
    /// @throws NullPointerException if `options` is `null`
    public StreamingEncodingDetector(DetectionOptions options) {
        this.options = Objects.requireNonNull(options, "options");
        this.buffer = new ByteArrayOutputStream(Math.min(options.maxBytes(), 8192));
    }

    /// Returns the options retained by this detector.
    ///
    /// @return immutable detection options
    public DetectionOptions options() {
        return options;
    }

    /// Copies a complete byte array into the bounded input buffer.
    ///
    /// @param input bytes to feed
    /// @throws NullPointerException  if `input` is `null` and the detector has
    /// not been finished
    /// @throws IllegalStateException if [#finish()] has already been called
    public void feed(byte[] input) {
        ensureFeedPhase();
        Objects.requireNonNull(input, "input");
        feed(input, 0, input.length);
    }

    /// Copies a byte-array range into the bounded input buffer.
    ///
    /// Bytes beyond the configured maximum are ignored. Once the maximum is
    /// reached, subsequent calls before `finish` validate their range and then
    /// return without retaining data.
    ///
    /// @param input  source bytes
    /// @param offset first source index
    /// @param length number of source bytes
    /// @throws NullPointerException      if `input` is `null` and the detector has
    /// not been finished
    /// @throws IndexOutOfBoundsException if the range is outside `input`
    /// @throws IllegalStateException     if [#finish()] has already been called
    public void feed(byte[] input, int offset, int length) {
        ensureFeedPhase();
        Objects.requireNonNull(input, "input");
        Objects.checkFromIndexSize(offset, length, input.length);
        if (saturated || length == 0) {
            return;
        }

        int remaining = options.maxBytes() - buffer.size();
        int accepted = Math.min(remaining, length);
        buffer.write(input, offset, accepted);
        saturated = buffer.size() >= options.maxBytes();
    }

    /// Verifies that the current lifecycle still accepts feeds.
    ///
    /// @throws IllegalStateException if [#finish()] has already been called
    private void ensureFeedPhase() {
        if (finished) {
            throw new IllegalStateException("feed() called after finish() without reset()");
        }
    }

    /// Finalizes detection and returns the highest-ranked result.
    ///
    /// Repeated calls return the same object without rerunning detection.
    ///
    /// @return the finalized detection result
    public DetectionResult finish() {
        if (!finished) {
            result = EncodingDetector.detect(buffer.toByteArray(), options);
            finished = true;
            saturated = true;
        }
        return result;
    }

    /// Resets buffered data and result state for reuse with the same options.
    public void reset() {
        buffer.reset();
        saturated = false;
        finished = false;
        result = NONE_RESULT;
    }

    /// Reports whether no additional bytes will be retained.
    ///
    /// This becomes `true` after the byte limit is reached or after
    /// [#finish()] is called.
    ///
    /// @return whether the detector is saturated or finalized
    public boolean isDone() {
        return saturated || finished;
    }

    /// Returns the current result without finalizing detection.
    ///
    /// Before [#finish()] this returns a sentinel whose encoding, language,
    /// and MIME type are `null` and whose confidence is zero.
    ///
    /// @return the current result
    public DetectionResult result() {
        return result;
    }
}
