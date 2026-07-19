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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the public detector API and option contracts.
@NotNullByDefault
final class PublicApiTest {
    /// Verifies all reference-compatible default option values.
    @Test
    void defaultOptionsMatchReference() {
        DetectionOptions options = DetectionOptions.DEFAULT;
        assertEquals(EnumSet.allOf(EncodingEra.class), options.encodingEras());
        assertEquals(200_000, options.maxBytes());
        assertFalse(options.preferSuperset());
        assertEquals(EncodingNameStyle.CHARDET_COMPATIBLE, options.nameStyle());
        assertNull(options.includeEncodings());
        assertNull(options.excludeEncodings());
        assertEquals("cp1252", options.noMatchEncoding());
        assertEquals("utf-8", options.emptyInputEncoding());
        assertThrows(
                UnsupportedOperationException.class,
                () -> options.encodingEras().add(EncodingEra.DOS)
        );
    }

    /// Verifies builder isolation, alias normalization, and immutable copies.
    @Test
    void optionsDefensivelyCopyAndNormalizeCollections() {
        EnumSet<EncodingEra> eras = EnumSet.of(EncodingEra.MODERN_WEB);
        LinkedHashSet<String> included = new LinkedHashSet<>(Set.of("Windows-1252", "UTF8"));
        DetectionOptions options = DetectionOptions.builder()
                .encodingEras(eras)
                .includeEncodings(included)
                .excludeEncodings(Set.of("latin_1"))
                .noMatchEncoding("windows-1252")
                .emptyInputEncoding("US-ASCII")
                .build();

        eras.add(EncodingEra.DOS);
        included.clear();
        assertEquals(Set.of(EncodingEra.MODERN_WEB), options.encodingEras());
        assertEquals(Set.of("cp1252", "utf-8"), options.includeEncodings());
        assertEquals(Set.of("iso8859-1"), options.excludeEncodings());
        assertEquals("cp1252", options.noMatchEncoding());
        assertEquals("ascii", options.emptyInputEncoding());
        assertThrows(
                UnsupportedOperationException.class,
                () -> options.includeEncodings().add("ascii")
        );
        assertEquals(options, options.toBuilder().build());
    }

    /// Verifies invalid option states and unknown names are rejected eagerly.
    @Test
    void optionsRejectInvalidValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DetectionOptions.builder().maxBytes(0).build()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> DetectionOptions.builder().encodingEras(Set.of()).build()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> DetectionOptions.builder().includeEncodings(Set.of()).build()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> DetectionOptions.builder().includeEncodings(Set.of("not-real")).build()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> DetectionOptions.builder().noMatchEncoding("not-real").build()
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
        assertEquals(
                new DetectionResult("ascii", 1.0, "pl", "text/plain"),
                EncodingDetector.detect("Hello world".getBytes(StandardCharsets.US_ASCII))
        );
        assertEquals(
                new DetectionResult("utf-8", 0.10, null, "text/plain"),
                EncodingDetector.detect(new byte[0])
        );
        assertEquals(
                "UTF-8-SIG",
                EncodingDetector.detect(new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf, 'x'}).encoding()
        );
        assertEquals(
                "utf-8",
                EncodingDetector.detect("Héllo 世界".getBytes(StandardCharsets.UTF_8)).encoding()
        );
    }

    /// Verifies canonical naming and preferred-superset transformations.
    @Test
    void appliesRequestedOutputNameTransforms() {
        byte[] data = "Hello world".getBytes(StandardCharsets.US_ASCII);
        DetectionOptions preferred = DetectionOptions.builder().preferSuperset(true).build();
        DetectionOptions rawPreferred = preferred.toBuilder()
                .nameStyle(EncodingNameStyle.CANONICAL)
                .build();
        assertEquals("Windows-1252", EncodingDetector.detect(data, preferred).encoding());
        assertEquals("cp1252", EncodingDetector.detect(data, rawPreferred).encoding());
        assertEquals(
                "ascii",
                EncodingDetector.detect(
                        data,
                        DetectionOptions.builder().nameStyle(EncodingNameStyle.CANONICAL).build()
                ).encoding()
        );
    }

    /// Verifies era, inclusion, exclusion, and fallback priority.
    @Test
    void appliesCandidateFiltersAndFallbacks() {
        DetectionOptions includeCp1252 = DetectionOptions.builder()
                .includeEncodings(Set.of("cp1252"))
                .nameStyle(EncodingNameStyle.CANONICAL)
                .build();
        assertEquals(
                "cp1252",
                EncodingDetector.detect("Héllo café".getBytes(StandardCharsets.UTF_8), includeCp1252)
                        .encoding()
        );

        DetectionOptions overlap = DetectionOptions.builder()
                .includeEncodings(Set.of("ascii"))
                .excludeEncodings(Set.of("ascii"))
                .build();
        DetectionResult none = EncodingDetector.detect("Hello".getBytes(StandardCharsets.US_ASCII), overlap);
        assertNull(none.encoding());
        assertEquals(0.0, none.confidence());
        assertEquals("application/octet-stream", none.mimeType());

        DetectionOptions customFallbacks = DetectionOptions.builder()
                .includeEncodings(Set.of("ascii"))
                .noMatchEncoding("ascii")
                .emptyInputEncoding("ascii")
                .nameStyle(EncodingNameStyle.CANONICAL)
                .build();
        assertEquals("ascii", EncodingDetector.detect(new byte[0], customFallbacks).encoding());
        assertEquals(
                "ascii",
                EncodingDetector.detect(
                        new byte[]{(byte) 0x80, (byte) 0x81, (byte) 0x82},
                        customFallbacks
                ).encoding()
        );
    }

    /// Verifies BOM results are gated by the same candidate filters.
    @Test
    void filtersEarlyBomResults() {
        byte[] data = {(byte) 0xef, (byte) 0xbb, (byte) 0xbf, 'H', 'i'};
        DetectionOptions excluded = DetectionOptions.builder()
                .excludeEncodings(Set.of("utf-8-sig"))
                .nameStyle(EncodingNameStyle.CANONICAL)
                .build();
        DetectionOptions included = DetectionOptions.builder()
                .includeEncodings(Set.of("cp1252"))
                .nameStyle(EncodingNameStyle.CANONICAL)
                .build();
        assertEquals("utf-8", EncodingDetector.detect(data, excluded).encoding());
        assertEquals("cp1252", EncodingDetector.detect(data, included).encoding());
    }

    /// Verifies scan bounding changes detection at the configured byte boundary.
    @Test
    void honorsMaximumBytes() {
        byte[] ascii = "Hello".getBytes(StandardCharsets.US_ASCII);
        byte[] suffix = " 世界".getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[ascii.length + suffix.length];
        System.arraycopy(ascii, 0, data, 0, ascii.length);
        System.arraycopy(suffix, 0, data, ascii.length, suffix.length);
        DetectionOptions bounded = DetectionOptions.builder().maxBytes(ascii.length).build();
        assertEquals("ascii", EncodingDetector.detect(data, bounded).encoding());
        assertEquals("utf-8", EncodingDetector.detect(data).encoding());
    }

    /// Verifies candidate filtering, stable ordering, and immutability.
    @Test
    void detectAllReturnsStableImmutableCandidates() {
        byte[] data = {
                (byte) 0xe9, (byte) 0xe8, (byte) 0xea, (byte) 0xeb,
                (byte) 0xf6, (byte) 0xfc, (byte) 0xe4
        };
        List<DetectionResult> all = EncodingDetector.detectAllUnfiltered(data);
        List<DetectionResult> filtered = EncodingDetector.detectAll(data);
        assertFalse(all.isEmpty());
        assertEquals(EncodingDetector.detect(data), all.get(0));
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
        assertEquals(EncodingDetector.detectAllUnfiltered(data), all);
    }

    /// Verifies public argument null checks and result confidence validation.
    @Test
    @SuppressWarnings("DataFlowIssue")
    void rejectsNullArgumentsAndInvalidResults() {
        assertThrows(NullPointerException.class, () -> EncodingDetector.detect(null));
        assertThrows(
                NullPointerException.class,
                () -> EncodingDetector.detect(new byte[0], null)
        );
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
