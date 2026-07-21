// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.EncodingDetector.Encoding;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Applies byte-evidence ranking corrections after statistical scoring.
@NotNullByDefault
final class PostProcessor {
    /// Common Western Latin promotion targets.
    private static final @Unmodifiable Set<Encoding> COMMON_LATIN_ENCODINGS =
            Collections.unmodifiableSet(EnumSet.of(
                    Encoding.ISO_8859_1,
                    Encoding.ISO_8859_15,
                    Encoding.CP1252
            ));

    /// Distinguishing bytes for niche Latin candidates.
    private static final @Unmodifiable Map<Encoding, ByteSet> DEMOTION_CANDIDATES =
            createDemotionCandidates();

    /// Tajik-specific KOI8-T bytes.
    private static final ByteSet KOI8_T_DISTINGUISHING = ByteSet.of(
            0x80, 0x81, 0x83, 0x8a, 0x8c, 0x8d, 0x8e, 0x90, 0xa1, 0xa2, 0xa5, 0xb5
    );

    /// Prevents instantiation of this static stage.
    private PostProcessor() {
    }

    /// Creates the immutable byte-evidence table for niche Latin encodings.
    ///
    /// @return the byte-evidence table
    private static @Unmodifiable Map<Encoding, ByteSet> createDemotionCandidates() {
        EnumMap<Encoding, ByteSet> candidates = new EnumMap<>(Encoding.class);
        candidates.put(Encoding.ISO_8859_10, ByteSet.of(
                0xa1, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa8, 0xa9, 0xaa, 0xab, 0xac,
                0xae, 0xaf, 0xb1, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb8, 0xb9, 0xba,
                0xbb, 0xbc, 0xbd, 0xbe, 0xbf, 0xc0, 0xc7, 0xc8, 0xca, 0xcc, 0xd1,
                0xd2, 0xd7, 0xd9, 0xe0, 0xe7, 0xe8, 0xea, 0xec, 0xf1, 0xf2, 0xf7,
                0xf9, 0xff
        ));
        candidates.put(Encoding.ISO_8859_14, ByteSet.of(
                0xa1, 0xa2, 0xa4, 0xa5, 0xa6, 0xa8, 0xaa, 0xab, 0xac, 0xaf, 0xb0,
                0xb1, 0xb2, 0xb3, 0xb4, 0xb5, 0xb7, 0xb8, 0xb9, 0xba, 0xbb, 0xbc,
                0xbd, 0xbe, 0xbf, 0xd0, 0xd7, 0xde, 0xf0, 0xf7, 0xfe
        ));
        candidates.put(Encoding.CP1254, ByteSet.of(0xd0, 0xdd, 0xde, 0xf0, 0xfd, 0xfe));
        candidates.put(Encoding.HP_ROMAN8, ByteSet.of(
                0xc0, 0xc1, 0xc2, 0xc3, 0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca,
                0xcb, 0xcc, 0xcd, 0xce, 0xcf, 0xd1, 0xd4, 0xd5, 0xd6, 0xd9, 0xdd,
                0xde
        ));
        return Collections.unmodifiableMap(candidates);
    }

    /// Applies confusion resolution, niche-Latin demotion, and KOI8-T promotion.
    ///
    /// @param data    analyzed bytes
    /// @param results statistically ranked candidates
    /// @return corrected ranking
    static List<PipelineResult> process(
            @UnmodifiableView ByteBuffer data,
            List<PipelineResult> results
    ) {
        List<PipelineResult> resolved = ConfusionResolver.resolve(data, results);
        resolved = demoteNicheLatin(data, resolved);
        return promoteKoi8T(data, resolved);
    }

    /// Demotes a niche-Latin winner that lacks distinguishing-byte evidence.
    ///
    /// @param data    analyzed bytes
    /// @param results ranked candidates
    /// @return corrected ranking
    private static List<PipelineResult> demoteNicheLatin(
            @UnmodifiableView ByteBuffer data,
            List<PipelineResult> results
    ) {
        if (results.size() <= 1) {
            return results;
        }
        @Nullable Encoding demotedEncoding = results.get(0).encoding();
        if (demotedEncoding == null) {
            return results;
        }
        if (!shouldDemote(demotedEncoding, data)) {
            return results;
        }
        int promotedIndex = -1;
        for (int index = 1; index < results.size(); index++) {
            @Nullable Encoding encoding = results.get(index).encoding();
            if (encoding != null && COMMON_LATIN_ENCODINGS.contains(encoding)) {
                promotedIndex = index;
                break;
            }
        }
        if (promotedIndex < 0) {
            return results;
        }

        PipelineResult selected = results.get(promotedIndex);
        PipelineResult promoted = new PipelineResult(
                selected.encoding(),
                results.get(0).confidence(),
                selected.language(),
                selected.mimeType()
        );
        ArrayList<PipelineResult> reordered = new ArrayList<>(results.size());
        reordered.add(promoted);
        for (int index = 0; index < results.size(); index++) {
            PipelineResult result = results.get(index);
            if (index != promotedIndex && result.encoding() != demotedEncoding) {
                reordered.add(result);
            }
        }
        for (PipelineResult result : results) {
            if (result.encoding() == demotedEncoding) {
                reordered.add(result);
            }
        }
        return reordered;
    }

    /// Reports whether a candidate lacks all of its distinguishing bytes.
    ///
    /// @param encoding candidate encoding
    /// @param data     analyzed bytes
    /// @return whether the candidate should be demoted
    private static boolean shouldDemote(
            Encoding encoding,
            @UnmodifiableView ByteBuffer data
    ) {
        @Nullable ByteSet distinguishing = DEMOTION_CANDIDATES.get(encoding);
        if (distinguishing == null) {
            return false;
        }
        for (int index = 0; index < data.remaining(); index++) {
            byte value = data.get(index);
            if (value < 0 && distinguishing.contains(value)) {
                return false;
            }
        }
        return true;
    }

    /// Promotes KOI8-T when Tajik-specific bytes distinguish it from KOI8-R.
    ///
    /// @param data    analyzed bytes
    /// @param results ranked candidates
    /// @return corrected ranking
    private static List<PipelineResult> promoteKoi8T(
            @UnmodifiableView ByteBuffer data,
            List<PipelineResult> results
    ) {
        if (results.isEmpty() || results.get(0).encoding() != Encoding.KOI8_R) {
            return results;
        }
        int koi8TIndex = -1;
        for (int index = 0; index < results.size(); index++) {
            if (results.get(index).encoding() == Encoding.KOI8_T) {
                koi8TIndex = index;
                break;
            }
        }
        if (koi8TIndex < 0 || !containsAny(data, KOI8_T_DISTINGUISHING)) {
            return results;
        }
        PipelineResult selected = results.get(koi8TIndex);
        PipelineResult promoted = new PipelineResult(
                selected.encoding(),
                results.get(0).confidence(),
                selected.language(),
                selected.mimeType()
        );
        ArrayList<PipelineResult> reordered = new ArrayList<>(results.size());
        reordered.add(promoted);
        for (int index = 0; index < results.size(); index++) {
            if (index != koi8TIndex) {
                reordered.add(results.get(index));
            }
        }
        return reordered;
    }

    /// Tests whether input contains any high byte in a set.
    ///
    /// @param data   bytes to inspect
    /// @param values unsigned byte set
    /// @return whether a matching high byte exists
    private static boolean containsAny(
            @UnmodifiableView ByteBuffer data,
            ByteSet values
    ) {
        for (int index = 0; index < data.remaining(); index++) {
            byte value = data.get(index);
            if (value < 0 && values.contains(value)) {
                return true;
            }
        }
        return false;
    }
}
