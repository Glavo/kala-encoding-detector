// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.DetectionResult;
import kala.encdet.EncodingDetector;
import kala.encdet.EncodingNameStyle;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/// Runs the complete ordered detection pipeline and public result transforms.
@NotNullByDefault
public final class DetectionEngine {
    /// Logger used when a configured fallback is filtered out.
    private static final System.Logger LOGGER = System.getLogger(DetectionEngine.class.getName());

    /// Confidence used by deterministic non-BOM stages.
    private static final double DETERMINISTIC_CONFIDENCE = 0.95;

    /// Confidence used by fallback results.
    private static final double FALLBACK_CONFIDENCE = 0.10;

    /// Structural score that enables combined CJK and statistical ranking.
    private static final double STRUCTURAL_CONFIDENCE_THRESHOLD = 0.85;

    /// Maximum statistical-scoring prefix length.
    private static final int STAT_SCORE_MAX_BYTES = 16_384;

    /// Maximum language-scoring prefix length.
    private static final int LANGUAGE_SCORE_MAX_BYTES = 2048;

    /// Minimum CJK valid-sequence-to-lead ratio.
    private static final double CJK_MIN_MULTIBYTE_RATIO = 0.05;

    /// Minimum non-ASCII byte count for CJK candidates.
    private static final int CJK_MIN_NON_ASCII = 2;

    /// Minimum non-ASCII byte coverage by valid CJK sequences.
    private static final double CJK_MIN_BYTE_COVERAGE = 0.35;

    /// Minimum distinct lead values for sufficiently long CJK samples.
    private static final int CJK_MIN_LEAD_DIVERSITY = 4;

    /// Non-ASCII count at which lead-diversity gating begins.
    private static final int CJK_DIVERSITY_MIN_NON_ASCII = 16;

    /// No-detection sentinel.
    private static final PipelineResult NONE_RESULT = new PipelineResult(null, 0.0, null, null);

    /// Generic binary classification result.
    private static final PipelineResult BINARY_RESULT = new PipelineResult(
            null,
            DETERMINISTIC_CONFIDENCE,
            null,
            "application/octet-stream"
    );

    /// Optional preferred-superset remapping applied before name styling.
    private static final @Unmodifiable Map<String, String> PREFERRED_SUPERSETS = Map.ofEntries(
            Map.entry("ascii", "cp1252"),
            Map.entry("euc_kr", "cp949"),
            Map.entry("iso8859-1", "cp1252"),
            Map.entry("iso8859-2", "cp1250"),
            Map.entry("iso8859-5", "cp1251"),
            Map.entry("iso8859-6", "cp1256"),
            Map.entry("iso8859-7", "cp1253"),
            Map.entry("iso8859-8", "cp1255"),
            Map.entry("iso8859-9", "cp1254"),
            Map.entry("iso8859-11", "cp874"),
            Map.entry("iso8859-13", "cp1257"),
            Map.entry("tis-620", "cp874")
    );

    /// Chardet 5.x and 6.x compatible display-name mapping.
    private static final @Unmodifiable Map<String, String> COMPATIBLE_NAMES = Map.ofEntries(
            Map.entry("big5hkscs", "Big5"),
            Map.entry("cp855", "IBM855"),
            Map.entry("cp866", "IBM866"),
            Map.entry("cp949", "CP949"),
            Map.entry("euc_jis_2004", "EUC-JP"),
            Map.entry("euc_kr", "EUC-KR"),
            Map.entry("gb18030", "GB18030"),
            Map.entry("hz", "HZ-GB-2312"),
            Map.entry("iso2022_jp_2", "ISO-2022-JP"),
            Map.entry("iso2022_kr", "ISO-2022-KR"),
            Map.entry("iso8859-1", "ISO-8859-1"),
            Map.entry("iso8859-5", "ISO-8859-5"),
            Map.entry("iso8859-7", "ISO-8859-7"),
            Map.entry("iso8859-8", "ISO-8859-8"),
            Map.entry("iso8859-9", "ISO-8859-9"),
            Map.entry("johab", "Johab"),
            Map.entry("koi8-r", "KOI8-R"),
            Map.entry("mac-cyrillic", "MacCyrillic"),
            Map.entry("mac-roman", "MacRoman"),
            Map.entry("shift_jis_2004", "SHIFT_JIS"),
            Map.entry("tis-620", "TIS-620"),
            Map.entry("utf-16", "UTF-16"),
            Map.entry("utf-32", "UTF-32"),
            Map.entry("utf-8-sig", "UTF-8-SIG"),
            Map.entry("cp1251", "Windows-1251"),
            Map.entry("cp1252", "Windows-1252"),
            Map.entry("cp1253", "Windows-1253"),
            Map.entry("cp1254", "Windows-1254"),
            Map.entry("cp1255", "Windows-1255"),
            Map.entry("kz1048", "KZ1048"),
            Map.entry("mac-greek", "MacGreek"),
            Map.entry("mac-iceland", "MacIceland"),
            Map.entry("mac-latin2", "MacLatin2"),
            Map.entry("mac-turkish", "MacTurkish")
    );

    /// Prevents instantiation of this static engine.
    private DetectionEngine() {
    }

    /// Runs detection and converts internal canonical candidates to public results.
    ///
    /// @param input    caller-owned bytes, which are never modified or retained
    /// @param detector immutable detector configuration
    /// @return immutable public candidates in stable ranking order
    public static @Unmodifiable List<DetectionResult> detect(
            byte @Unmodifiable [] input,
            EncodingDetector detector
    ) {
        byte[] data = input.length <= detector.maxBytes()
                ? input
                : Arrays.copyOf(input, detector.maxBytes());
        List<PipelineResult> results = runCore(data, detector);
        results = fillLanguages(data, results);
        ArrayList<DetectionResult> publicResults = new ArrayList<>(results.size());
        for (PipelineResult result : results) {
            String mimeType = result.mimeType() != null
                    ? result.mimeType()
                    : result.encoding() == null ? "application/octet-stream" : "text/plain";
            @Nullable String encoding = transformName(result.encoding(), detector);
            double confidence = Math.max(0.0, Math.min(result.confidence(), 1.0));
            publicResults.add(new DetectionResult(
                    encoding,
                    confidence,
                    result.language(),
                    mimeType
            ));
        }
        if (publicResults.isEmpty()) {
            throw new IllegalStateException("The detection pipeline returned no result");
        }
        return List.copyOf(publicResults);
    }

    /// Runs all detection stages through post-processing in reference order.
    ///
    /// @param data     caller input already bounded by the configured maximum
    /// @param detector immutable detector configuration
    /// @return internal canonical candidates
    private static List<PipelineResult> runCore(
            byte @Unmodifiable [] data,
            EncodingDetector detector
    ) {
        List<EncodingRegistry.Info> candidates = EncodingRegistry.candidates(detector);
        LinkedHashSet<String> mutableAllowed = new LinkedHashSet<>(candidates.size());
        for (EncodingRegistry.Info candidate : candidates) {
            mutableAllowed.add(candidate.name());
        }
        Set<String> allowed = Collections.unmodifiableSet(mutableAllowed);

        if (data.length == 0) {
            return fallback(detector.emptyInputEncoding(), allowed, "emptyInputEncoding");
        }

        @Nullable PipelineResult result = detectBom(data);
        if (isAllowed(result, allowed)) {
            return List.of(Objects.requireNonNull(result));
        }

        result = UnicodePatternDetector.detect(data);
        if (isAllowed(result, allowed)) {
            return List.of(Objects.requireNonNull(result));
        }

        result = EscapeDetector.detect(data);
        if (isAllowed(result, allowed)) {
            return List.of(Objects.requireNonNull(result));
        }

        @Nullable PipelineResult magic = MagicDetector.detect(data);
        if (magic != null) {
            return List.of(Objects.requireNonNull(magic));
        }

        @Nullable PipelineResult utf8 = detectUtf8(data);
        @Nullable PipelineResult ascii = detectAscii(data);
        if (utf8 == null && ascii == null && isBinary(data)) {
            return List.of(BINARY_RESULT);
        }

        @Nullable PipelineResult markup = MarkupDetector.detect(data);
        if (isAllowed(markup, allowed)) {
            return List.of(MarkupDetector.promoteSuperset(data, markup, allowed));
        }
        if (isAllowed(ascii, allowed)) {
            return List.of(Objects.requireNonNull(ascii));
        }
        if (isAllowed(utf8, allowed)) {
            return List.of(Objects.requireNonNull(utf8));
        }

        List<EncodingRegistry.Info> validCandidates = ByteValidity.filter(data, candidates);
        if (validCandidates.isEmpty()) {
            return fallback(detector.noMatchEncoding(), allowed, "noMatchEncoding");
        }

        PipelineContext context = new PipelineContext();
        validCandidates = gateCjkCandidates(data, validCandidates, context);
        if (validCandidates.isEmpty()) {
            return fallback(detector.noMatchEncoding(), allowed, "noMatchEncoding");
        }

        ArrayList<ScoredEncoding> structuralScores = new ArrayList<>();
        for (EncodingRegistry.Info candidate : validCandidates) {
            if (!candidate.multibyte()) {
                continue;
            }
            @Nullable Double score = context.multibyteScores.get(candidate.name());
            if (score == null) {
                @Nullable StructuralAnalysis.Analysis analysis = StructuralAnalysis.get(
                        data,
                        candidate.name(),
                        context.analysisCache
                );
                score = analysis == null ? 0.0 : analysis.pairRatio();
            }
            if (score > 0.0) {
                structuralScores.add(new ScoredEncoding(candidate.name(), score));
            }
        }
        structuralScores.sort(Comparator.comparingDouble(ScoredEncoding::score).reversed());
        if (!structuralScores.isEmpty()
                && structuralScores.get(0).score() >= STRUCTURAL_CONFIDENCE_THRESHOLD) {
            List<PipelineResult> scored = scoreStructuralCandidates(
                    data,
                    structuralScores,
                    validCandidates,
                    context
            );
            if (!scored.isEmpty()) {
                return PostProcessor.process(data, scored);
            }
        }

        byte[] statisticalData = data.length <= STAT_SCORE_MAX_BYTES
                ? data
                : Arrays.copyOf(data, STAT_SCORE_MAX_BYTES);
        List<PipelineResult> scored = scoreCandidates(statisticalData, validCandidates);
        if (scored.isEmpty()) {
            return fallback(detector.noMatchEncoding(), allowed, "noMatchEncoding");
        }
        return PostProcessor.process(data, scored);
    }

    /// Tests whether a non-null text result's encoding is allowed.
    ///
    /// @param result  candidate result, or `null`
    /// @param allowed canonical allowed names
    /// @return whether the result exists and is allowed
    private static boolean isAllowed(
            @Nullable PipelineResult result,
            Set<String> allowed
    ) {
        return result != null && result.encoding() != null && allowed.contains(result.encoding());
    }

    /// Returns a fallback or the no-detection sentinel if it was filtered out.
    ///
    /// @param encoding   canonical fallback encoding
    /// @param allowed    canonical allowed names
    /// @param optionName option name used in a warning
    /// @return singleton internal result list
    private static List<PipelineResult> fallback(
            String encoding,
            Set<String> allowed,
            String optionName
    ) {
        if (!allowed.contains(encoding)) {
            LOGGER.log(
                    System.Logger.Level.WARNING,
                    optionName + " '" + encoding
                            + "' is excluded by includeEncodings/excludeEncodings; returning no encoding"
            );
            return List.of(NONE_RESULT);
        }
        return List.of(new PipelineResult(encoding, FALLBACK_CONFIDENCE, null, null));
    }

    /// Detects byte-order marks longest-first with UTF-32 overlap handling.
    ///
    /// @param data bytes to inspect
    /// @return BOM result, or `null`
    private static @Nullable PipelineResult detectBom(byte @Unmodifiable [] data) {
        if (startsWith(data, 0x00, 0x00, 0xfe, 0xff)) {
            if ((data.length - 4) % 4 == 0) {
                return new PipelineResult("utf-32", 1.0, null, null);
            }
        }
        if (startsWith(data, 0xff, 0xfe, 0x00, 0x00)) {
            if ((data.length - 4) % 4 == 0) {
                return new PipelineResult("utf-32", 1.0, null, null);
            }
        }
        if (startsWith(data, 0xef, 0xbb, 0xbf)) {
            return new PipelineResult("utf-8-sig", 1.0, null, null);
        }
        if (startsWith(data, 0xfe, 0xff) || startsWith(data, 0xff, 0xfe)) {
            return new PipelineResult("utf-16", 1.0, null, null);
        }
        return null;
    }

    /// Detects printable ASCII with sparse null-separator tolerance.
    ///
    /// @param data bytes to inspect
    /// @return ASCII result, or `null`
    private static @Nullable PipelineResult detectAscii(byte @Unmodifiable [] data) {
        if (data.length == 0) {
            return null;
        }
        int nulls = 0;
        for (byte value : data) {
            int unsigned = Byte.toUnsignedInt(value);
            boolean allowed = unsigned == '\t'
                    || unsigned == '\n'
                    || unsigned == '\r'
                    || (unsigned >= 0x20 && unsigned <= 0x7e);
            if (allowed) {
                continue;
            }
            if (unsigned != 0) {
                return null;
            }
            nulls++;
        }
        if (nulls == 0) {
            return new PipelineResult("ascii", 1.0, null, null);
        }
        return (double) nulls / data.length <= 0.05
                ? new PipelineResult("ascii", 0.99, null, null)
                : null;
    }

    /// Detects structurally valid non-ASCII UTF-8 with truncation tolerance.
    ///
    /// @param data bytes to inspect
    /// @return UTF-8 result, or `null`
    private static @Nullable PipelineResult detectUtf8(byte @Unmodifiable [] data) {
        if (data.length == 0) {
            return null;
        }
        int sequences = 0;
        int multibyteBytes = 0;
        for (int index = 0; index < data.length; ) {
            int first = Byte.toUnsignedInt(data[index]);
            if (first < 0x80) {
                index++;
                continue;
            }
            int length;
            if (between(first, 0xc2, 0xdf)) {
                length = 2;
            } else if (between(first, 0xe0, 0xef)) {
                length = 3;
            } else if (between(first, 0xf0, 0xf4)) {
                length = 4;
            } else {
                return null;
            }
            if (index + length > data.length) {
                break;
            }
            for (int continuation = 1; continuation < length; continuation++) {
                if (!between(Byte.toUnsignedInt(data[index + continuation]), 0x80, 0xbf)) {
                    return null;
                }
            }
            int second = Byte.toUnsignedInt(data[index + 1]);
            if ((first == 0xe0 && second < 0xa0)
                    || (first == 0xed && second > 0x9f)
                    || (first == 0xf0 && second < 0x90)
                    || (first == 0xf4 && second > 0x8f)) {
                return null;
            }
            sequences++;
            multibyteBytes += length;
            index += length;
        }
        if (sequences == 0) {
            return null;
        }
        double ratio = (double) multibyteBytes / data.length;
        double confidence = Math.min(
                0.99,
                0.80 + 0.19 * Math.min(ratio * 6.0, 1.0)
        );
        return new PipelineResult("utf-8", confidence, null, null);
    }

    /// Classifies input having more than one percent binary control bytes.
    ///
    /// @param data bounded bytes to inspect
    /// @return whether input is binary
    private static boolean isBinary(byte @Unmodifiable [] data) {
        if (data.length == 0) {
            return false;
        }
        int controls = 0;
        for (byte value : data) {
            int unsigned = Byte.toUnsignedInt(value);
            if (unsigned <= 0x08 || between(unsigned, 0x0e, 0x1f)) {
                controls++;
            }
        }
        return (double) controls / data.length > 0.01;
    }

    /// Applies all CJK structural gates and caches their metrics.
    ///
    /// @param data       analyzed bytes
    /// @param candidates byte-valid candidates
    /// @param context    per-run context
    /// @return immutable gated candidates
    private static @Unmodifiable List<EncodingRegistry.Info> gateCjkCandidates(
            byte @Unmodifiable [] data,
            List<EncodingRegistry.Info> candidates,
            PipelineContext context
    ) {
        ArrayList<EncodingRegistry.Info> gated = new ArrayList<>(candidates.size());
        for (EncodingRegistry.Info candidate : candidates) {
            if (candidate.multibyte()) {
                @Nullable StructuralAnalysis.Analysis analysis = StructuralAnalysis.get(
                        data,
                        candidate.name(),
                        context.analysisCache
                );
                double ratio = analysis == null ? 0.0 : analysis.pairRatio();
                context.multibyteScores.put(candidate.name(), ratio);
                if (ratio < CJK_MIN_MULTIBYTE_RATIO) {
                    continue;
                }
                if (context.nonAsciiCount == null) {
                    context.nonAsciiCount = countNonAscii(data);
                }
                if (context.nonAsciiCount < CJK_MIN_NON_ASCII) {
                    continue;
                }
                double coverage = analysis == null
                        ? 0.0
                        : (double) analysis.multibyteBytes() / context.nonAsciiCount;
                context.multibyteCoverage.put(candidate.name(), coverage);
                if (coverage < CJK_MIN_BYTE_COVERAGE) {
                    continue;
                }
                if (context.nonAsciiCount >= CJK_DIVERSITY_MIN_NON_ASCII
                        && (analysis == null
                        ? 256
                        : analysis.leadDiversity()) < CJK_MIN_LEAD_DIVERSITY) {
                    continue;
                }
            }
            gated.add(candidate);
        }
        return List.copyOf(gated);
    }

    /// Scores CJK candidates ordered by structure together with valid single-byte candidates.
    ///
    /// @param data             analyzed bytes
    /// @param structuralScores sorted CJK structural scores
    /// @param validCandidates  all gated candidates
    /// @param context          per-run context
    /// @return statistically ranked and coverage-boosted candidates
    private static List<PipelineResult> scoreStructuralCandidates(
            byte @Unmodifiable [] data,
            List<ScoredEncoding> structuralScores,
            List<EncodingRegistry.Info> validCandidates,
            PipelineContext context
    ) {
        HashMap<String, EncodingRegistry.Info> multibyte = new HashMap<>();
        for (EncodingRegistry.Info candidate : validCandidates) {
            if (candidate.multibyte()) {
                multibyte.put(candidate.name(), candidate);
            }
        }
        ArrayList<EncodingRegistry.Info> ordered = new ArrayList<>(validCandidates.size());
        for (ScoredEncoding scored : structuralScores) {
            @Nullable EncodingRegistry.Info candidate = multibyte.get(scored.encoding());
            if (candidate != null) {
                ordered.add(candidate);
            }
        }
        for (EncodingRegistry.Info candidate : validCandidates) {
            if (!candidate.multibyte()) {
                ordered.add(candidate);
            }
        }
        byte[] statisticalData = data.length <= STAT_SCORE_MAX_BYTES
                ? data
                : Arrays.copyOf(data, STAT_SCORE_MAX_BYTES);
        List<PipelineResult> scored = scoreCandidates(statisticalData, ordered);
        ArrayList<PipelineResult> boosted = new ArrayList<>(scored.size());
        for (PipelineResult result : scored) {
            double coverage = result.encoding() == null
                    ? 0.0
                    : context.multibyteCoverage.getOrDefault(result.encoding(), 0.0);
            boosted.add(coverage >= 0.95
                    ? new PipelineResult(
                    result.encoding(),
                    result.confidence() * (1.0 + coverage),
                    result.language(),
                    result.mimeType()
            )
                    : result);
        }
        boosted.sort(Comparator.comparingDouble(PipelineResult::confidence).reversed());
        return boosted;
    }

    /// Scores candidates with one shared IDF-weighted bigram profile.
    ///
    /// @param data       bounded statistical bytes
    /// @param candidates candidates in tie-breaking order
    /// @return stable descending positive-score results
    private static List<PipelineResult> scoreCandidates(
            byte @Unmodifiable [] data,
            List<EncodingRegistry.Info> candidates
    ) {
        if (data.length == 0 || candidates.isEmpty()) {
            return List.of();
        }
        ModelStore.Profile profile = ModelStore.profile(data);
        ArrayList<PipelineResult> results = new ArrayList<>(candidates.size());
        for (EncodingRegistry.Info candidate : candidates) {
            ModelStore.Score score = ModelStore.scoreBestLanguage(candidate.name(), profile);
            if (score.score() > 0.0) {
                results.add(new PipelineResult(
                        candidate.name(),
                        score.score(),
                        score.language(),
                        null
                ));
            }
        }
        results.sort(Comparator.comparingDouble(PipelineResult::confidence).reversed());
        return results;
    }

    /// Fills absent languages through registry, direct-model, and UTF-8-model tiers.
    ///
    /// @param original complete original input
    /// @param results  pipeline results
    /// @return results with languages filled where possible
    private static List<PipelineResult> fillLanguages(
            byte @Unmodifiable [] original,
            List<PipelineResult> results
    ) {
        byte[] data = original.length <= LANGUAGE_SCORE_MAX_BYTES
                ? original
                : Arrays.copyOf(original, LANGUAGE_SCORE_MAX_BYTES);
        ArrayList<PipelineResult> filled = new ArrayList<>(results.size());
        @Nullable ModelStore.Profile profile = null;
        @Nullable ModelStore.Profile utf8Profile = null;
        for (PipelineResult result : results) {
            if (result.language() != null || result.encoding() == null) {
                filled.add(result);
                continue;
            }
            String encoding = result.encoding();
            @Nullable String language = ModelStore.inferSingleLanguage(encoding);
            if (language == null && data.length > 0 && ModelStore.hasVariants(encoding)) {
                if (profile == null) {
                    profile = ModelStore.profile(data);
                }
                language = ModelStore.scoreBestLanguage(encoding, profile).language();
            }
            if (language == null && data.length > 0 && ModelStore.hasVariants("utf-8")) {
                byte @Nullable [] utf8Data = TextDecoder.toUtf8(data, encoding);
                if (utf8Data != null && utf8Data.length > 0) {
                    if (utf8Profile == null || !encoding.equals("utf-8")) {
                        utf8Profile = ModelStore.profile(utf8Data);
                    }
                    language = ModelStore.scoreBestLanguage("utf-8", utf8Profile).language();
                }
            }
            filled.add(language == null
                    ? result
                    : new PipelineResult(
                    result.encoding(),
                    result.confidence(),
                    language,
                    result.mimeType()
            ));
        }
        return filled;
    }

    /// Applies preferred-superset and compatibility-name transformations.
    ///
    /// @param canonical canonical encoding, or `null`
    /// @param detector  detector configuration
    /// @return transformed name, or `null`
    private static @Nullable String transformName(
            @Nullable String canonical,
            EncodingDetector detector
    ) {
        if (canonical == null) {
            return null;
        }
        String transformed = detector.preferSuperset()
                ? PREFERRED_SUPERSETS.getOrDefault(canonical, canonical)
                : canonical;
        return detector.nameStyle() == EncodingNameStyle.CHARDET_COMPATIBLE
                ? COMPATIBLE_NAMES.getOrDefault(transformed, transformed)
                : transformed;
    }

    /// Counts bytes having their high bit set.
    ///
    /// @param data bytes to inspect
    /// @return non-ASCII count
    private static int countNonAscii(byte @Unmodifiable [] data) {
        int count = 0;
        for (byte value : data) {
            count += value < 0 ? 1 : 0;
        }
        return count;
    }

    /// Tests an inclusive integer range.
    ///
    /// @param value   value to test
    /// @param minimum inclusive minimum
    /// @param maximum inclusive maximum
    /// @return whether the value lies in the range
    private static boolean between(int value, int minimum, int maximum) {
        return value >= minimum && value <= maximum;
    }

    /// Tests an unsigned byte prefix.
    ///
    /// @param data   source bytes
    /// @param prefix unsigned prefix bytes
    /// @return whether it matches
    private static boolean startsWith(byte @Unmodifiable [] data, int @Unmodifiable ... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (Byte.toUnsignedInt(data[index]) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    /// Stores per-invocation structural caches and counters.
    @NotNullByDefault
    private static final class PipelineContext {
        /// Cached analyzer triples by canonical encoding.
        private final Map<String, StructuralAnalysis.Analysis> analysisCache = new HashMap<>();

        /// Cached structural pair ratios.
        private final Map<String, Double> multibyteScores = new HashMap<>();

        /// Cached non-ASCII byte coverage ratios.
        private final Map<String, Double> multibyteCoverage = new HashMap<>();

        /// Lazily counted non-ASCII bytes.
        private @Nullable Integer nonAsciiCount;

        /// Creates an empty per-invocation context.
        private PipelineContext() {
        }
    }

    /// Associates a canonical encoding with a structural score.
    ///
    /// @param encoding canonical encoding
    /// @param score    structural pair ratio
    @NotNullByDefault
    private record ScoredEncoding(String encoding, double score) {
        /// Creates a structural score entry.
        private ScoredEncoding {
        }
    }
}
