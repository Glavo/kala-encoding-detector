// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/// Detects BOM-less UTF-16 and UTF-32 from positional null-byte patterns.
@NotNullByDefault
final class UnicodePatternDetector {
    /// Maximum pattern-analysis sample length.
    private static final int SAMPLE_SIZE = 4096;

    /// Minimum bytes for UTF-32 pattern analysis.
    private static final int MIN_UTF32_BYTES = 16;

    /// Minimum bytes for UTF-16 pattern analysis.
    private static final int MIN_UTF16_BYTES = 10;

    /// Minimum expected-position null fraction for UTF-16.
    private static final double UTF16_MIN_NULL_FRACTION = 0.03;

    /// Minimum quality when both UTF-16 endiannesses match.
    private static final double MIN_TEXT_QUALITY = 0.5;

    /// Minimum printable fraction for decoded samples.
    private static final double MIN_PRINTABLE_FRACTION = 0.7;

    /// Positional null threshold below which ASCII separator data is rejected.
    private static final double NULL_SEPARATOR_MAX_FRACTION = 0.15;

    /// Deterministic confidence for accepted patterns.
    private static final double CONFIDENCE = 0.95;

    /// Prevents instantiation of this static stage.
    private UnicodePatternDetector() {
    }

    /// Detects a BOM-less UTF-32 or UTF-16 pattern.
    ///
    /// @param data bytes to examine
    /// @return deterministic result, or `null`
    static @Nullable PipelineResult detect(byte @Unmodifiable [] data) {
        byte[] sample = data.length <= SAMPLE_SIZE ? data : Arrays.copyOf(data, SAMPLE_SIZE);
        if (sample.length < MIN_UTF16_BYTES) {
            return null;
        }
        @Nullable PipelineResult utf32 = checkUtf32(sample);
        return utf32 == null ? checkUtf16(sample) : utf32;
    }

    /// Checks UTF-32 positional patterns before UTF-16.
    ///
    /// @param data bounded sample
    /// @return result, or `null`
    private static @Nullable PipelineResult checkUtf32(byte @Unmodifiable [] data) {
        int length = data.length - data.length % 4;
        if (length < MIN_UTF32_BYTES) {
            return null;
        }
        byte[] sample = length == data.length ? data : Arrays.copyOf(data, length);
        int units = length / 4;
        int firstNulls = 0;
        int secondNulls = 0;
        for (int index = 0; index < length; index += 4) {
            firstNulls += sample[index] == 0 ? 1 : 0;
            secondNulls += sample[index + 1] == 0 ? 1 : 0;
        }
        if (firstNulls == units && (double) secondNulls / units > 0.5) {
            @Nullable String text = decodeStrict(sample, "utf-32-be");
            if (text != null && looksLikeText(text)) {
                return result("utf-32-be");
            }
        }

        int lastNulls = 0;
        int thirdNulls = 0;
        for (int index = 0; index < length; index += 4) {
            thirdNulls += sample[index + 2] == 0 ? 1 : 0;
            lastNulls += sample[index + 3] == 0 ? 1 : 0;
        }
        if (lastNulls == units && (double) thirdNulls / units > 0.5) {
            @Nullable String text = decodeStrict(sample, "utf-32-le");
            if (text != null && looksLikeText(text)) {
                return result("utf-32-le");
            }
        }
        return null;
    }

    /// Checks alternating UTF-16 null-byte patterns and text quality.
    ///
    /// @param data bounded sample
    /// @return result, or `null`
    private static @Nullable PipelineResult checkUtf16(byte @Unmodifiable [] data) {
        int length = Math.min(data.length, SAMPLE_SIZE);
        length -= length % 2;
        if (length < MIN_UTF16_BYTES) {
            return null;
        }
        byte[] sample = length == data.length ? data : Arrays.copyOf(data, length);
        int units = length / 2;
        int evenNulls = 0;
        int oddNulls = 0;
        for (int index = 0; index < length; index += 2) {
            evenNulls += sample[index] == 0 ? 1 : 0;
            oddNulls += sample[index + 1] == 0 ? 1 : 0;
        }
        double bigEndianFraction = (double) evenNulls / units;
        double littleEndianFraction = (double) oddNulls / units;

        ArrayList<EndianCandidate> candidates = new ArrayList<>(2);
        if (littleEndianFraction >= UTF16_MIN_NULL_FRACTION
                && !isNullSeparatorPattern(sample, littleEndianFraction)) {
            candidates.add(new EndianCandidate("utf-16-le", littleEndianFraction));
        }
        if (bigEndianFraction >= UTF16_MIN_NULL_FRACTION
                && !isNullSeparatorPattern(sample, bigEndianFraction)) {
            candidates.add(new EndianCandidate("utf-16-be", bigEndianFraction));
        }
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            String encoding = candidates.get(0).encoding();
            @Nullable String text = decodeStrict(sample, encoding);
            return text != null && looksLikeText(text) ? result(encoding) : null;
        }

        @Nullable String bestEncoding = null;
        double bestQuality = -1.0;
        for (EndianCandidate candidate : candidates) {
            @Nullable String text = decodeStrict(sample, candidate.encoding());
            if (text == null) {
                continue;
            }
            double quality = textQuality(text);
            if (quality > bestQuality) {
                bestQuality = quality;
                bestEncoding = candidate.encoding();
            }
        }
        return bestEncoding != null && bestQuality >= MIN_TEXT_QUALITY
                ? result(bestEncoding)
                : null;
    }

    /// Rejects sparse null-separated printable ASCII as false UTF-16.
    ///
    /// @param data               bounded sample
    /// @param positionalFraction candidate-position null fraction
    /// @return whether the sample is ASCII with sparse null separators
    private static boolean isNullSeparatorPattern(
            byte @Unmodifiable [] data,
            double positionalFraction
    ) {
        if (positionalFraction >= NULL_SEPARATOR_MAX_FRACTION) {
            return false;
        }
        for (byte value : data) {
            int unsigned = Byte.toUnsignedInt(value);
            if (unsigned != 0
                    && unsigned != '\t'
                    && unsigned != '\n'
                    && unsigned != '\r'
                    && (unsigned < 0x20 || unsigned > 0x7e)) {
                return false;
            }
        }
        return true;
    }

    /// Strictly decodes an endian-specific Unicode sample.
    ///
    /// @param data     sample bytes
    /// @param encoding canonical Unicode encoding
    /// @return decoded text, or `null` when malformed
    private static @Nullable String decodeStrict(
            byte @Unmodifiable [] data,
            String encoding
    ) {
        if (!ByteValidity.isValid(data, encoding)) {
            return null;
        }
        byte @Nullable [] utf8 = TextDecoder.toUtf8(data, encoding);
        return utf8 == null ? null : new String(utf8, StandardCharsets.UTF_8);
    }

    /// Reports whether decoded text is mostly printable.
    ///
    /// @param text decoded text
    /// @return whether more than 70 percent of the first 500 code points print
    private static boolean looksLikeText(String text) {
        int[] sample = text.codePoints().limit(500).toArray();
        if (sample.length == 0) {
            return false;
        }
        int printable = 0;
        for (int codePoint : sample) {
            if (isPythonPrintable(codePoint)
                    || codePoint == '\n'
                    || codePoint == '\r'
                    || codePoint == '\t') {
                printable++;
            }
        }
        return (double) printable / sample.length > MIN_PRINTABLE_FRACTION;
    }

    /// Scores decoded text for endianness disambiguation.
    ///
    /// @param text decoded text
    /// @return reference-compatible quality score
    private static double textQuality(String text) {
        int[] sample = text.codePoints().limit(500).toArray();
        if (sample.length == 0) {
            return -1.0;
        }
        int letters = 0;
        int marks = 0;
        int spaces = 0;
        int controls = 0;
        int asciiLetters = 0;
        for (int codePoint : sample) {
            int category = Character.getType(codePoint);
            if (isLetterCategory(category)) {
                letters++;
                if (codePoint < 128) {
                    asciiLetters++;
                }
            } else if (isMarkCategory(category)) {
                marks++;
            } else if (category == Character.SPACE_SEPARATOR
                    || codePoint == '\n'
                    || codePoint == '\r'
                    || codePoint == '\t') {
                spaces++;
            } else if (isOtherCategory(category)) {
                controls++;
            }
        }
        int length = sample.length;
        if ((double) controls / length > 0.1 || (double) marks / length > 0.2) {
            return -1.0;
        }
        double score = (double) letters / length + ((double) asciiLetters / length) * 0.5;
        if (length > 20 && spaces > 0) {
            score += 0.1;
        }
        return score;
    }

    /// Implements Python's single-character printability categories.
    ///
    /// @param codePoint Unicode code point
    /// @return whether Python `str.isprintable` accepts the character
    private static boolean isPythonPrintable(int codePoint) {
        if (codePoint == 0x20) {
            return true;
        }
        int category = Character.getType(codePoint);
        return category != Character.CONTROL
                && category != Character.FORMAT
                && category != Character.SURROGATE
                && category != Character.PRIVATE_USE
                && category != Character.UNASSIGNED
                && category != Character.SPACE_SEPARATOR
                && category != Character.LINE_SEPARATOR
                && category != Character.PARAGRAPH_SEPARATOR;
    }

    /// Tests Unicode letter categories.
    ///
    /// @param category `Character.getType` value
    /// @return whether it is an `L*` category
    private static boolean isLetterCategory(int category) {
        return category == Character.UPPERCASE_LETTER
                || category == Character.LOWERCASE_LETTER
                || category == Character.TITLECASE_LETTER
                || category == Character.MODIFIER_LETTER
                || category == Character.OTHER_LETTER;
    }

    /// Tests Unicode mark categories.
    ///
    /// @param category `Character.getType` value
    /// @return whether it is an `M*` category
    private static boolean isMarkCategory(int category) {
        return category == Character.NON_SPACING_MARK
                || category == Character.COMBINING_SPACING_MARK
                || category == Character.ENCLOSING_MARK;
    }

    /// Tests Unicode `C*` categories.
    ///
    /// @param category `Character.getType` value
    /// @return whether it is an other/control category
    private static boolean isOtherCategory(int category) {
        return category == Character.CONTROL
                || category == Character.FORMAT
                || category == Character.SURROGATE
                || category == Character.PRIVATE_USE
                || category == Character.UNASSIGNED;
    }

    /// Creates a deterministic Unicode-pattern result.
    ///
    /// @param encoding endian-specific canonical encoding
    /// @return result
    private static PipelineResult result(String encoding) {
        return new PipelineResult(encoding, CONFIDENCE, null, null);
    }

    /// Holds a UTF-16 endianness candidate and its positional evidence.
    ///
    /// @param encoding     endian-specific canonical name
    /// @param nullFraction expected-position null fraction
    @NotNullByDefault
    private record EndianCandidate(String encoding, double nullFraction) {
        /// Creates an endianness candidate.
        private EndianCandidate {
        }
    }
}
