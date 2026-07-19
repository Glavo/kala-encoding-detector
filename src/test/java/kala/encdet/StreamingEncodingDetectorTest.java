// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

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
        assertEquals(EncodingDetector.detect(data), detector.finish());
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

    /// Verifies the maximum byte count saturates the detector and discards later data.
    @Test
    void maximumBytesBoundsRetainedInput() {
        DetectionOptions options = DetectionOptions.builder().maxBytes(5).build();
        StreamingEncodingDetector detector = new StreamingEncodingDetector(options);
        detector.feed("Hello".getBytes(StandardCharsets.US_ASCII));
        assertTrue(detector.isDone());
        detector.feed(" 世界".getBytes(StandardCharsets.UTF_8));
        assertEquals("ascii", detector.finish().encoding());
    }

    /// Verifies finalization is idempotent and closes the feed phase.
    @Test
    void finishIsIdempotentAndRejectsFurtherFeeds() {
        StreamingEncodingDetector detector = new StreamingEncodingDetector();
        assertFalse(detector.isDone());
        assertEquals(new DetectionResult(null, 0.0, null, null), detector.result());
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
        assertThrows(IllegalStateException.class, () -> detector.feed(null));
        assertThrows(IllegalStateException.class, () -> detector.feed(null, 0, 0));
    }

    /// Verifies reset starts a reusable lifecycle with the same options.
    @Test
    void resetAllowsReuse() {
        DetectionOptions options = DetectionOptions.builder().preferSuperset(true).build();
        StreamingEncodingDetector detector = new StreamingEncodingDetector(options);
        detector.feed("Hello".getBytes(StandardCharsets.US_ASCII));
        assertEquals("Windows-1252", detector.finish().encoding());
        detector.reset();
        assertFalse(detector.isDone());
        assertEquals(new DetectionResult(null, 0.0, null, null), detector.result());
        detector.feed("世界".getBytes(StandardCharsets.UTF_8));
        assertEquals("utf-8", detector.finish().encoding());
        assertSame(options, detector.options());
    }

    /// Verifies range checks are performed even when the detector is saturated.
    @Test
    void validatesFeedRanges() {
        StreamingEncodingDetector detector = new StreamingEncodingDetector(
                DetectionOptions.builder().maxBytes(1).build()
        );
        detector.feed(new byte[]{'x'});
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> detector.feed(new byte[1], 1, 1)
        );
    }
}
