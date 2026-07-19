// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the streaming detector lifecycle and bounded copying behavior.
@NotNullByDefault
final class StreamingEncodingDetectorTest {
    /// Verifies variable-size feeds produce the same result as batch detection.
    @Test
    void streamingMatchesBatchDetection() {
        byte[] data = "Héllo 世界 — Καλημέρα".getBytes(StandardCharsets.UTF_8);
        StreamingEncodingDetector detector = new StreamingEncodingDetector();
        for (int offset = 0; offset < data.length; ) {
            int length = Math.min((offset % 5) + 1, data.length - offset);
            detector.feed(data, offset, length);
            offset += length;
        }
        assertEquals(EncodingDetector.DEFAULT.detect(data), detector.finish());
    }

    /// Verifies feeds are copied and later caller mutations are not observed.
    @Test
    void feedCopiesInput() {
        byte[] data = "Hello".getBytes(StandardCharsets.US_ASCII);
        StreamingEncodingDetector detector = new StreamingEncodingDetector();
        detector.feed(data);
        java.util.Arrays.fill(data, (byte) 0);
        assertEquals("ascii", detector.finish().encoding());
    }

    /// Verifies direct-buffer feeds copy only remaining bytes without consuming the buffer.
    @Test
    void byteBufferFeedCopiesRemainingBytesAndPreservesState() {
        byte[] data = "Héllo 世界 — Καλημέρα".getBytes(StandardCharsets.UTF_8);
        DetectionResult expected = EncodingDetector.DEFAULT.detect(data);
        ByteBuffer direct = ByteBuffer.allocateDirect(data.length + 4);
        direct.put((byte) 0x80);
        int start = direct.position();
        direct.put(data);
        int end = direct.position();
        direct.put((byte) 0x81);
        direct.position(start).limit(end);
        direct.mark();

        StreamingEncodingDetector detector = new StreamingEncodingDetector();
        detector.feed(direct);
        assertEquals(start, direct.position());
        assertEquals(end, direct.limit());
        direct.put(start, (byte) 0);
        assertEquals(expected, detector.finish());
        direct.reset();
        assertEquals(start, direct.position());

        StreamingEncodingDetector readOnlyDetector = new StreamingEncodingDetector();
        ByteBuffer readOnly = ByteBuffer.wrap(data).asReadOnlyBuffer();
        readOnlyDetector.feed(readOnly);
        assertEquals(expected, readOnlyDetector.finish());
        assertEquals(0, readOnly.position());
    }

    /// Verifies the maximum byte count saturates the detector and discards later data.
    @Test
    void maximumBytesBoundsRetainedInput() {
        EncodingDetector configuration = EncodingDetector.DEFAULT.withMaxBytes(5);
        StreamingEncodingDetector detector = new StreamingEncodingDetector(configuration);
        ByteBuffer input = ByteBuffer.wrap("Hello 世界".getBytes(StandardCharsets.UTF_8));
        detector.feed(input);
        assertTrue(detector.isDone());
        assertEquals(0, input.position());
        detector.feed(" 世界".getBytes(StandardCharsets.UTF_8));
        assertEquals("ascii", detector.finish().encoding());
    }

    /// Verifies finalization is idempotent and closes the feed phase.
    @Test
    void finishIsIdempotentAndRejectsFurtherFeeds() {
        StreamingEncodingDetector detector = new StreamingEncodingDetector();
        assertFalse(detector.isDone());
        assertEquals(new DetectionResult(null, 0.0, null, null), detector.result());
        assertThrows(NullPointerException.class, () -> detector.feed((ByteBuffer) null));
        detector.feed(new byte[0]);
        DetectionResult first = detector.finish();
        assertSame(first, detector.finish());
        assertSame(first, detector.result());
        assertTrue(detector.isDone());
        assertThrows(IllegalStateException.class, () -> detector.feed(new byte[0]));
        assertThrows(
                IllegalStateException.class,
                () -> detector.feed(new byte[1], 0, 1)
        );
        assertThrows(IllegalStateException.class, () -> detector.feed((byte[]) null));
        assertThrows(IllegalStateException.class, () -> detector.feed((ByteBuffer) null));
        assertThrows(IllegalStateException.class, () -> detector.feed(null, 0, 0));
    }

    /// Verifies reset starts a reusable lifecycle with the same detector configuration.
    @Test
    void resetAllowsReuse() {
        EncodingDetector configuration = EncodingDetector.DEFAULT.withPreferredSuperset(true);
        StreamingEncodingDetector detector = configuration.newStreamingDetector();
        detector.feed("Hello".getBytes(StandardCharsets.US_ASCII));
        assertEquals("Windows-1252", detector.finish().encoding());
        detector.reset();
        assertFalse(detector.isDone());
        assertEquals(new DetectionResult(null, 0.0, null, null), detector.result());
        detector.feed("世界".getBytes(StandardCharsets.UTF_8));
        assertEquals("utf-8", detector.finish().encoding());
        assertSame(configuration, detector.detector());
    }

    /// Verifies range checks are performed even when the detector is saturated.
    @Test
    void validatesFeedRanges() {
        StreamingEncodingDetector detector = new StreamingEncodingDetector(
                EncodingDetector.DEFAULT.withMaxBytes(1)
        );
        detector.feed(new byte[]{'x'});
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> detector.feed(new byte[1], 1, 1)
        );
    }
}
