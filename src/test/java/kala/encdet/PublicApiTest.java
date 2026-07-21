// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import kala.encdet.EncodingDetector.Encoding;
import kala.encdet.EncodingDetector.Era;
import kala.encdet.EncodingDetector.Result;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the public detector API and configuration contracts.
@NotNullByDefault
final class PublicApiTest {
    /// Verifies all reference-compatible default configuration values.
    @Test
    void defaultDetectorMatchesReference() {
        EncodingDetector detector = EncodingDetector.DEFAULT;
        assertEquals(EnumSet.allOf(Era.class), detector.encodingEras());
        assertEquals(200_000, detector.maxBytes());
        assertEquals(
                EncodingDetector.DEFAULT_MINIMUM_CONFIDENCE,
                detector.minimumConfidence()
        );
        assertFalse(detector.preferSuperset());
        assertEquals(EnumSet.allOf(Encoding.class), detector.encodings());
        assertEquals(Encoding.CP1252, detector.noMatchEncoding());
        assertEquals(Encoding.UTF_8, detector.emptyInputEncoding());
        assertThrows(
                UnsupportedOperationException.class,
                () -> detector.encodingEras().add(Era.DOS)
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> detector.encodings().remove(Encoding.ASCII)
        );
    }

    /// Verifies immutable configuration changes and defensive copies.
    @Test
    void configurationMethodsDefensivelyCopyCollections() {
        EnumSet<Era> eras = EnumSet.of(Era.MODERN_WEB);
        EnumSet<Encoding> encodings = EnumSet.of(Encoding.CP1252, Encoding.UTF_8);
        EncodingDetector detector = EncodingDetector.DEFAULT
                .withEncodingEras(eras)
                .withMinimumConfidence(0.75)
                .withEncodings(encodings)
                .withNoMatchEncoding(Encoding.CP1252)
                .withEmptyInputEncoding(Encoding.ASCII);

        eras.add(Era.DOS);
        encodings.clear();
        assertEquals(Set.of(Era.MODERN_WEB), detector.encodingEras());
        assertEquals(0.75, detector.minimumConfidence());
        assertEquals(Set.of(Encoding.CP1252, Encoding.UTF_8), detector.encodings());
        assertEquals(Encoding.CP1252, detector.noMatchEncoding());
        assertEquals(Encoding.ASCII, detector.emptyInputEncoding());
        assertThrows(
                UnsupportedOperationException.class,
                () -> detector.encodings().add(Encoding.ASCII)
        );

        EncodingDetector changed = detector.withMaxBytes(17);
        assertNotSame(detector, changed);
        assertEquals(200_000, detector.maxBytes());
        assertEquals(17, changed.maxBytes());
        assertEquals(0.75, changed.minimumConfidence());
        assertEquals(Set.of(Era.MODERN_WEB), changed.encodingEras());
        assertEquals(Set.of(Era.DOS), changed.withEncodingEra(Era.DOS).encodingEras());
    }

    /// Verifies unchanged configuration operations preserve detector identity.
    @Test
    void unchangedConfigurationReturnsSameInstance() {
        EncodingDetector detector = EncodingDetector.DEFAULT
                .withEncodingEra(Era.DOS)
                .withMaxBytes(12)
                .withMinimumConfidence(0.4)
                .withPreferredSuperset(true)
                .withEncodings(Set.of(Encoding.CP437))
                .withNoMatchEncoding(Encoding.CP437)
                .withEmptyInputEncoding(Encoding.ASCII);

        assertSame(detector, detector.withEncodingEras(Set.of(Era.DOS)));
        assertSame(detector, detector.withEncodingEra(Era.DOS));
        assertSame(detector, detector.withMaxBytes(12));
        assertSame(detector, detector.withMinimumConfidence(0.4));
        assertSame(detector, detector.withPreferredSuperset(true));
        assertSame(detector, detector.withEncodings(Set.of(Encoding.CP437)));
        assertSame(detector, detector.withNoMatchEncoding(Encoding.CP437));
        assertSame(detector, detector.withEmptyInputEncoding(Encoding.ASCII));
        assertSame(
                EncodingDetector.DEFAULT,
                EncodingDetector.DEFAULT.withEncodings(EnumSet.allOf(Encoding.class))
        );
    }

    /// Verifies invalid configuration states are rejected eagerly.
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
        for (double value : new double[]{
                -0.01,
                1.01,
                Double.NaN,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY
        }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> EncodingDetector.DEFAULT.withMinimumConfidence(value)
            );
        }
        assertEquals(
                0.0,
                EncodingDetector.DEFAULT.withMinimumConfidence(0.0).minimumConfidence()
        );
        assertEquals(
                1.0,
                EncodingDetector.DEFAULT.withMinimumConfidence(1.0).minimumConfidence()
        );
    }

    /// Verifies provider-independent canonical and alias lookup.
    @Test
    void registryResolvesCanonicalIanaWhatwgAndCodecAliases() {
        assertEquals(86, Encoding.values().length);
        assertEquals(86, EncodingDetector.supportedEncodings().size());
        assertEquals(Encoding.CP1252, EncodingDetector.lookupEncoding("WINDOWS-1252"));
        assertEquals(Encoding.ISO_8859_1, EncodingDetector.lookupEncoding("latin_1"));
        assertEquals(Encoding.GB18030, EncodingDetector.lookupEncoding("GB_2312-80"));
        assertEquals(Encoding.TIS_620, EncodingDetector.lookupEncoding("iso_8859-11"));
        assertEquals(Encoding.EUC_JIS_2004, EncodingDetector.lookupEncoding("x-euc-jp"));
        assertEquals("cp1252", Encoding.CP1252.canonicalName());
        assertEquals("Windows-1252", Encoding.CP1252.displayName());
        assertNull(EncodingDetector.lookupEncoding("not-a-real-encoding"));
        assertNull(EncodingDetector.lookupEncoding("\0utf-8"));
        assertNull(EncodingDetector.lookupEncoding("utf-8\0"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> EncodingDetector.supportedEncodings().add(Encoding.ASCII)
        );
    }

    /// Verifies every display name retained from the compatible string API.
    @Test
    void exposesCompatibleDisplayNames() {
        Map<Encoding, String> distinctNames = Map.ofEntries(
                Map.entry(Encoding.BIG5_HKSCS, "Big5"),
                Map.entry(Encoding.CP855, "IBM855"),
                Map.entry(Encoding.CP866, "IBM866"),
                Map.entry(Encoding.CP949, "CP949"),
                Map.entry(Encoding.EUC_JIS_2004, "EUC-JP"),
                Map.entry(Encoding.EUC_KR, "EUC-KR"),
                Map.entry(Encoding.GB18030, "GB18030"),
                Map.entry(Encoding.HZ, "HZ-GB-2312"),
                Map.entry(Encoding.ISO_2022_JP_2, "ISO-2022-JP"),
                Map.entry(Encoding.ISO_2022_KR, "ISO-2022-KR"),
                Map.entry(Encoding.ISO_8859_1, "ISO-8859-1"),
                Map.entry(Encoding.ISO_8859_5, "ISO-8859-5"),
                Map.entry(Encoding.ISO_8859_7, "ISO-8859-7"),
                Map.entry(Encoding.ISO_8859_8, "ISO-8859-8"),
                Map.entry(Encoding.ISO_8859_9, "ISO-8859-9"),
                Map.entry(Encoding.JOHAB, "Johab"),
                Map.entry(Encoding.KOI8_R, "KOI8-R"),
                Map.entry(Encoding.MAC_CYRILLIC, "MacCyrillic"),
                Map.entry(Encoding.MAC_ROMAN, "MacRoman"),
                Map.entry(Encoding.SHIFT_JIS_2004, "SHIFT_JIS"),
                Map.entry(Encoding.TIS_620, "TIS-620"),
                Map.entry(Encoding.UTF_16, "UTF-16"),
                Map.entry(Encoding.UTF_32, "UTF-32"),
                Map.entry(Encoding.UTF_8_SIG, "UTF-8-SIG"),
                Map.entry(Encoding.CP1251, "Windows-1251"),
                Map.entry(Encoding.CP1252, "Windows-1252"),
                Map.entry(Encoding.CP1253, "Windows-1253"),
                Map.entry(Encoding.CP1254, "Windows-1254"),
                Map.entry(Encoding.CP1255, "Windows-1255"),
                Map.entry(Encoding.KZ1048, "KZ1048"),
                Map.entry(Encoding.MAC_GREEK, "MacGreek"),
                Map.entry(Encoding.MAC_ICELAND, "MacIceland"),
                Map.entry(Encoding.MAC_LATIN2, "MacLatin2"),
                Map.entry(Encoding.MAC_TURKISH, "MacTurkish")
        );
        for (Encoding encoding : Encoding.values()) {
            assertEquals(
                    distinctNames.getOrDefault(encoding, encoding.canonicalName()),
                    encoding.displayName(),
                    encoding.name()
            );
        }
    }

    /// Verifies deterministic ASCII, empty-input, BOM, and UTF-8 results.
    @Test
    void detectsDeterministicTextCases() {
        EncodingDetector detector = EncodingDetector.DEFAULT;
        assertEquals(
                new Result(Encoding.ASCII, 1.0, "pl", "text/plain"),
                detector.detect("Hello world".getBytes(StandardCharsets.US_ASCII))
        );
        assertEquals(
                new Result(Encoding.UTF_8, 0.10, null, "text/plain"),
                detector.detect(new byte[0])
        );
        assertEquals(
                Encoding.UTF_8_SIG,
                detector.detect(new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf, 'x'}).encoding()
        );
        assertEquals(
                Encoding.UTF_8,
                detector.detect("Héllo 世界".getBytes(StandardCharsets.UTF_8)).encoding()
        );
    }

    /// Verifies heap and direct buffers have the same detection semantics as arrays.
    @Test
    void byteBufferInputsMatchArraysAndPreserveBufferState() {
        byte[] data = "Héllo 世界 — Καλημέρα".getBytes(StandardCharsets.UTF_8);
        EncodingDetector detector = EncodingDetector.DEFAULT;
        Result expected = detector.detect(data);
        List<Result> expectedAll = detector.detectAllUnfiltered(data);

        ByteBuffer direct = ByteBuffer.allocateDirect(data.length + 4);
        direct.put((byte) 0x80);
        int start = direct.position();
        direct.put(data);
        int end = direct.position();
        direct.put((byte) 0x81);
        direct.position(start).limit(end);
        direct.mark();

        assertEquals(expected, detector.detect(direct));
        assertEquals(expectedAll, detector.detectAllUnfiltered(direct));
        assertEquals(start, direct.position());
        assertEquals(end, direct.limit());
        direct.reset();
        assertEquals(start, direct.position());

        byte[] framed = new byte[data.length + 4];
        framed[0] = (byte) 0x80;
        System.arraycopy(data, 0, framed, 2, data.length);
        framed[framed.length - 1] = (byte) 0x81;
        ByteBuffer readOnlySlice = ByteBuffer.wrap(framed, 2, data.length)
                .slice()
                .asReadOnlyBuffer();
        assertEquals(expected, detector.detect(readOnlySlice));
        assertEquals(detector.detectAll(data), detector.detectAll(readOnlySlice));

        ByteBuffer empty = ByteBuffer.allocateDirect(4);
        empty.position(2).limit(2);
        assertEquals(detector.detect(new byte[0]), detector.detect(empty));
    }

    /// Verifies preferred-superset transformation.
    @Test
    void appliesPreferredSupersetTransform() {
        byte[] data = "Hello world".getBytes(StandardCharsets.US_ASCII);
        EncodingDetector preferred = EncodingDetector.DEFAULT.withPreferredSuperset(true);
        assertEquals(Encoding.CP1252, preferred.detect(data).encoding());
        assertEquals(Encoding.ASCII, EncodingDetector.DEFAULT.detect(data).encoding());
    }

    /// Verifies encoding eligibility and fallback gating.
    @Test
    void appliesEncodingSetAndFallbacks() {
        EncodingDetector includeCp1252 = EncodingDetector.DEFAULT
                .withEncodings(Set.of(Encoding.CP1252));
        assertEquals(
                Encoding.CP1252,
                includeCp1252.detect("Héllo café".getBytes(StandardCharsets.UTF_8)).encoding()
        );

        EncodingDetector noEncodings = EncodingDetector.DEFAULT.withEncodings(Set.of());
        Result none = noEncodings.detect("Hello".getBytes(StandardCharsets.US_ASCII));
        assertNull(none.encoding());
        assertEquals(0.0, none.confidence());
        assertEquals("application/octet-stream", none.mimeType());
        assertNull(noEncodings.detect(new byte[0]).encoding());

        EncodingDetector customFallbacks = EncodingDetector.DEFAULT
                .withEncodings(Set.of(Encoding.ASCII))
                .withNoMatchEncoding(Encoding.ASCII)
                .withEmptyInputEncoding(Encoding.ASCII);
        assertEquals(Encoding.ASCII, customFallbacks.detect(new byte[0]).encoding());
        assertEquals(
                Encoding.ASCII,
                customFallbacks.detect(new byte[]{(byte) 0x80, (byte) 0x81, (byte) 0x82}).encoding()
        );
    }

    /// Verifies BOM results are gated by the same candidate filters.
    @Test
    void filtersEarlyBomResults() {
        byte[] data = {(byte) 0xef, (byte) 0xbb, (byte) 0xbf, 'H', 'i'};
        EncodingDetector excluded = EncodingDetector.DEFAULT
                .withEncodings(EnumSet.complementOf(EnumSet.of(Encoding.UTF_8_SIG)));
        EncodingDetector included = EncodingDetector.DEFAULT
                .withEncodings(Set.of(Encoding.CP1252));
        assertEquals(Encoding.UTF_8, excluded.detect(data).encoding());
        assertEquals(Encoding.CP1252, included.detect(data).encoding());
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
        Result boundedExpected = bounded.detect(ascii);
        assertEquals(boundedExpected, bounded.detect(data));
        assertEquals(boundedExpected, bounded.detect(ByteBuffer.wrap(data)));
        assertEquals(Encoding.ASCII, boundedExpected.encoding());
        assertEquals(Encoding.UTF_8, EncodingDetector.DEFAULT.detect(data).encoding());
    }

    /// Verifies candidate filtering, stable ordering, and immutability.
    @Test
    void detectAllReturnsStableImmutableCandidates() {
        byte[] data = {
                (byte) 0xe9, (byte) 0xe8, (byte) 0xea, (byte) 0xeb,
                (byte) 0xf6, (byte) 0xfc, (byte) 0xe4
        };
        EncodingDetector detector = EncodingDetector.DEFAULT;
        List<Result> all = detector.detectAllUnfiltered(data);
        List<Result> filtered = detector.detectAll(data);
        assertFalse(all.isEmpty());
        assertEquals(detector.detect(data), all.get(0));
        double minimumConfidence = detector.minimumConfidence();
        if (all.stream().anyMatch(result -> result.confidence() >= minimumConfidence)) {
            assertTrue(
                    filtered.stream()
                            .allMatch(result -> result.confidence() >= minimumConfidence)
            );
        } else {
            assertEquals(all, filtered);
        }
        for (int index = 1; index < all.size(); index++) {
            assertTrue(all.get(index - 1).confidence() >= all.get(index).confidence());
        }
        assertThrows(
                UnsupportedOperationException.class,
                () -> all.add(new Result(Encoding.ASCII, 1.0, null, "text/plain"))
        );
        assertEquals(detector.detectAllUnfiltered(data), all);
    }

    /// Verifies the configured minimum filters both input forms inclusively.
    @Test
    void configuresMinimumConfidence() {
        byte[] data = {
                (byte) 0xe9, (byte) 0xe8, (byte) 0xea, (byte) 0xeb,
                (byte) 0xf6, (byte) 0xfc, (byte) 0xe4
        };
        List<Result> all = EncodingDetector.DEFAULT.detectAllUnfiltered(data);
        double highest = all.get(0).confidence();
        double lowest = all.get(all.size() - 1).confidence();
        assertTrue(highest > lowest);

        double threshold = (highest + lowest) / 2.0;
        List<Result> expected = all.stream()
                .filter(result -> result.confidence() >= threshold)
                .toList();
        assertFalse(expected.isEmpty());
        assertTrue(expected.size() < all.size());

        EncodingDetector detector = EncodingDetector.DEFAULT.withMinimumConfidence(threshold);
        assertEquals(threshold, detector.minimumConfidence());
        assertEquals(expected, detector.detectAll(data));
        assertEquals(expected, detector.detectAll(ByteBuffer.wrap(data)));
        assertEquals(all, detector.detectAllUnfiltered(data));
        assertEquals(EncodingDetector.DEFAULT.detect(data), detector.detect(data));

        List<Result> boundary = all.stream()
                .filter(result -> result.confidence() >= highest)
                .toList();
        assertEquals(
                boundary,
                detector.withMinimumConfidence(highest).detectAll(data)
        );

        assertTrue(highest < 1.0);
        assertEquals(all, detector.withMinimumConfidence(1.0).detectAll(data));
    }

    /// Verifies public argument null checks and result confidence validation.
    @Test
    @SuppressWarnings("DataFlowIssue")
    void rejectsNullArgumentsAndInvalidResults() {
        assertThrows(NullPointerException.class, () -> EncodingDetector.DEFAULT.detect((byte[]) null));
        assertThrows(NullPointerException.class, () -> EncodingDetector.DEFAULT.detect((ByteBuffer) null));
        assertThrows(NullPointerException.class, () -> EncodingDetector.DEFAULT.detectAll((ByteBuffer) null));
        assertThrows(
                NullPointerException.class,
                () -> EncodingDetector.DEFAULT.detectAllUnfiltered((ByteBuffer) null)
        );
        assertThrows(NullPointerException.class, () -> EncodingDetector.DEFAULT.withEncodingEras(null));
        assertThrows(NullPointerException.class, () -> EncodingDetector.DEFAULT.withEncodingEra(null));
        assertThrows(
                NullPointerException.class,
                () -> EncodingDetector.DEFAULT.withEncodingEras(
                        Collections.singleton(null)
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> EncodingDetector.DEFAULT.withEncodings(null)
        );
        assertThrows(
                NullPointerException.class,
                () -> EncodingDetector.DEFAULT.withEncodings(
                        Collections.singleton(null)
                )
        );
        assertThrows(NullPointerException.class, () -> EncodingDetector.DEFAULT.withNoMatchEncoding(null));
        assertThrows(NullPointerException.class, () -> EncodingDetector.DEFAULT.withEmptyInputEncoding(null));
        assertThrows(NullPointerException.class, () -> EncodingDetector.lookupEncoding(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Result(Encoding.ASCII, Double.NaN, null, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new Result(Encoding.ASCII, 1.01, null, null)
        );
    }
}
