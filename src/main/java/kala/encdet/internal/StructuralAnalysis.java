// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.EncodingDetector.Encoding;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.ByteBuffer;
import java.util.Map;

/// Computes CJK multibyte structural metrics.
@NotNullByDefault
final class StructuralAnalysis {
    /// Prevents instantiation of this static stage.
    private StructuralAnalysis() {
    }

    /// Returns cached or newly computed metrics for a canonical encoding.
    ///
    /// @param data     bytes to analyze
    /// @param encoding encoding to analyze
    /// @param cache    per-detection cache
    /// @return metrics, or `null` when no analyzer exists
    static @Nullable Analysis get(
            @UnmodifiableView ByteBuffer data,
            Encoding encoding,
            Map<Encoding, Analysis> cache
    ) {
        @Nullable Analysis cached = cache.get(encoding);
        if (cached != null) {
            return cached;
        }
        @Nullable Analysis result = switch (encoding) {
            case SHIFT_JIS_2004 -> analyzeShiftJis(data, 0xef);
            case CP932 -> analyzeShiftJis(data, 0xfc);
            case EUC_JIS_2004 -> analyzeEucJp(data);
            case EUC_KR -> analyzeEucKr(data);
            case CP949 -> analyzeCp949(data);
            case GB18030 -> analyzeGb18030(data);
            case BIG5_HKSCS -> analyzeBig5Hkscs(data);
            case JOHAB -> analyzeJohab(data);
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
            if ((value >= 0x81 && value <= 0x9f)
                    || (value >= 0xe0 && value <= upperLead)) {
                leadCount++;
                if (index + 1 < data.remaining()) {
                    int trail = Byte.toUnsignedInt(data.get(index + 1));
                    if ((trail >= 0x40 && trail <= 0x7e)
                            || (trail >= 0x80 && trail <= 0xfc)) {
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
        return createAnalysis(leadCount, validCount, multibyteBytes, leads);
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
                        && Byte.toUnsignedInt(data.get(index + 1)) >= 0xa1
                        && Byte.toUnsignedInt(data.get(index + 1)) <= 0xdf) {
                    validCount++;
                    leads[value] = true;
                    multibyteBytes += 2;
                    index += 2;
                    continue;
                }
            } else if (value == 0x8f) {
                leadCount++;
                if (index + 2 < data.remaining()
                        && Byte.toUnsignedInt(data.get(index + 1)) >= 0xa1
                        && Byte.toUnsignedInt(data.get(index + 1)) <= 0xfe
                        && Byte.toUnsignedInt(data.get(index + 2)) >= 0xa1
                        && Byte.toUnsignedInt(data.get(index + 2)) <= 0xfe) {
                    validCount++;
                    leads[value] = true;
                    multibyteBytes += 3;
                    index += 3;
                    continue;
                }
            } else if (value >= 0xa1 && value <= 0xfe) {
                leadCount++;
                if (index + 1 < data.remaining()
                        && Byte.toUnsignedInt(data.get(index + 1)) >= 0xa1
                        && Byte.toUnsignedInt(data.get(index + 1)) <= 0xfe) {
                    validCount++;
                    leads[value] = true;
                    multibyteBytes += 2;
                    index += 2;
                    continue;
                }
            }
            index++;
        }
        return createAnalysis(leadCount, validCount, multibyteBytes, leads);
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
            if (value >= 0xa1 && value <= 0xfe) {
                leadCount++;
                if (index + 1 < data.remaining()
                        && Byte.toUnsignedInt(data.get(index + 1)) >= 0xa1
                        && Byte.toUnsignedInt(data.get(index + 1)) <= 0xfe) {
                    validCount++;
                    leads[value] = true;
                    multibyteBytes += 2;
                    index += 2;
                    continue;
                }
            }
            index++;
        }
        return createAnalysis(leadCount, validCount, multibyteBytes, leads);
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
            if ((value >= 0x81 && value <= 0xc8)
                    || (value >= 0xca && value <= 0xfd)) {
                leadCount++;
                if (index + 1 < data.remaining()) {
                    int trail = Byte.toUnsignedInt(data.get(index + 1));
                    if ((trail >= 0x41 && trail <= 0x5a)
                            || (trail >= 0x61 && trail <= 0x7a)
                            || (trail >= 0x81 && trail <= 0xfe)) {
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
        return createAnalysis(leadCount, validCount, multibyteBytes, leads);
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
            if (value >= 0x81 && value <= 0xfe) {
                leadCount++;
                if (index + 3 < data.remaining()
                        && Byte.toUnsignedInt(data.get(index + 1)) >= 0x30
                        && Byte.toUnsignedInt(data.get(index + 1)) <= 0x39
                        && Byte.toUnsignedInt(data.get(index + 2)) >= 0x81
                        && Byte.toUnsignedInt(data.get(index + 2)) <= 0xfe
                        && Byte.toUnsignedInt(data.get(index + 3)) >= 0x30
                        && Byte.toUnsignedInt(data.get(index + 3)) <= 0x39) {
                    validCount++;
                    leads[value] = true;
                    multibyteBytes += 2;
                    index += 4;
                    continue;
                }
                if (value >= 0xa1 && value <= 0xf7
                        && index + 1 < data.remaining()
                        && Byte.toUnsignedInt(data.get(index + 1)) >= 0xa1
                        && Byte.toUnsignedInt(data.get(index + 1)) <= 0xfe) {
                    validCount++;
                    leads[value] = true;
                    multibyteBytes += 2;
                    index += 2;
                    continue;
                }
            }
            index++;
        }
        return createAnalysis(leadCount, validCount, multibyteBytes, leads);
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
            if (value >= 0x87 && value <= 0xfe) {
                leadCount++;
                if (index + 1 < data.remaining()) {
                    int trail = Byte.toUnsignedInt(data.get(index + 1));
                    if ((trail >= 0x40 && trail <= 0x7e)
                            || (trail >= 0xa1 && trail <= 0xfe)) {
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
        return createAnalysis(leadCount, validCount, multibyteBytes, leads);
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
            if ((value >= 0x84 && value <= 0xd3)
                    || (value >= 0xd8 && value <= 0xde)
                    || (value >= 0xe0 && value <= 0xf9)) {
                leadCount++;
                if (index + 1 < data.remaining()) {
                    int trail = Byte.toUnsignedInt(data.get(index + 1));
                    if ((trail >= 0x31 && trail <= 0x7e)
                            || (trail >= 0x91 && trail <= 0xfe)) {
                        validCount++;
                        leads[value] = true;
                        multibyteBytes++;
                        multibyteBytes += trail > 0x7f ? 1 : 0;
                        index += 2;
                        continue;
                    }
                }
            }
            index++;
        }
        return createAnalysis(leadCount, validCount, multibyteBytes, leads);
    }

    /// Constructs metrics from analyzer counters.
    ///
    /// @param leadCount      lead-byte count
    /// @param validCount     valid-sequence count
    /// @param multibyteBytes non-ASCII bytes participating in valid sequences
    /// @param leads          distinct-lead bitmap
    /// @return structural metrics
    private static Analysis createAnalysis(
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
