// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.Encoding;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.ByteBuffer;
import java.util.EnumMap;
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
    /// @param data normalized read-only bytes to inspect
    /// @return declared result, or `null`
    static @Nullable PipelineResult detect(@UnmodifiableView ByteBuffer data) {
        if (!data.hasRemaining()) {
            return null;
        }
        @UnmodifiableView ByteBuffer prefix = ByteBufferSupport.prefix(data, SCAN_LIMIT);
        String head = ByteBufferSupport.latin1String(
                prefix,
                0,
                prefix.remaining()
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
    /// @param data    normalized read-only analyzed bytes
    /// @param markup  declared result
    /// @param allowed allowed encoding identities
    /// @return promoted or original result
    static PipelineResult promoteSuperset(
            @UnmodifiableView ByteBuffer data,
            PipelineResult markup,
            Set<Encoding> allowed
    ) {
        @Nullable Encoding encoding = markup.encoding();
        if (encoding == null) {
            return markup;
        }
        @Nullable Encoding superset = switch (encoding) {
            case SHIFT_JIS_2004 -> Encoding.CP932;
            case EUC_KR -> Encoding.CP949;
            default -> null;
        };
        if (superset == null || !allowed.contains(superset) || !ByteValidity.isValid(data, superset)) {
            return markup;
        }
        Map<Encoding, StructuralAnalysis.Analysis> cache = new EnumMap<>(Encoding.class);
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
            @UnmodifiableView ByteBuffer data,
            String head,
            Pattern pattern,
            String mimeType
    ) {
        Matcher matcher = pattern.matcher(head);
        if (!matcher.find()) {
            return null;
        }
        @Nullable Encoding encoding = resolveAsciiName(matcher.group(1));
        if (encoding == null || !validPrefix(data, encoding)) {
            return null;
        }
        return new PipelineResult(encoding, CONFIDENCE, null, mimeType);
    }

    /// Detects a PEP 263 declaration in the first two physical lines.
    ///
    /// @param data source bytes
    /// @return result, or `null`
    private static @Nullable PipelineResult detectPep263(
            @UnmodifiableView ByteBuffer data
    ) {
        int quickLimit = Math.min(data.remaining(), 200);
        boolean hasComment = false;
        for (int index = 0; index < quickLimit; index++) {
            if (data.get(index) == '#') {
                hasComment = true;
                break;
            }
        }
        if (!hasComment) {
            return null;
        }

        int newlineCount = 0;
        int end = data.remaining();
        for (int index = 0; index < data.remaining(); index++) {
            if (data.get(index) == '\n' && ++newlineCount == 2) {
                end = index;
                break;
            }
        }
        String firstTwoLines = ByteBufferSupport.latin1String(data, 0, end);
        Matcher matcher = PEP_263.matcher(firstTwoLines);
        if (!matcher.find()) {
            return null;
        }
        @Nullable Encoding encoding = resolveAsciiName(matcher.group(1));
        if (encoding == null || !validPrefix(data, encoding)) {
            return null;
        }
        return new PipelineResult(encoding, CONFIDENCE, null, "text/x-python");
    }

    /// Resolves an ASCII declaration name.
    ///
    /// @param raw captured byte-preserving name
    /// @return encoding identity, or `null`
    private static @Nullable Encoding resolveAsciiName(String raw) {
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
    /// @param encoding encoding identity
    /// @return whether the prefix is valid
    private static boolean validPrefix(
            @UnmodifiableView ByteBuffer data,
            Encoding encoding
    ) {
        return ByteValidity.isValid(ByteBufferSupport.prefix(data, SCAN_LIMIT), encoding);
    }
}
