// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Extracts XML, HTML, and PEP 263 charset declarations.
@NotNullByDefault
final class MarkupDetector {
    /// Maximum declaration and validation prefix length.
    private static final int SCAN_LIMIT = 4096;

    /// XML declaration pattern.
    private static final Pattern XML_ENCODING = Pattern.compile(
            "<\\?xml[^>]+encoding\\s*=\\s*['\"]([^'\"]+)['\"]",
            Pattern.CASE_INSENSITIVE
    );

    /// HTML5 `meta charset` pattern.
    private static final Pattern HTML5_CHARSET = Pattern.compile(
            "<meta[^>]+charset\\s*=\\s*['\"]?\\s*([^\\s'\">;]+)",
            Pattern.CASE_INSENSITIVE
    );

    /// HTML4 content-type metadata pattern.
    private static final Pattern HTML4_CONTENT_TYPE = Pattern.compile(
            "<meta[^>]+content\\s*=\\s*['\"][^'\"]*charset=([^\\s'\">;]+)",
            Pattern.CASE_INSENSITIVE
    );

    /// PEP 263 coding-cookie pattern.
    private static final Pattern PEP_263 = Pattern.compile(
            "(?m)^[ \\t\\f]*#.*?coding[:=][ \\t]*([-\\w.]+)"
    );

    /// Deterministic declaration confidence.
    private static final double CONFIDENCE = 0.95;

    /// Prevents instantiation of this static stage.
    private MarkupDetector() {
    }

    /// Detects a valid declaration in the input prefix.
    ///
    /// @param data bytes to inspect
    /// @return declared result, or `null`
    static @Nullable PipelineResult detect(byte @Unmodifiable [] data) {
        if (data.length == 0) {
            return null;
        }
        String head = new String(
                data,
                0,
                Math.min(data.length, SCAN_LIMIT),
                StandardCharsets.ISO_8859_1
        );
        @Nullable PipelineResult result = matchDeclaration(data, head, XML_ENCODING, "text/xml");
        if (result != null) {
            return result;
        }
        result = matchDeclaration(data, head, HTML5_CHARSET, "text/html");
        if (result != null) {
            return result;
        }
        result = matchDeclaration(data, head, HTML4_CONTENT_TYPE, "text/html");
        return result == null ? detectPep263(data) : result;
    }

    /// Promotes declared Shift_JIS or EUC-KR to a structurally better superset.
    ///
    /// @param data    complete analyzed bytes
    /// @param markup  declared result
    /// @param allowed canonical allowed names
    /// @return promoted or original result
    static PipelineResult promoteSuperset(
            byte @Unmodifiable [] data,
            PipelineResult markup,
            Set<String> allowed
    ) {
        @Nullable String encoding = markup.encoding();
        if (encoding == null) {
            return markup;
        }
        @Nullable String superset = switch (encoding) {
            case "shift_jis_2004" -> "cp932";
            case "euc_kr" -> "cp949";
            default -> null;
        };
        if (superset == null || !allowed.contains(superset) || !ByteValidity.isValid(data, superset)) {
            return markup;
        }
        Map<String, StructuralAnalysis.Analysis> cache = new HashMap<>();
        @Nullable StructuralAnalysis.Analysis baseAnalysis = StructuralAnalysis.get(data, encoding, cache);
        @Nullable StructuralAnalysis.Analysis supersetAnalysis = StructuralAnalysis.get(data, superset, cache);
        double baseScore = baseAnalysis == null ? 0.0 : baseAnalysis.pairRatio();
        double supersetScore = supersetAnalysis == null ? 0.0 : supersetAnalysis.pairRatio();
        if (supersetScore > baseScore) {
            return new PipelineResult(
                    superset,
                    markup.confidence(),
                    markup.language(),
                    markup.mimeType()
            );
        }
        return markup;
    }

    /// Matches and validates one markup declaration pattern.
    ///
    /// @param data     source bytes
    /// @param head     byte-preserving prefix text
    /// @param pattern  declaration pattern
    /// @param mimeType result MIME type
    /// @return result, or `null`
    private static @Nullable PipelineResult matchDeclaration(
            byte @Unmodifiable [] data,
            String head,
            Pattern pattern,
            String mimeType
    ) {
        Matcher matcher = pattern.matcher(head);
        if (!matcher.find()) {
            return null;
        }
        @Nullable String encoding = resolveAsciiName(matcher.group(1));
        if (encoding == null || !validPrefix(data, encoding)) {
            return null;
        }
        return new PipelineResult(encoding, CONFIDENCE, null, mimeType);
    }

    /// Detects a PEP 263 declaration in the first two physical lines.
    ///
    /// @param data source bytes
    /// @return result, or `null`
    private static @Nullable PipelineResult detectPep263(byte @Unmodifiable [] data) {
        int quickLimit = Math.min(data.length, 200);
        boolean hasComment = false;
        for (int index = 0; index < quickLimit; index++) {
            if (data[index] == '#') {
                hasComment = true;
                break;
            }
        }
        if (!hasComment) {
            return null;
        }

        int newlineCount = 0;
        int end = data.length;
        for (int index = 0; index < data.length; index++) {
            if (data[index] == '\n' && ++newlineCount == 2) {
                end = index;
                break;
            }
        }
        String firstTwoLines = new String(data, 0, end, StandardCharsets.ISO_8859_1);
        Matcher matcher = PEP_263.matcher(firstTwoLines);
        if (!matcher.find()) {
            return null;
        }
        @Nullable String encoding = resolveAsciiName(matcher.group(1));
        if (encoding == null || !validPrefix(data, encoding)) {
            return null;
        }
        return new PipelineResult(encoding, CONFIDENCE, null, "text/x-python");
    }

    /// Resolves an ASCII declaration name.
    ///
    /// @param raw captured byte-preserving name
    /// @return canonical encoding, or `null`
    private static @Nullable String resolveAsciiName(String raw) {
        String stripped = raw.strip();
        for (int index = 0; index < stripped.length(); index++) {
            if (stripped.charAt(index) > 0x7f) {
                return null;
            }
        }
        return EncodingRegistry.lookup(stripped);
    }

    /// Strictly validates at most the first 4 KiB under an encoding.
    ///
    /// @param data     source bytes
    /// @param encoding canonical encoding
    /// @return whether the prefix is valid
    private static boolean validPrefix(byte @Unmodifiable [] data, String encoding) {
        byte[] prefix = data.length <= SCAN_LIMIT ? data : Arrays.copyOf(data, SCAN_LIMIT);
        return ByteValidity.isValid(prefix, encoding);
    }
}
