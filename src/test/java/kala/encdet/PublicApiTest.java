// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the public detector API and configuration contracts.
@NotNullByDefault
final class PublicApiTest {
    /// Verifies all reference-compatible default configuration values.
    @Test
    void defaultDetectorMatchesReference() {
        EncodingDetector detector = EncodingDetector.DEFAULT;
        assertEquals(EnumSet.allOf(EncodingEra.class), detector.encodingEras());
        assertEquals(200_000, detector.maxBytes());
        assertFalse(detector.preferSuperset());
        assertEquals(EncodingNameStyle.CHARDET_COMPATIBLE, detector.nameStyle());
        assertNull(detector.includeEncodings());
        assertNull(detector.excludeEncodings());
        assertEquals("cp1252", detector.noMatchEncoding());
        assertEquals("utf-8", detector.emptyInputEncoding());
        assertThrows(
                UnsupportedOperationException.class,
                () -> detector.encodingEras().add(EncodingEra.DOS)
        );

        EncodingDetector independentlyCreated = new EncodingDetector();
        assertEquals(detector.encodingEras(), independentlyCreated.encodingEras());
        assertEquals(detector.maxBytes(), independentlyCreated.maxBytes());
        assertEquals(detector.nameStyle(), independentlyCreated.nameStyle());
    }

    /// Verifies immutable configuration changes, alias normalization, and defensive copies.
    @Test
    void configurationMethodsDefensivelyCopyAndNormalizeCollections() {
        EnumSet<EncodingEra> eras = EnumSet.of(EncodingEra.MODERN_WEB);
        LinkedHashSet<String> included = new LinkedHashSet<>(Set.of("Windows-1252", "UTF8"));
        EncodingDetector detector = EncodingDetector.DEFAULT
                .withEncodingEras(eras)
                .withIncludedEncodings(included)
                .withExcludedEncodings(Set.of("latin_1"))
                .withNoMatchEncoding("windows-1252")
                .withEmptyInputEncoding("US-ASCII");

        eras.add(EncodingEra.DOS);
        included.clear();
        assertEquals(Set.of(EncodingEra.MODERN_WEB), detector.encodingEras());
        assertEquals(Set.of("cp1252", "utf-8"), detector.includeEncodings());
        assertEquals(Set.of("iso8859-1"), detector.excludeEncodings());
        assertEquals("cp1252", detector.noMatchEncoding());
        assertEquals("ascii", detector.emptyInputEncoding());
        assertThrows(
                UnsupportedOperationException.class,
                () -> detector.includeEncodings().add("ascii")
        );

        EncodingDetector changed = detector.withMaxBytes(17);
        assertNotSame(detector, changed);
        assertEquals(200_000, detector.maxBytes());
        assertEquals(17, changed.maxBytes());
        assertEquals(Set.of(EncodingEra.MODERN_WEB), changed.encodingEras());
        assertEquals(Set.of(EncodingEra.DOS), changed.withEncodingEra(EncodingEra.DOS).encodingEras());
    }

    /// Verifies invalid configuration states and unknown names are rejected eagerly.
    @Test
    void configurationMethodsRejectInvalidValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> EncodingDetector.DEFAULT.withMaxBytes(0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> EncodingDetector.DEFAULT.withEncodingEras(Set.of())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> EncodingDetector.DEFAULT.withIncludedEncodings(Set.of())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> EncodingDetector.DEFAULT.withIncludedEncodings(Set.of("not-real"))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> EncodingDetector.DEFAULT.withNoMatchEncoding("not-real")
        );
    }

    /// Verifies provider-independent canonical and alias lookup.
    @Test
    void registryResolvesCanonicalIanaWhatwgAndCodecAliases() {
        assertEquals(86, EncodingDetector.supportedEncodings().size());
        assertEquals("cp1252", EncodingDetector.lookupEncoding("WINDOWS-1252"));
        assertEquals("iso8859-1", EncodingDetector.lookupEncoding("latin_1"));
        assertEquals("gb18030", EncodingDetector.lookupEncoding("GB_2312-80"));
        assertEquals("tis-620", EncodingDetector.lookupEncoding("iso_8859-11"));
        assertEquals("euc_jis_2004", EncodingDetector.lookupEncoding("x-euc-jp"));
        assertNull(EncodingDetector.lookupEncoding("not-a-real-encoding"));
        assertNull(EncodingDetector.lookupEncoding("\0utf-8"));
        assertNull(EncodingDetector.lookupEncoding("utf-8\0"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> EncodingDetector.supportedEncodings().add("unknown")
        );
    }

    /// Verifies deterministic ASCII, empty-input, BOM, and UTF-8 results.
    @Test
    void detectsDeterministicTextCases() {
        EncodingDetector detector = EncodingDetector.DEFAULT;
        assertEquals(
                new DetectionResult("ascii", 1.0, "pl", "text/plain"),
                detector.detect("Hello world".getBytes(StandardCharsets.US_ASCII))
        );
        assertEquals(
                new DetectionResult("utf-8", 0.10, null, "text/plain"),
                detector.detect(new byte[0])
        );
        assertEquals(
                "UTF-8-SIG",
                detector.detect(new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf, 'x'}).encoding()
        );
        assertEquals(
                "utf-8",
                detector.detect("Héllo 世界".getBytes(StandardCharsets.UTF_8)).encoding()
        );
    }

    /// Verifies canonical naming and preferred-superset transformations.
    @Test
    void appliesRequestedOutputNameTransforms() {
        byte[] data = "Hello world".getBytes(StandardCharsets.US_ASCII);
        EncodingDetector preferred = EncodingDetector.DEFAULT.withPreferredSuperset(true);
        EncodingDetector rawPreferred = preferred.withNameStyle(EncodingNameStyle.CANONICAL);
        assertEquals("Windows-1252", preferred.detect(data).encoding());
        assertEquals("cp1252", rawPreferred.detect(data).encoding());
        assertEquals(
                "ascii",
                EncodingDetector.DEFAULT
                        .withNameStyle(EncodingNameStyle.CANONICAL)
                        .detect(data)
                        .encoding()
        );
    }

    /// Verifies era, inclusion, exclusion, and fallback priority.
    @Test
    void appliesCandidateFiltersAndFallbacks() {
        EncodingDetector includeCp1252 = EncodingDetector.DEFAULT
                .withIncludedEncodings(Set.of("cp1252"))
                .withNameStyle(EncodingNameStyle.CANONICAL);
        assertEquals(
                "cp1252",
                includeCp1252.detect("Héllo café".getBytes(StandardCharsets.UTF_8)).encoding()
        );

        EncodingDetector overlap = EncodingDetector.DEFAULT
                .withIncludedEncodings(Set.of("ascii"))
                .withExcludedEncodings(Set.of("ascii"));
        DetectionResult none = overlap.detect("Hello".getBytes(StandardCharsets.US_ASCII));
        assertNull(none.encoding());
        assertEquals(0.0, none.confidence());
        assertEquals("application/octet-stream", none.mimeType());

        EncodingDetector customFallbacks = EncodingDetector.DEFAULT
                .withIncludedEncodings(Set.of("ascii"))
                .withNoMatchEncoding("ascii")
                .withEmptyInputEncoding("ascii")
                .withNameStyle(EncodingNameStyle.CANONICAL);
        assertEquals("ascii", customFallbacks.detect(new byte[0]).encoding());
        assertEquals(
                "ascii",
                customFallbacks.detect(new byte[]{(byte) 0x80, (byte) 0x81, (byte) 0x82}).encoding()
        );
    }

    /// Verifies BOM results are gated by the same candidate filters.
    @Test
    void filtersEarlyBomResults() {
        byte[] data = {(byte) 0xef, (byte) 0xbb, (byte) 0xbf, 'H', 'i'};
        EncodingDetector excluded = EncodingDetector.DEFAULT
                .withExcludedEncodings(Set.of("utf-8-sig"))
                .withNameStyle(EncodingNameStyle.CANONICAL);
        EncodingDetector included = EncodingDetector.DEFAULT
                .withIncludedEncodings(Set.of("cp1252"))
                .withNameStyle(EncodingNameStyle.CANONICAL);
        assertEquals("utf-8", excluded.detect(data).encoding());
        assertEquals("cp1252", included.detect(data).encoding());
    }

    /// Verifies scan bounding changes detection at the configured byte boundary.
    @Test
    void honorsMaximumBytes() {
        byte[] ascii = "Hello".getBytes(StandardCharsets.US_ASCII);
        byte[] suffix = " 世界".getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[ascii.length + suffix.length];
        System.arraycopy(ascii, 0, data, 0, ascii.length);
        System.arraycopy(suffix, 0, data, ascii.length, suffix.length);
        EncodingDetector bounded = EncodingDetector.DEFAULT.withMaxBytes(ascii.length);
        assertEquals("ascii", bounded.detect(data).encoding());
        assertEquals("utf-8", EncodingDetector.DEFAULT.detect(data).encoding());
    }

    /// Verifies candidate filtering, stable ordering, and immutability.
    @Test
    void detectAllReturnsStableImmutableCandidates() {
        byte[] data = {
                (byte) 0xe9, (byte) 0xe8, (byte) 0xea, (byte) 0xeb,
                (byte) 0xf6, (byte) 0xfc, (byte) 0xe4
        };
        EncodingDetector detector = EncodingDetector.DEFAULT;
        List<DetectionResult> all = detector.detectAllUnfiltered(data);
        List<DetectionResult> filtered = detector.detectAll(data);
        assertFalse(all.isEmpty());
        assertEquals(detector.detect(data), all.get(0));
        if (all.stream().anyMatch(result -> result.confidence() > 0.20)) {
            assertTrue(filtered.stream().allMatch(result -> result.confidence() > 0.20));
        } else {
            assertEquals(all, filtered);
        }
        for (int index = 1; index < all.size(); index++) {
            assertTrue(all.get(index - 1).confidence() >= all.get(index).confidence());
        }
        assertThrows(
                UnsupportedOperationException.class,
                () -> all.add(new DetectionResult("ascii", 1.0, null, "text/plain"))
        );
        assertEquals(detector.detectAllUnfiltered(data), all);
    }

    /// Verifies public argument null checks and result confidence validation.
    @Test
    @SuppressWarnings("DataFlowIssue")
    void rejectsNullArgumentsAndInvalidResults() {
        assertThrows(NullPointerException.class, () -> EncodingDetector.DEFAULT.detect(null));
        assertThrows(NullPointerException.class, () -> EncodingDetector.DEFAULT.withEncodingEras(null));
        assertThrows(NullPointerException.class, () -> EncodingDetector.DEFAULT.withNameStyle(null));
        assertThrows(NullPointerException.class, () -> EncodingDetector.lookupEncoding(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DetectionResult("ascii", Double.NaN, null, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new DetectionResult("ascii", 1.01, null, null)
        );
    }
}
