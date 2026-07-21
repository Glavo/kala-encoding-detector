// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import kala.encdet.EncodingDetector.Encoding;
import kala.encdet.EncodingDetector.Era;
import kala.encdet.EncodingDetector.Candidate;
import kala.encdet.EncodingDetector.Result;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the public detector API and configuration contracts.
@NotNullByDefault
final class PublicApiTest {
    /// Verifies all documented default configuration values.
    @Test
    void defaultDetectorUsesDocumentedConfiguration() {
        EncodingDetector detector = EncodingDetector.DEFAULT;
        assertEquals(200_000, detector.maxBytes());
        assertEquals(
                EncodingDetector.DEFAULT_MINIMUM_CONFIDENCE,
                detector.minimumConfidence()
        );
        assertFalse(detector.preferSuperset());
        assertEquals(EnumSet.allOf(Encoding.class), detector.encodings());
        assertNull(detector.noMatchEncoding());
        assertEquals(Encoding.UTF_8, detector.emptyInputEncoding());
        assertThrows(
                UnsupportedOperationException.class,
                () -> detector.encodings().remove(Encoding.ASCII)
        );
    }

    /// Verifies the modern-web preset changes only the effective encoding set.
    @Test
    void modernWebPresetUsesModernWebEncodings() {
        EncodingDetector defaults = EncodingDetector.DEFAULT;
        EncodingDetector detector = EncodingDetector.MODERN_WEB;

        assertNotSame(defaults, detector);
        assertEquals(encodingsIn(Era.MODERN_WEB), detector.encodings());
        assertEquals(defaults.maxBytes(), detector.maxBytes());
        assertEquals(defaults.minimumConfidence(), detector.minimumConfidence());
        assertEquals(defaults.preferSuperset(), detector.preferSuperset());
        assertEquals(defaults.noMatchEncoding(), detector.noMatchEncoding());
        assertEquals(defaults.emptyInputEncoding(), detector.emptyInputEncoding());
        assertSame(detector, detector.withEncodingEra(Era.MODERN_WEB));
    }

    /// Verifies immutable configuration changes and defensive copies.
    @Test
    void configurationMethodsDefensivelyCopyCollections() {
        EnumSet<Era> eras = EnumSet.of(Era.MODERN_WEB);
        EnumSet<Encoding> encodings = EnumSet.of(Encoding.CP1252, Encoding.UTF_8);
        EncodingDetector eraDetector = EncodingDetector.DEFAULT.withEncodingEras(eras);
        EncodingDetector detector = eraDetector
                .withMinimumConfidence(0.75)
                .withEncodings(encodings)
                .withNoMatchEncoding(Encoding.CP1252)
                .withEmptyInputEncoding(Encoding.ASCII);

        eras.add(Era.DOS);
        encodings.clear();
        assertEquals(encodingsIn(Era.MODERN_WEB), eraDetector.encodings());
        assertEquals(0.75, detector.minimumConfidence());
        assertEquals(Set.of(Encoding.CP1252, Encoding.UTF_8), detector.encodings());
        assertEquals(Encoding.CP1252, detector.noMatchEncoding());
        assertEquals(Encoding.ASCII, detector.emptyInputEncoding());
        EncodingDetector noFallback = detector.withNoMatchEncoding(null);
        assertNotSame(detector, noFallback);
        assertNull(noFallback.noMatchEncoding());
        assertSame(noFallback, noFallback.withNoMatchEncoding(null));
        assertThrows(
                UnsupportedOperationException.class,
                () -> detector.encodings().add(Encoding.ASCII)
        );

        EncodingDetector changed = detector.withMaxBytes(17);
        assertNotSame(detector, changed);
        assertEquals(200_000, detector.maxBytes());
        assertEquals(17, changed.maxBytes());
        assertEquals(0.75, changed.minimumConfidence());
        assertEquals(Set.of(Encoding.CP1252, Encoding.UTF_8), changed.encodings());
        assertEquals(encodingsIn(Era.DOS), changed.withEncodingEra(Era.DOS).encodings());
    }

    /// Verifies unchanged configuration operations preserve detector identity.
    @Test
    void unchangedConfigurationReturnsSameInstance() {
        EncodingDetector dosDetector = EncodingDetector.DEFAULT.withEncodingEra(Era.DOS);
        EncodingDetector detector = dosDetector
                .withMaxBytes(12)
                .withMinimumConfidence(0.4)
                .withPreferredSuperset(true)
                .withEncodings(Set.of(Encoding.CP437))
                .withNoMatchEncoding(Encoding.CP437)
                .withEmptyInputEncoding(Encoding.ASCII);

        assertSame(dosDetector, dosDetector.withEncodingEras(Set.of(Era.DOS)));
        assertSame(dosDetector, dosDetector.withEncodingEra(Era.DOS));
        assertSame(detector, detector.withMaxBytes(12));
        assertSame(detector, detector.withMinimumConfidence(0.4));
        assertSame(detector, detector.withPreferredSuperset(true));
        assertSame(detector, detector.withEncodings(Set.of(Encoding.CP437)));
        assertSame(detector, detector.withNoMatchEncoding(Encoding.CP437));
        assertSame(detector, detector.withEmptyInputEncoding(Encoding.ASCII));
        assertSame(
                EncodingDetector.DEFAULT,
                EncodingDetector.DEFAULT.withNoMatchEncoding(null)
        );
        assertSame(
                EncodingDetector.DEFAULT,
                EncodingDetector.DEFAULT.withEncodings(EnumSet.allOf(Encoding.class))
        );
    }

    /// Verifies varargs and collection selectors replace one shared state.
    @Test
    void encodingSelectorsReplaceOneSharedState() {
        EncodingDetector eraLast = EncodingDetector.DEFAULT
                .withEncodings(Set.of(Encoding.UTF_8))
                .withEncodingEra(Era.DOS);
        assertEquals(encodingsIn(Era.DOS), eraLast.encodings());

        EncodingDetector encodingsLast = EncodingDetector.DEFAULT
                .withEncodingEra(Era.DOS)
                .withEncodings(Encoding.UTF_8);
        assertEquals(Set.of(Encoding.UTF_8), encodingsLast.encodings());

        EnumSet<Encoding> expected = encodingsIn(Era.LEGACY_MAC);
        expected.addAll(encodingsIn(Era.MAINFRAME));
        assertEquals(
                expected,
                EncodingDetector.DEFAULT
                        .withEncodingEras(Era.LEGACY_MAC, Era.MAINFRAME)
                        .encodings()
        );
        assertEquals(
                expected,
                EncodingDetector.DEFAULT
                        .withEncodingEras(List.of(Era.LEGACY_MAC, Era.MAINFRAME))
                        .encodings()
        );
        assertTrue(EncodingDetector.DEFAULT.withEncodingEras().encodings().isEmpty());
        assertTrue(EncodingDetector.DEFAULT.withEncodings().encodings().isEmpty());
    }

    /// Verifies invalid configuration states are rejected eagerly.
    @Test
    void configurationMethodsRejectInvalidValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> EncodingDetector.DEFAULT.withMaxBytes(0)
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
        assertEquals(86, Encoding.all().size());
        assertEquals(Encoding.CP1252, Encoding.lookup("WINDOWS-1252"));
        assertEquals(Encoding.ISO_8859_1, Encoding.lookup("latin_1"));
        assertEquals(Encoding.GB18030, Encoding.lookup("GB_2312-80"));
        assertEquals(Encoding.TIS_620, Encoding.lookup("iso_8859-11"));
        assertEquals(Encoding.EUC_JIS_2004, Encoding.lookup("x-euc-jp"));
        assertEquals("cp1252", Encoding.CP1252.canonicalName());
        assertEquals("Windows-1252", Encoding.CP1252.displayName());
        assertNull(Encoding.lookup("not-a-real-encoding"));
        assertNull(Encoding.lookup("\0utf-8"));
        assertNull(Encoding.lookup("utf-8\0"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> Encoding.all().add(Encoding.ASCII)
        );
    }

    /// Verifies exact Java charset mappings and rejects related substitutes.
    @Test
    void mapsEncodingTargetsToExactJavaCharsets() {
        assertEquals(StandardCharsets.US_ASCII, Encoding.ASCII.charset());
        assertEquals(StandardCharsets.UTF_8, Encoding.UTF_8.charset());
        assertEquals(StandardCharsets.UTF_16, Encoding.UTF_16.charset());
        assertEquals(StandardCharsets.UTF_16BE, Encoding.UTF_16_BE.charset());
        assertEquals(StandardCharsets.UTF_16LE, Encoding.UTF_16_LE.charset());
        assertEquals(Charset.forName("windows-31j"), Encoding.CP932.charset());
        assertEquals(Charset.forName("x-windows-949"), Encoding.CP949.charset());
        assertEquals(Charset.forName("x-windows-874"), Encoding.CP874.charset());
        assertEquals(Charset.forName("IBM01140"), Encoding.CP1140.charset());
        assertNotEquals(Charset.forName("IBM037"), Encoding.CP1140.charset());
        assertNotEquals(StandardCharsets.UTF_8, Encoding.UTF_8_SIG.charset());
        assertNotEquals(Charset.forName("EUC-JP"), Encoding.EUC_JIS_2004.charset());
        assertNotEquals(Charset.forName("Shift_JIS"), Encoding.SHIFT_JIS_2004.charset());
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
                new Candidate(Encoding.ASCII, 1.0, "pl", "text/plain"),
                detector.detect(
                        "Hello world".getBytes(StandardCharsets.US_ASCII)
                ).bestCandidate()
        );
        Result empty = detector.detect(new byte[0]);
        assertTrue(empty.candidates().isEmpty());
        assertNull(empty.bestCandidate());
        assertEquals(Encoding.UTF_8, empty.bestEncoding());
        assertEquals(
                Encoding.UTF_8_SIG,
                detector.detect(
                        new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf, 'x'}
                ).bestEncoding()
        );
        assertEquals(
                Encoding.UTF_8,
                detector.detect(
                        "Héllo 世界".getBytes(StandardCharsets.UTF_8)
                ).bestEncoding()
        );
    }

    /// Verifies heap and direct buffers have the same detection semantics as arrays.
    @Test
    void byteBufferInputsMatchArraysAndPreserveBufferState() {
        byte[] data = "Héllo 世界 — Καλημέρα".getBytes(StandardCharsets.UTF_8);
        EncodingDetector detector = EncodingDetector.DEFAULT;
        Result expected = detector.detect(data);
        List<Candidate> expectedCandidates = expected.candidates();

        ByteBuffer direct = ByteBuffer.allocateDirect(data.length + 4);
        direct.put((byte) 0x80);
        int start = direct.position();
        direct.put(data);
        int end = direct.position();
        direct.put((byte) 0x81);
        direct.position(start).limit(end);
        direct.mark();

        assertEquals(expected, detector.detect(direct));
        assertEquals(expectedCandidates, detector.detect(direct).candidates());
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
        assertEquals(
                expected.bestEncoding(),
                detector.detect(readOnlySlice).bestEncoding()
        );

        ByteBuffer empty = ByteBuffer.allocateDirect(4);
        empty.position(2).limit(2);
        assertEquals(detector.detect(new byte[0]), detector.detect(empty));
    }

    /// Verifies preferred-superset transformation.
    @Test
    void appliesPreferredSupersetTransform() {
        byte[] data = "Hello world".getBytes(StandardCharsets.US_ASCII);
        EncodingDetector preferred = EncodingDetector.DEFAULT.withPreferredSuperset(true);
        assertEquals(Encoding.CP1252, preferred.detect(data).bestEncoding());
        assertEquals(
                Encoding.ASCII,
                EncodingDetector.DEFAULT.detect(data).bestEncoding()
        );

        EncodingDetector preferredRecommendation = preferred
                .withEncodings(Encoding.ASCII)
                .withNoMatchEncoding(Encoding.ASCII)
                .withEmptyInputEncoding(Encoding.ASCII);
        assertEquals(
                Encoding.CP1252,
                preferredRecommendation.detect(new byte[0]).bestEncoding()
        );
        assertEquals(
                Encoding.CP1252,
                preferredRecommendation.detect(
                        new byte[]{(byte) 0x80, (byte) 0x81, (byte) 0x82}
                ).bestEncoding()
        );
    }

    /// Verifies encoding eligibility and recommendation gating.
    @Test
    void appliesEncodingSetAndRecommendations() {
        EncodingDetector includeCp1252 = EncodingDetector.DEFAULT
                .withMinimumConfidence(0.0)
                .withEncodings(Set.of(Encoding.CP1252));
        assertEquals(
                Encoding.CP1252,
                includeCp1252.detect(
                        "Héllo café".getBytes(StandardCharsets.UTF_8)
                ).bestEncoding()
        );

        EncodingDetector noEncodings = EncodingDetector.DEFAULT.withEncodings(Set.of());
        Result none = noEncodings.detect(
                "Hello".getBytes(StandardCharsets.US_ASCII)
        );
        assertTrue(none.candidates().isEmpty());
        assertNull(none.bestCandidate());
        assertNull(none.bestEncoding());
        Result noEncodingEmpty = noEncodings.detect(new byte[0]);
        assertTrue(noEncodingEmpty.candidates().isEmpty());
        assertNull(noEncodingEmpty.bestEncoding());

        EncodingDetector noRecommendation = EncodingDetector.DEFAULT
                .withEncodings(Set.of(Encoding.ASCII));
        Result noMatch = noRecommendation.detect(
                new byte[]{(byte) 0x80, (byte) 0x81, (byte) 0x82}
        );
        assertTrue(noMatch.candidates().isEmpty());
        assertNull(noMatch.bestCandidate());
        assertNull(noMatch.bestEncoding());

        EncodingDetector customRecommendations = EncodingDetector.DEFAULT
                .withEncodings(Set.of(Encoding.ASCII))
                .withNoMatchEncoding(Encoding.ASCII)
                .withEmptyInputEncoding(Encoding.ASCII);
        Result emptyRecommendation = customRecommendations.detect(new byte[0]);
        assertTrue(emptyRecommendation.candidates().isEmpty());
        assertNull(emptyRecommendation.bestCandidate());
        assertEquals(Encoding.ASCII, emptyRecommendation.bestEncoding());

        Result noMatchRecommendation = customRecommendations.detect(
                new byte[]{(byte) 0x80, (byte) 0x81, (byte) 0x82}
        );
        assertTrue(noMatchRecommendation.candidates().isEmpty());
        assertNull(noMatchRecommendation.bestCandidate());
        assertEquals(Encoding.ASCII, noMatchRecommendation.bestEncoding());
    }

    /// Verifies BOM results are gated by the same candidate filters.
    @Test
    void filtersEarlyBomResults() {
        byte[] data = {(byte) 0xef, (byte) 0xbb, (byte) 0xbf, 'H', 'i'};
        EncodingDetector excluded = EncodingDetector.DEFAULT
                .withEncodings(EnumSet.complementOf(EnumSet.of(Encoding.UTF_8_SIG)));
        EncodingDetector included = EncodingDetector.DEFAULT
                .withMinimumConfidence(0.0)
                .withEncodings(Set.of(Encoding.CP1252));
        assertEquals(Encoding.UTF_8, excluded.detect(data).bestEncoding());
        assertEquals(Encoding.CP1252, included.detect(data).bestEncoding());
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
        assertEquals(Encoding.ASCII, boundedExpected.bestEncoding());
        assertEquals(
                Encoding.UTF_8,
                EncodingDetector.DEFAULT.detect(data).bestEncoding()
        );
    }

    /// Verifies aggregate candidate stable ordering and immutability.
    @Test
    void resultContainsStableImmutableCandidates() {
        byte[] data = {
                (byte) 0xe9, (byte) 0xe8, (byte) 0xea, (byte) 0xeb,
                (byte) 0xf6, (byte) 0xfc, (byte) 0xe4
        };
        EncodingDetector detector = EncodingDetector.DEFAULT.withMinimumConfidence(0.0);
        Result result = detector.detect(data);
        List<Candidate> all = result.candidates();
        assertFalse(all.isEmpty());
        assertEquals(result.bestCandidate(), all.get(0));
        assertEquals(all.get(0).encoding(), result.bestEncoding());
        assertTrue(all.stream().allMatch(candidate -> candidate.confidence() >= 0.0));
        for (int index = 1; index < all.size(); index++) {
            assertTrue(all.get(index - 1).confidence() >= all.get(index).confidence());
        }
        assertThrows(
                UnsupportedOperationException.class,
                () -> all.add(new Candidate(Encoding.ASCII, 1.0, null, "text/plain"))
        );
        assertEquals(detector.detect(data), result);
    }

    /// Verifies the configured minimum filters both input forms inclusively.
    @Test
    void configuresMinimumConfidence() {
        byte[] data = {
                (byte) 0xe9, (byte) 0xe8, (byte) 0xea, (byte) 0xeb,
                (byte) 0xf6, (byte) 0xfc, (byte) 0xe4
        };
        Result unfilteredResult = EncodingDetector.DEFAULT
                .withMinimumConfidence(0.0)
                .detect(data);
        List<Candidate> all = unfilteredResult.candidates();
        double highest = all.get(0).confidence();
        double lowest = all.get(all.size() - 1).confidence();
        assertTrue(highest > lowest);

        double threshold = (highest + lowest) / 2.0;
        List<Candidate> expected = all.stream()
                .filter(candidate -> candidate.confidence() >= threshold)
                .toList();
        assertFalse(expected.isEmpty());
        assertTrue(expected.size() < all.size());

        EncodingDetector detector = EncodingDetector.DEFAULT.withMinimumConfidence(threshold);
        assertEquals(threshold, detector.minimumConfidence());
        Result result = detector.detect(data);
        assertEquals(expected, result.candidates());
        assertEquals(result, detector.detect(ByteBuffer.wrap(data)));
        assertEquals(expected.get(0), result.bestCandidate());
        assertEquals(expected.get(0).encoding(), result.bestEncoding());

        List<Candidate> boundary = all.stream()
                .filter(candidate -> candidate.confidence() >= highest)
                .toList();
        assertEquals(
                boundary,
                detector.withMinimumConfidence(highest)
                        .detect(data)
                        .candidates()
        );

        assertTrue(highest < 1.0);
        Result rejected = detector.withMinimumConfidence(1.0).detect(data);
        assertTrue(rejected.candidates().isEmpty());
        assertNull(rejected.bestCandidate());
        assertNull(rejected.bestEncoding());

        Result recommendation = detector.withMinimumConfidence(1.0)
                .withNoMatchEncoding(Encoding.ASCII)
                .detect(data);
        assertTrue(recommendation.candidates().isEmpty());
        assertNull(recommendation.bestCandidate());
        assertEquals(Encoding.ASCII, recommendation.bestEncoding());
    }

    /// Verifies aggregate results copy and validate their candidates and recommendation.
    @Test
    void resultCopiesAndValidatesCandidateLists() {
        Candidate high = new Candidate(Encoding.UTF_8, 0.9, null, "text/plain");
        Candidate low = new Candidate(Encoding.CP1252, 0.4, "en", "text/plain");
        ArrayList<Candidate> candidates = new ArrayList<>(List.of(high, low));
        Result result = new Result(candidates, Encoding.UTF_8);
        Result empty = new Result(List.of(), null);
        Result recommendation = new Result(List.of(), Encoding.CP1252);
        Candidate binaryCandidate = new Candidate(
                null,
                0.95,
                null,
                "application/octet-stream"
        );
        Result binary = new Result(List.of(binaryCandidate), null);

        candidates.clear();
        assertFalse(Result.class.isRecord());
        assertEquals(List.of(high, low), result.candidates());
        assertEquals(high, result.bestCandidate());
        assertEquals(Encoding.UTF_8, result.bestEncoding());
        Result equalResult = new Result(List.of(high, low), Encoding.UTF_8);
        assertEquals(result, equalResult);
        assertEquals(result.hashCode(), equalResult.hashCode());
        assertEquals(
                "Result[candidates=" + List.of(high, low)
                        + ", bestEncoding=UTF_8]",
                result.toString()
        );
        assertThrows(UnsupportedOperationException.class, () -> result.candidates().clear());
        assertTrue(empty.candidates().isEmpty());
        assertNull(empty.bestCandidate());
        assertNull(empty.bestEncoding());
        assertTrue(recommendation.candidates().isEmpty());
        assertNull(recommendation.bestCandidate());
        assertEquals(Encoding.CP1252, recommendation.bestEncoding());
        assertEquals(binaryCandidate, binary.bestCandidate());
        assertNull(binary.bestEncoding());

        assertThrows(
                IllegalArgumentException.class,
                () -> new Result(List.of(low, high), Encoding.CP1252)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new Result(List.of(high, low), Encoding.CP1252)
        );
    }

    /// Verifies public argument null checks and candidate confidence validation.
    @Test
    @SuppressWarnings("DataFlowIssue")
    void rejectsNullArgumentsAndInvalidResults() {
        assertThrows(NullPointerException.class, () -> EncodingDetector.DEFAULT.detect((byte[]) null));
        assertThrows(NullPointerException.class, () -> EncodingDetector.DEFAULT.detect((ByteBuffer) null));
        assertThrows(NullPointerException.class, () -> EncodingDetector.DEFAULT.withEncodingEras((Era[]) null));
        assertThrows(
                NullPointerException.class,
                () -> EncodingDetector.DEFAULT.withEncodingEras(Era.MODERN_WEB, null)
        );
        assertThrows(NullPointerException.class, () -> EncodingDetector.DEFAULT.withEncodingEra(null));
        assertThrows(
                NullPointerException.class,
                () -> EncodingDetector.DEFAULT.withEncodingEras(
                        Collections.singleton(null)
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> EncodingDetector.DEFAULT.withEncodings((Collection<Encoding>) null)
        );
        assertThrows(
                NullPointerException.class,
                () -> EncodingDetector.DEFAULT.withEncodings((Encoding[]) null)
        );
        assertThrows(
                NullPointerException.class,
                () -> EncodingDetector.DEFAULT.withEncodings(Encoding.UTF_8, null)
        );
        assertThrows(
                NullPointerException.class,
                () -> EncodingDetector.DEFAULT.withEncodings(
                        Collections.singleton(null)
                )
        );
        assertThrows(NullPointerException.class, () -> EncodingDetector.DEFAULT.withEmptyInputEncoding(null));
        assertThrows(NullPointerException.class, () -> Encoding.lookup(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Candidate(Encoding.ASCII, Double.NaN, null, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new Candidate(Encoding.ASCII, 1.01, null, null)
        );
        Candidate candidate = new Candidate(Encoding.ASCII, 1.0, null, null);
        assertThrows(
                NullPointerException.class,
                () -> new Result(null, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new Result(List.of(candidate), null)
        );
    }

    /// Returns all encodings classified in one era.
    ///
    /// @param era era to select
    /// @return mutable encoding set in enum declaration order
    private static EnumSet<Encoding> encodingsIn(Era era) {
        EnumSet<Encoding> result = EnumSet.noneOf(Encoding.class);
        for (Encoding encoding : Encoding.values()) {
            if (encoding.era() == era) {
                result.add(encoding);
            }
        }
        return result;
    }
}
