// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.EncodingDetector.Encoding;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/// Extracts XML, HTML, and PEP 263 charset declarations.
@NotNullByDefault
final class MarkupDetector {
    /// Maximum declaration and validation prefix length.
    private static final int SCAN_LIMIT = 4096;

    /// Case-insensitive XML declaration prefix.
    private static final String XML_PREFIX = "<?xml";

    /// Case-insensitive HTML metadata prefix.
    private static final String META_PREFIX = "<meta";

    /// Case-insensitive XML encoding attribute name.
    private static final String ENCODING_ATTRIBUTE = "encoding";

    /// Case-insensitive HTML charset attribute name.
    private static final String CHARSET_ATTRIBUTE = "charset";

    /// Case-insensitive HTML content attribute name.
    private static final String CONTENT_ATTRIBUTE = "content";

    /// Case-insensitive charset assignment inside an HTML content value.
    private static final String CHARSET_ASSIGNMENT = "charset=";

    /// Case-sensitive PEP 263 coding marker.
    private static final String CODING_MARKER = "coding";

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
        int start = data.position();
        int end = start + Math.min(data.remaining(), SCAN_LIMIT);
        @Nullable PipelineResult result = detectXmlDeclaration(data, start, end);
        if (result != null) {
            return result;
        }
        result = detectHtml5Declaration(data, start, end);
        if (result != null) {
            return result;
        }
        result = detectHtml4Declaration(data, start, end);
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

    /// Detects the first XML encoding declaration in a byte range.
    ///
    /// Attribute candidates are examined from right to left within each XML
    /// declaration to preserve the greediness of the reference byte pattern.
    ///
    /// @param data  source bytes
    /// @param start inclusive absolute scan index
    /// @param end   exclusive absolute scan index
    /// @return declared result, or `null`
    private static @Nullable PipelineResult detectXmlDeclaration(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end
    ) {
        int declarationStart = indexOfAsciiIgnoreCase(data, start, end, XML_PREFIX);
        while (declarationStart >= 0) {
            int bodyStart = declarationStart + XML_PREFIX.length();
            int bodyEnd = indexOf(data, bodyStart, end, (byte) '>');
            if (bodyEnd < 0) {
                bodyEnd = end;
            }

            int attributeStart = lastIndexOfAsciiIgnoreCase(
                    data,
                    bodyStart + 1,
                    bodyEnd,
                    ENCODING_ATTRIBUTE
            );
            while (attributeStart >= 0) {
                int cursor = skipRegexWhitespace(
                        data,
                        attributeStart + ENCODING_ATTRIBUTE.length(),
                        end
                );
                if (cursor < end && data.get(cursor) == '=') {
                    cursor = skipRegexWhitespace(data, cursor + 1, end);
                    if (cursor < end && isQuote(data.get(cursor))) {
                        int nameStart = cursor + 1;
                        int nameEnd = indexOfQuote(data, nameStart, end);
                        if (nameEnd > nameStart) {
                            return createDeclaration(data, nameStart, nameEnd, "text/xml");
                        }
                    }
                }
                attributeStart = lastIndexOfAsciiIgnoreCase(
                        data,
                        bodyStart + 1,
                        attributeStart,
                        ENCODING_ATTRIBUTE
                );
            }
            declarationStart = indexOfAsciiIgnoreCase(
                    data,
                    declarationStart + 1,
                    end,
                    XML_PREFIX
            );
        }
        return null;
    }

    /// Detects the first HTML `meta charset` declaration in a byte range.
    ///
    /// Attribute candidates are examined from right to left within each tag to
    /// preserve the greediness and backtracking of the reference byte pattern.
    ///
    /// @param data  source bytes
    /// @param start inclusive absolute scan index
    /// @param end   exclusive absolute scan index
    /// @return declared result, or `null`
    private static @Nullable PipelineResult detectHtml5Declaration(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end
    ) {
        int metaStart = indexOfAsciiIgnoreCase(data, start, end, META_PREFIX);
        while (metaStart >= 0) {
            int bodyStart = metaStart + META_PREFIX.length();
            int bodyEnd = indexOf(data, bodyStart, end, (byte) '>');
            if (bodyEnd < 0) {
                bodyEnd = end;
            }

            int attributeStart = lastIndexOfAsciiIgnoreCase(
                    data,
                    bodyStart + 1,
                    bodyEnd,
                    CHARSET_ATTRIBUTE
            );
            while (attributeStart >= 0) {
                int cursor = skipRegexWhitespace(
                        data,
                        attributeStart + CHARSET_ATTRIBUTE.length(),
                        end
                );
                if (cursor < end && data.get(cursor) == '=') {
                    cursor = skipRegexWhitespace(data, cursor + 1, end);
                    if (cursor < end && isQuote(data.get(cursor))) {
                        cursor++;
                    }
                    cursor = skipRegexWhitespace(data, cursor, end);
                    int nameEnd = htmlCharsetNameEnd(data, cursor, end);
                    if (nameEnd > cursor) {
                        return createDeclaration(data, cursor, nameEnd, "text/html");
                    }
                }
                attributeStart = lastIndexOfAsciiIgnoreCase(
                        data,
                        bodyStart + 1,
                        attributeStart,
                        CHARSET_ATTRIBUTE
                );
            }
            metaStart = indexOfAsciiIgnoreCase(data, metaStart + 1, end, META_PREFIX);
        }
        return null;
    }

    /// Detects the first HTML content-type charset declaration in a byte range.
    ///
    /// Both content attributes and charset assignments are examined from right
    /// to left to reproduce the two greedy reference pattern segments.
    ///
    /// @param data  source bytes
    /// @param start inclusive absolute scan index
    /// @param end   exclusive absolute scan index
    /// @return declared result, or `null`
    private static @Nullable PipelineResult detectHtml4Declaration(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end
    ) {
        int metaStart = indexOfAsciiIgnoreCase(data, start, end, META_PREFIX);
        while (metaStart >= 0) {
            int bodyStart = metaStart + META_PREFIX.length();
            int bodyEnd = indexOf(data, bodyStart, end, (byte) '>');
            if (bodyEnd < 0) {
                bodyEnd = end;
            }

            int contentStart = lastIndexOfAsciiIgnoreCase(
                    data,
                    bodyStart + 1,
                    bodyEnd,
                    CONTENT_ATTRIBUTE
            );
            while (contentStart >= 0) {
                int cursor = skipRegexWhitespace(
                        data,
                        contentStart + CONTENT_ATTRIBUTE.length(),
                        end
                );
                if (cursor < end && data.get(cursor) == '=') {
                    cursor = skipRegexWhitespace(data, cursor + 1, end);
                    if (cursor < end && isQuote(data.get(cursor))) {
                        int valueStart = cursor + 1;
                        int valueEnd = indexOfQuote(data, valueStart, end);
                        if (valueEnd < 0) {
                            valueEnd = end;
                        }
                        int assignmentStart = lastIndexOfAsciiIgnoreCase(
                                data,
                                valueStart,
                                valueEnd,
                                CHARSET_ASSIGNMENT
                        );
                        while (assignmentStart >= 0) {
                            int nameStart = assignmentStart + CHARSET_ASSIGNMENT.length();
                            int nameEnd = htmlCharsetNameEnd(data, nameStart, valueEnd);
                            if (nameEnd > nameStart) {
                                return createDeclaration(
                                        data,
                                        nameStart,
                                        nameEnd,
                                        "text/html"
                                );
                            }
                            assignmentStart = lastIndexOfAsciiIgnoreCase(
                                    data,
                                    valueStart,
                                    assignmentStart,
                                    CHARSET_ASSIGNMENT
                            );
                        }
                    }
                }
                contentStart = lastIndexOfAsciiIgnoreCase(
                        data,
                        bodyStart + 1,
                        contentStart,
                        CONTENT_ATTRIBUTE
                );
            }
            metaStart = indexOfAsciiIgnoreCase(data, metaStart + 1, end, META_PREFIX);
        }
        return null;
    }

    /// Detects a PEP 263 declaration in the first two physical lines.
    ///
    /// @param data source bytes
    /// @return result, or `null`
    private static @Nullable PipelineResult detectPep263(
            @UnmodifiableView ByteBuffer data
    ) {
        int start = data.position();
        int end = start + data.remaining();
        int quickEnd = start + Math.min(data.remaining(), 200);
        if (indexOf(data, start, quickEnd, (byte) '#') < 0) {
            return null;
        }

        int newlineCount = 0;
        int firstTwoLinesEnd = end;
        for (int index = start; index < end; index++) {
            if (data.get(index) == '\n' && ++newlineCount == 2) {
                firstTwoLinesEnd = index;
                break;
            }
        }

        int lineStart = start;
        while (lineStart <= firstTwoLinesEnd) {
            int lineEnd = indexOf(data, lineStart, firstTwoLinesEnd, (byte) '\n');
            if (lineEnd < 0) {
                lineEnd = firstTwoLinesEnd;
            }
            int commentStart = skipPepLeadingWhitespace(data, lineStart, lineEnd);
            if (commentStart < lineEnd && data.get(commentStart) == '#') {
                int markerStart = indexOfCodingMarker(
                        data,
                        commentStart + 1,
                        lineEnd
                );
                while (markerStart >= 0) {
                    int separator = markerStart + CODING_MARKER.length();
                    if (separator < lineEnd
                            && (data.get(separator) == ':' || data.get(separator) == '=')) {
                        int nameStart = skipPepSeparatorWhitespace(
                                data,
                                separator + 1,
                                lineEnd
                        );
                        int nameEnd = pepEncodingNameEnd(data, nameStart, lineEnd);
                        if (nameEnd > nameStart) {
                            return createDeclaration(
                                    data,
                                    nameStart,
                                    nameEnd,
                                    "text/x-python"
                            );
                        }
                    }
                    markerStart = indexOfCodingMarker(
                            data,
                            markerStart + 1,
                            lineEnd
                    );
                }
            }
            if (lineEnd == firstTwoLinesEnd) {
                break;
            }
            lineStart = lineEnd + 1;
        }
        return null;
    }

    /// Resolves and validates one captured declaration name.
    ///
    /// @param data      source bytes
    /// @param nameStart inclusive absolute name index
    /// @param nameEnd   exclusive absolute name index
    /// @param mimeType  result MIME type
    /// @return declared result, or `null`
    private static @Nullable PipelineResult createDeclaration(
            @UnmodifiableView ByteBuffer data,
            int nameStart,
            int nameEnd,
            String mimeType
    ) {
        @Nullable Encoding encoding = resolveAsciiName(data, nameStart, nameEnd);
        if (encoding == null || !validPrefix(data, encoding)) {
            return null;
        }
        return new PipelineResult(encoding, CONFIDENCE, null, mimeType);
    }

    /// Resolves an ASCII declaration name captured from the input buffer.
    ///
    /// Only the trimmed captured name is materialized as a string for registry
    /// lookup; the scanned input prefix remains in its original byte storage.
    ///
    /// @param data  source bytes
    /// @param start inclusive absolute name index
    /// @param end   exclusive absolute name index
    /// @return encoding identity, or `null`
    private static @Nullable Encoding resolveAsciiName(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end
    ) {
        while (start < end && isAsciiStripWhitespace(data.get(start))) {
            start++;
        }
        while (start < end && isAsciiStripWhitespace(data.get(end - 1))) {
            end--;
        }
        if (start == end) {
            return null;
        }

        for (int index = start; index < end; index++) {
            int value = Byte.toUnsignedInt(data.get(index));
            if (value > 0x7f || value == 0) {
                return null;
            }
        }
        return Encoding.lookup(
                ByteBufferSupport.latin1String(data, start, end - start)
        );
    }

    /// Finds one byte in an absolute buffer range.
    ///
    /// @param data  source bytes
    /// @param start inclusive absolute search index
    /// @param end   exclusive absolute search index
    /// @param value byte to find
    /// @return matching index, or `-1`
    private static int indexOf(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end,
            byte value
    ) {
        for (int index = start; index < end; index++) {
            if (data.get(index) == value) {
                return index;
            }
        }
        return -1;
    }

    /// Finds the case-sensitive PEP 263 coding marker in a buffer range.
    ///
    /// @param data  source bytes
    /// @param start inclusive absolute search index
    /// @param end   exclusive absolute search index
    /// @return matching index, or `-1`
    private static int indexOfCodingMarker(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end
    ) {
        int lastStart = end - CODING_MARKER.length();
        for (int index = start; index <= lastStart; index++) {
            int offset = 0;
            while (offset < CODING_MARKER.length()
                    && Byte.toUnsignedInt(data.get(index + offset))
                    == CODING_MARKER.charAt(offset)) {
                offset++;
            }
            if (offset == CODING_MARKER.length()) {
                return index;
            }
        }
        return -1;
    }

    /// Finds a case-insensitive lower-case ASCII literal in a buffer range.
    ///
    /// @param data    source bytes
    /// @param start   inclusive absolute search index
    /// @param end     exclusive absolute search index
    /// @param literal lower-case ASCII literal
    /// @return matching index, or `-1`
    private static int indexOfAsciiIgnoreCase(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end,
            String literal
    ) {
        int lastStart = end - literal.length();
        for (int index = start; index <= lastStart; index++) {
            if (matchesAsciiIgnoreCase(data, index, literal)) {
                return index;
            }
        }
        return -1;
    }

    /// Finds the last case-insensitive lower-case ASCII literal in a range.
    ///
    /// @param data    source bytes
    /// @param start   inclusive absolute search index
    /// @param end     exclusive absolute search index
    /// @param literal lower-case ASCII literal
    /// @return matching index, or `-1`
    private static int lastIndexOfAsciiIgnoreCase(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end,
            String literal
    ) {
        for (int index = end - literal.length(); index >= start; index--) {
            if (matchesAsciiIgnoreCase(data, index, literal)) {
                return index;
            }
        }
        return -1;
    }

    /// Tests a lower-case ASCII literal at one absolute buffer index.
    ///
    /// @param data    source bytes
    /// @param start   absolute comparison index
    /// @param literal lower-case ASCII literal
    /// @return whether the literal matches with ASCII case folding
    private static boolean matchesAsciiIgnoreCase(
            @UnmodifiableView ByteBuffer data,
            int start,
            String literal
    ) {
        for (int offset = 0; offset < literal.length(); offset++) {
            int value = Byte.toUnsignedInt(data.get(start + offset));
            if (value >= 'A' && value <= 'Z') {
                value += 'a' - 'A';
            }
            if (value != literal.charAt(offset)) {
                return false;
            }
        }
        return true;
    }

    /// Skips whitespace recognized by Python byte regular expressions.
    ///
    /// @param data  source bytes
    /// @param start inclusive absolute scan index
    /// @param end   exclusive absolute scan index
    /// @return first non-whitespace index, or `end`
    private static int skipRegexWhitespace(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end
    ) {
        while (start < end && isRegexWhitespace(data.get(start))) {
            start++;
        }
        return start;
    }

    /// Skips whitespace permitted before a PEP 263 comment marker.
    ///
    /// @param data  source bytes
    /// @param start inclusive absolute scan index
    /// @param end   exclusive absolute scan index
    /// @return first non-whitespace index, or `end`
    private static int skipPepLeadingWhitespace(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end
    ) {
        while (start < end) {
            byte value = data.get(start);
            if (value != ' ' && value != '\t' && value != '\f') {
                break;
            }
            start++;
        }
        return start;
    }

    /// Skips horizontal whitespace after a PEP 263 separator.
    ///
    /// @param data  source bytes
    /// @param start inclusive absolute scan index
    /// @param end   exclusive absolute scan index
    /// @return first non-whitespace index, or `end`
    private static int skipPepSeparatorWhitespace(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end
    ) {
        while (start < end) {
            byte value = data.get(start);
            if (value != ' ' && value != '\t') {
                break;
            }
            start++;
        }
        return start;
    }

    /// Finds the end of an HTML charset name.
    ///
    /// @param data  source bytes
    /// @param start inclusive absolute name index
    /// @param end   exclusive absolute scan index
    /// @return exclusive absolute name end
    private static int htmlCharsetNameEnd(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end
    ) {
        while (start < end) {
            byte value = data.get(start);
            if (isRegexWhitespace(value)
                    || value == '\''
                    || value == '"'
                    || value == '>'
                    || value == ';') {
                break;
            }
            start++;
        }
        return start;
    }

    /// Finds the end of a PEP 263 encoding name.
    ///
    /// @param data  source bytes
    /// @param start inclusive absolute name index
    /// @param end   exclusive absolute line index
    /// @return exclusive absolute name end
    private static int pepEncodingNameEnd(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end
    ) {
        while (start < end) {
            int value = Byte.toUnsignedInt(data.get(start));
            boolean word = (value >= 'a' && value <= 'z')
                    || (value >= 'A' && value <= 'Z')
                    || (value >= '0' && value <= '9')
                    || value == '_';
            if (!word && value != '-' && value != '.') {
                break;
            }
            start++;
        }
        return start;
    }

    /// Finds the next single- or double-quote byte.
    ///
    /// @param data  source bytes
    /// @param start inclusive absolute scan index
    /// @param end   exclusive absolute scan index
    /// @return quote index, or `-1`
    private static int indexOfQuote(
            @UnmodifiableView ByteBuffer data,
            int start,
            int end
    ) {
        for (int index = start; index < end; index++) {
            if (isQuote(data.get(index))) {
                return index;
            }
        }
        return -1;
    }

    /// Tests whether a byte is a single or double quote.
    ///
    /// @param value byte to test
    /// @return whether `value` is a quote
    private static boolean isQuote(byte value) {
        return value == '\'' || value == '"';
    }

    /// Tests whether a byte is whitespace under Python byte-regex rules.
    ///
    /// @param value byte to test
    /// @return whether `value` is ASCII regex whitespace
    private static boolean isRegexWhitespace(byte value) {
        int unsigned = Byte.toUnsignedInt(value);
        return unsigned == ' ' || (unsigned >= '\t' && unsigned <= '\r');
    }

    /// Tests whether an ASCII byte is removed by string-style stripping.
    ///
    /// @param value byte to test
    /// @return whether `value` is ASCII stripping whitespace
    private static boolean isAsciiStripWhitespace(byte value) {
        int unsigned = Byte.toUnsignedInt(value);
        return unsigned == ' '
                || (unsigned >= '\t' && unsigned <= '\r')
                || (unsigned >= 0x1c && unsigned <= 0x1f);
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
