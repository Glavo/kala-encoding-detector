// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.ByteBuffer;
import java.util.Map;

/// Computes cached CJK multibyte structural metrics in one pass per encoding.
@NotNullByDefault
final class StructuralAnalysis {
    /// Prevents instantiation of this static stage.
    private StructuralAnalysis() {
    }

    /// Returns cached or newly computed metrics for a canonical encoding.
    ///
    /// @param data     bytes to analyze
    /// @param encoding canonical encoding name
    /// @param cache    per-detection cache
    /// @return metrics, or `null` when no analyzer exists
    static @Nullable Analysis get(
            @UnmodifiableView ByteBuffer data,
            String encoding,
            Map<String, Analysis> cache
    ) {
        @Nullable Analysis cached = cache.get(encoding);
        if (cached != null) {
            return cached;
        }
        @Nullable Analysis result = switch (encoding) {
            case "shift_jis_2004" -> analyzeShiftJis(data, 0xef);
            case "cp932" -> analyzeShiftJis(data, 0xfc);
            case "euc_jis_2004" -> analyzeEucJp(data);
            case "euc_kr" -> analyzeEucKr(data);
            case "cp949" -> analyzeCp949(data);
            case "gb18030" -> analyzeGb18030(data);
            case "big5hkscs" -> analyzeBig5Hkscs(data);
            case "johab" -> analyzeJohab(data);
            default -> null;
        };
        if (result != null) {
            cache.put(encoding, result);
        }
        return result;
    }

    /// Analyzes Shift_JIS or CP932 byte structure.
    ///
    /// @param data      bytes to analyze
    /// @param upperLead inclusive upper lead byte for the second lead range
    /// @return structural metrics
    private static Analysis analyzeShiftJis(
            @UnmodifiableView ByteBuffer data,
            int upperLead
    ) {
        int leadCount = 0;
        int validCount = 0;
        int multibyteBytes = 0;
        boolean[] leads = new boolean[256];
        for (int index = 0; index < data.remaining(); ) {
            int value = Byte.toUnsignedInt(data.get(index));
            if (between(value, 0x81, 0x9f) || between(value, 0xe0, upperLead)) {
                leadCount++;
                if (index + 1 < data.remaining()) {
                    int trail = Byte.toUnsignedInt(data.get(index + 1));
                    if (between(trail, 0x40, 0x7e) || between(trail, 0x80, 0xfc)) {
                        validCount++;
                        leads[value] = true;
                        multibyteBytes += trail > 0x7f ? 2 : 1;
                        index += 2;
                        continue;
                    }
                }
            }
            index++;
        }
        return result(leadCount, validCount, multibyteBytes, leads);
    }

    /// Analyzes EUC-JIS-2004 byte structure.
    ///
    /// @param data bytes to analyze
    /// @return structural metrics
    private static Analysis analyzeEucJp(@UnmodifiableView ByteBuffer data) {
        int leadCount = 0;
        int validCount = 0;
        int multibyteBytes = 0;
        boolean[] leads = new boolean[256];
        for (int index = 0; index < data.remaining(); ) {
            int value = Byte.toUnsignedInt(data.get(index));
            if (value == 0x8e) {
                leadCount++;
                if (index + 1 < data.remaining()
                        && between(Byte.toUnsignedInt(data.get(index + 1)), 0xa1, 0xdf)) {
                    validCount++;
                    leads[value] = true;
                    multibyteBytes += 2;
                    index += 2;
                    continue;
                }
            } else if (value == 0x8f) {
                leadCount++;
                if (index + 2 < data.remaining()
                        && between(Byte.toUnsignedInt(data.get(index + 1)), 0xa1, 0xfe)
                        && between(Byte.toUnsignedInt(data.get(index + 2)), 0xa1, 0xfe)) {
                    validCount++;
                    leads[value] = true;
                    multibyteBytes += 3;
                    index += 3;
                    continue;
                }
            } else if (between(value, 0xa1, 0xfe)) {
                leadCount++;
                if (index + 1 < data.remaining()
                        && between(Byte.toUnsignedInt(data.get(index + 1)), 0xa1, 0xfe)) {
                    validCount++;
                    leads[value] = true;
                    multibyteBytes += 2;
                    index += 2;
                    continue;
                }
            }
            index++;
        }
        return result(leadCount, validCount, multibyteBytes, leads);
    }

    /// Analyzes EUC-KR byte structure.
    ///
    /// @param data bytes to analyze
    /// @return structural metrics
    private static Analysis analyzeEucKr(@UnmodifiableView ByteBuffer data) {
        int leadCount = 0;
        int validCount = 0;
        int multibyteBytes = 0;
        boolean[] leads = new boolean[256];
        for (int index = 0; index < data.remaining(); ) {
            int value = Byte.toUnsignedInt(data.get(index));
            if (between(value, 0xa1, 0xfe)) {
                leadCount++;
                if (index + 1 < data.remaining()
                        && between(Byte.toUnsignedInt(data.get(index + 1)), 0xa1, 0xfe)) {
                    validCount++;
                    leads[value] = true;
                    multibyteBytes += 2;
                    index += 2;
                    continue;
                }
            }
            index++;
        }
        return result(leadCount, validCount, multibyteBytes, leads);
    }

    /// Analyzes CP949 Unified Hangul Code byte structure.
    ///
    /// @param data bytes to analyze
    /// @return structural metrics
    private static Analysis analyzeCp949(@UnmodifiableView ByteBuffer data) {
        int leadCount = 0;
        int validCount = 0;
        int multibyteBytes = 0;
        boolean[] leads = new boolean[256];
        for (int index = 0; index < data.remaining(); ) {
            int value = Byte.toUnsignedInt(data.get(index));
            if (between(value, 0x81, 0xc8) || between(value, 0xca, 0xfd)) {
                leadCount++;
                if (index + 1 < data.remaining()) {
                    int trail = Byte.toUnsignedInt(data.get(index + 1));
                    if (between(trail, 0x41, 0x5a)
                            || between(trail, 0x61, 0x7a)
                            || between(trail, 0x81, 0xfe)) {
                        validCount++;
                        leads[value] = true;
                        multibyteBytes += trail > 0x7f ? 2 : 1;
                        index += 2;
                        continue;
                    }
                }
            }
            index++;
        }
        return result(leadCount, validCount, multibyteBytes, leads);
    }

    /// Analyzes strict GB2312 pairs and GB18030 four-byte structure.
    ///
    /// @param data bytes to analyze
    /// @return structural metrics
    private static Analysis analyzeGb18030(@UnmodifiableView ByteBuffer data) {
        int leadCount = 0;
        int validCount = 0;
        int multibyteBytes = 0;
        boolean[] leads = new boolean[256];
        for (int index = 0; index < data.remaining(); ) {
            int value = Byte.toUnsignedInt(data.get(index));
            if (between(value, 0x81, 0xfe)) {
                leadCount++;
                if (index + 3 < data.remaining()
                        && between(Byte.toUnsignedInt(data.get(index + 1)), 0x30, 0x39)
                        && between(Byte.toUnsignedInt(data.get(index + 2)), 0x81, 0xfe)
                        && between(Byte.toUnsignedInt(data.get(index + 3)), 0x30, 0x39)) {
                    validCount++;
                    leads[value] = true;
                    multibyteBytes += 2;
                    index += 4;
                    continue;
                }
                if (between(value, 0xa1, 0xf7)
                        && index + 1 < data.remaining()
                        && between(Byte.toUnsignedInt(data.get(index + 1)), 0xa1, 0xfe)) {
                    validCount++;
                    leads[value] = true;
                    multibyteBytes += 2;
                    index += 2;
                    continue;
                }
            }
            index++;
        }
        return result(leadCount, validCount, multibyteBytes, leads);
    }

    /// Analyzes Big5-HKSCS byte structure.
    ///
    /// @param data bytes to analyze
    /// @return structural metrics
    private static Analysis analyzeBig5Hkscs(@UnmodifiableView ByteBuffer data) {
        int leadCount = 0;
        int validCount = 0;
        int multibyteBytes = 0;
        boolean[] leads = new boolean[256];
        for (int index = 0; index < data.remaining(); ) {
            int value = Byte.toUnsignedInt(data.get(index));
            if (between(value, 0x87, 0xfe)) {
                leadCount++;
                if (index + 1 < data.remaining()) {
                    int trail = Byte.toUnsignedInt(data.get(index + 1));
                    if (between(trail, 0x40, 0x7e) || between(trail, 0xa1, 0xfe)) {
                        validCount++;
                        leads[value] = true;
                        multibyteBytes += trail > 0x7f ? 2 : 1;
                        index += 2;
                        continue;
                    }
                }
            }
            index++;
        }
        return result(leadCount, validCount, multibyteBytes, leads);
    }

    /// Analyzes Johab byte structure.
    ///
    /// @param data bytes to analyze
    /// @return structural metrics
    private static Analysis analyzeJohab(@UnmodifiableView ByteBuffer data) {
        int leadCount = 0;
        int validCount = 0;
        int multibyteBytes = 0;
        boolean[] leads = new boolean[256];
        for (int index = 0; index < data.remaining(); ) {
            int value = Byte.toUnsignedInt(data.get(index));
            if (between(value, 0x84, 0xd3)
                    || between(value, 0xd8, 0xde)
                    || between(value, 0xe0, 0xf9)) {
                leadCount++;
                if (index + 1 < data.remaining()) {
                    int trail = Byte.toUnsignedInt(data.get(index + 1));
                    if (between(trail, 0x31, 0x7e) || between(trail, 0x91, 0xfe)) {
                        validCount++;
                        leads[value] = true;
                        multibyteBytes += value > 0x7f ? 1 : 0;
                        multibyteBytes += trail > 0x7f ? 1 : 0;
                        index += 2;
                        continue;
                    }
                }
            }
            index++;
        }
        return result(leadCount, validCount, multibyteBytes, leads);
    }

    /// Constructs metrics from analyzer counters.
    ///
    /// @param leadCount      lead-byte count
    /// @param validCount     valid-sequence count
    /// @param multibyteBytes non-ASCII bytes participating in valid sequences
    /// @param leads          distinct-lead bitmap
    /// @return structural metrics
    private static Analysis result(
            int leadCount,
            int validCount,
            int multibyteBytes,
            boolean @Unmodifiable [] leads
    ) {
        int diversity = 0;
        for (boolean present : leads) {
            if (present) {
                diversity++;
            }
        }
        double ratio = leadCount == 0 ? 0.0 : (double) validCount / leadCount;
        return new Analysis(ratio, multibyteBytes, diversity);
    }

    /// Tests an inclusive byte-value range.
    ///
    /// @param value   value to test
    /// @param minimum inclusive minimum
    /// @param maximum inclusive maximum
    /// @return whether the value lies in the range
    private static boolean between(int value, int minimum, int maximum) {
        return value >= minimum && value <= maximum;
    }

    /// Holds one analyzer's three structural metrics.
    ///
    /// @param pairRatio      valid sequences divided by observed lead bytes
    /// @param multibyteBytes non-ASCII bytes participating in valid sequences
    /// @param leadDiversity  number of distinct valid lead-byte values
    @NotNullByDefault
    record Analysis(double pairRatio, int multibyteBytes, int leadDiversity) {
        /// Creates structural metrics.
        Analysis {
        }
    }
}
