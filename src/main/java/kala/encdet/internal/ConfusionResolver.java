// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.EncodingDetector.Encoding;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Resolves statistically confusable single-byte encoding candidates.
@NotNullByDefault
final class ConfusionResolver {
    /// Logger used for missing or empty resource warnings.
    private static final System.Logger LOGGER = System.getLogger(ConfusionResolver.class.getName());

    /// Bundled confusion-data resource.
    private static final String RESOURCE = "/kala/encdet/internal/confusion.bin";

    /// Maximum confidence gap for candidates after position one.
    private static final double CONFUSION_BAND = 0.005;

    /// Preference score indexed by serialized Unicode general-category code.
    private static final int @Unmodifiable [] CATEGORY_PREFERENCES = {
            10, 10, 10, 9, 9, 5, 5, 5, 8, 7,
            7, 6, 6, 6, 6, 6, 6, 6, 5, 5,
            4, 4, 3, 3, 3, 1, 2, 0, 1, 0
    };

    /// Prevents instantiation of this static resolver.
    private ConfusionResolver() {
    }

    /// Resolves confusion between a top result and nearby candidates.
    ///
    /// @param data    normalized read-only analyzed bytes
    /// @param results candidates sorted by descending confidence
    /// @return reordered candidates, or the original list when unresolved
    static List<PipelineResult> resolve(
            @UnmodifiableView ByteBuffer data,
            List<PipelineResult> results
    ) {
        if (results.size() < 2) {
            return results;
        }
        PipelineResult top = results.get(0);
        @Nullable Encoding topEncoding = top.encoding();
        if (topEncoding == null) {
            return results;
        }
        double topConfidence = top.confidence();
        Map<PairKey, PairData> maps = Holder.DATA;
        for (int index = 1; index < results.size(); index++) {
            PipelineResult candidate = results.get(index);
            @Nullable Encoding candidateEncoding = candidate.encoding();
            if (candidateEncoding == null) {
                continue;
            }
            if (index > 1 && topConfidence - candidate.confidence() > CONFUSION_BAND) {
                break;
            }
            @Nullable PairMatch match = findPair(maps, topEncoding, candidateEncoding);
            if (match == null) {
                continue;
            }
            @Nullable Encoding categoryWinner = categoryWinner(data, match.key(), match.data());
            @Nullable Encoding bigramWinner = bigramWinner(data, match.key(), match.data());
            @Nullable Encoding winner = bigramWinner == null ? categoryWinner : bigramWinner;
            if (winner == candidateEncoding) {
                PipelineResult promoted = new PipelineResult(
                        candidateEncoding,
                        top.confidence(),
                        candidate.language(),
                        candidate.mimeType()
                );
                ArrayList<PipelineResult> reordered = new ArrayList<>(results.size());
                reordered.add(promoted);
                for (int resultIndex = 0; resultIndex < results.size(); resultIndex++) {
                    if (resultIndex != index) {
                        reordered.add(results.get(resultIndex));
                    }
                }
                return reordered;
            }
        }
        return results;
    }

    /// Finds confusion data for an encoding pair in either orientation.
    ///
    /// @param maps   parsed confusion maps
    /// @param first  first encoding
    /// @param second second encoding
    /// @return stored pair and data, or `null`
    private static @Nullable PairMatch findPair(
            Map<PairKey, PairData> maps,
            Encoding first,
            Encoding second
    ) {
        PairKey direct = new PairKey(first, second);
        @Nullable PairData directData = maps.get(direct);
        if (directData != null) {
            return new PairMatch(direct, directData);
        }
        PairKey reverse = new PairKey(second, first);
        @Nullable PairData reverseData = maps.get(reverse);
        return reverseData == null ? null : new PairMatch(reverse, reverseData);
    }

    /// Resolves a pair using Unicode-category preference voting.
    ///
    /// @param data source bytes
    /// @param key  stored pair orientation
    /// @param pair distinguishing maps
    /// @return winning encoding, or `null` on a tie or no evidence
    private static @Nullable Encoding categoryWinner(
            @UnmodifiableView ByteBuffer data,
            PairKey key,
            PairData pair
    ) {
        boolean[] observed = new boolean[256];
        for (int index = 0; index < data.limit(); index++) {
            observed[Byte.toUnsignedInt(data.get(index))] = true;
        }
        int firstVotes = 0;
        int secondVotes = 0;
        for (int value = 0; value < observed.length; value++) {
            if (!observed[value] || !pair.distinguishes(value)) {
                continue;
            }
            int firstPreference = pair.firstPreference(value);
            int secondPreference = pair.secondPreference(value);
            if (firstPreference > secondPreference) {
                firstVotes += firstPreference - secondPreference;
            } else if (secondPreference > firstPreference) {
                secondVotes += secondPreference - firstPreference;
            }
        }
        if (firstVotes > secondVotes) {
            return key.first();
        }
        if (secondVotes > firstVotes) {
            return key.second();
        }
        return null;
    }

    /// Resolves a pair by rescoring only bigrams touching distinguishing bytes.
    ///
    /// @param data source bytes
    /// @param key  stored pair orientation
    /// @param pair distinguishing maps
    /// @return winning encoding, or `null` on a tie or no evidence
    private static @Nullable Encoding bigramWinner(
            @UnmodifiableView ByteBuffer data,
            PairKey key,
            PairData pair
    ) {
        if (data.limit() < 2) {
            return null;
        }
        int[] frequencies = new int[65_536];
        boolean found = false;
        for (int index = 0; index < data.limit() - 1; index++) {
            int first = Byte.toUnsignedInt(data.get(index));
            int second = Byte.toUnsignedInt(data.get(index + 1));
            if (!pair.distinguishes(first) && !pair.distinguishes(second)) {
                continue;
            }
            int bigram = (first << 8) | second;
            frequencies[bigram] += ModelStore.idfWeight(bigram);
            found = true;
        }
        if (!found) {
            return null;
        }
        ModelStore.Profile profile = ModelStore.profileFromWeightedFrequencies(frequencies);
        double firstScore = ModelStore.scoreBestLanguage(key.first(), profile).score();
        double secondScore = ModelStore.scoreBestLanguage(key.second(), profile).score();
        if (firstScore > secondScore) {
            return key.first();
        }
        if (secondScore > firstScore) {
            return key.second();
        }
        return null;
    }

    /// Loads and parses bundled confusion data.
    ///
    /// @return immutable normalized pair map
    private static @Unmodifiable Map<PairKey, PairData> load() {
        @Nullable InputStream input = ConfusionResolver.class.getResourceAsStream(RESOURCE);
        if (input == null) {
            LOGGER.log(
                    System.Logger.Level.WARNING,
                    "Bundled confusion.bin is missing; confusion resolution is disabled"
            );
            return Map.of();
        }
        byte[] raw;
        try (input) {
            raw = input.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read bundled confusion.bin", exception);
        }
        if (raw.length == 0) {
            LOGGER.log(
                    System.Logger.Level.WARNING,
                    "Bundled confusion.bin is empty; confusion resolution is disabled"
            );
            return Map.of();
        }
        try {
            return parse(raw);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Corrupt confusion.bin: " + exception.getMessage(), exception);
        }
    }

    /// Parses the compact confusion-map format.
    ///
    /// @param raw complete resource contents
    /// @return immutable normalized map
    private static @Unmodifiable Map<PairKey, PairData> parse(byte @Unmodifiable [] raw) {
        ByteBuffer buffer = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);
        requireRemaining(buffer, Short.BYTES, "missing pair count");
        int count = Short.toUnsignedInt(buffer.getShort());
        LinkedHashMap<PairKey, PairData> result = new LinkedHashMap<>(count);
        for (int pairIndex = 0; pairIndex < count; pairIndex++) {
            Encoding first = readEncoding(buffer);
            Encoding second = readEncoding(buffer);
            requireRemaining(buffer, 1, "missing difference count");
            int differences = Byte.toUnsignedInt(buffer.get());
            byte[] masks = new byte[32];
            byte[] firstPreferences = new byte[256];
            byte[] secondPreferences = new byte[256];
            for (int difference = 0; difference < differences; difference++) {
                requireRemaining(buffer, 3, "truncated difference entry");
                int value = Byte.toUnsignedInt(buffer.get());
                int firstCategory = Byte.toUnsignedInt(buffer.get());
                int secondCategory = Byte.toUnsignedInt(buffer.get());
                masks[value >>> 3] |= (byte) (1 << (value & 7));
                firstPreferences[value] = (byte) categoryPreference(firstCategory);
                secondPreferences[value] = (byte) categoryPreference(secondCategory);
            }
            PairKey key = new PairKey(first, second);
            if (result.putIfAbsent(
                    key,
                    new PairData(masks, firstPreferences, secondPreferences)
            ) != null) {
                throw new IllegalArgumentException("duplicate confusion pair " + key);
            }
        }
        if (buffer.hasRemaining()) {
            throw new IllegalArgumentException("unexpected trailing bytes");
        }
        return Collections.unmodifiableMap(result);
    }

    /// Reads one length-prefixed UTF-8 encoding name.
    ///
    /// @param buffer source buffer
    /// @return decoded name
    private static String readName(ByteBuffer buffer) {
        requireRemaining(buffer, 1, "missing name length");
        int length = Byte.toUnsignedInt(buffer.get());
        requireRemaining(buffer, length, "truncated name");
        byte[] encoded = new byte[length];
        buffer.get(encoded);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("encoding name is not valid UTF-8", exception);
        }
    }

    /// Reads and resolves one length-prefixed encoding name.
    ///
    /// @param buffer source buffer
    /// @return resolved encoding identity
    /// @throws IllegalArgumentException if the encoded name is malformed or unknown
    private static Encoding readEncoding(ByteBuffer buffer) {
        String name = readName(buffer);
        @Nullable Encoding encoding = EncodingRegistry.lookup(name);
        if (encoding == null) {
            throw new IllegalArgumentException("unknown encoding " + name + " in confusion data");
        }
        return encoding;
    }

    /// Maps a serialized Unicode category code to its vote preference.
    ///
    /// @param category serialized category code
    /// @return category preference, defaulting to zero
    private static int categoryPreference(int category) {
        return category < CATEGORY_PREFERENCES.length ? CATEGORY_PREFERENCES[category] : 0;
    }

    /// Checks remaining bytes while parsing confusion data.
    ///
    /// @param buffer   source buffer
    /// @param required minimum remaining bytes
    /// @param detail   corruption detail
    private static void requireRemaining(ByteBuffer buffer, int required, String detail) {
        if (required < 0 || buffer.remaining() < required) {
            throw new IllegalArgumentException(detail);
        }
    }

    /// Initialization-on-demand holder for confusion data.
    @NotNullByDefault
    private static final class Holder {
        /// Lazily loaded immutable pair map.
        private static final @Unmodifiable Map<PairKey, PairData> DATA = load();

        /// Prevents holder instantiation.
        private Holder() {
        }
    }

    /// Identifies one stored confusion pair and its orientation.
    ///
    /// @param first  first canonical encoding
    /// @param second second canonical encoding
    @NotNullByDefault
    private record PairKey(Encoding first, Encoding second) {
        /// Creates a pair key.
        private PairKey {
        }
    }

    /// Associates a stored pair key with its maps.
    ///
    /// @param key  stored pair key
    /// @param data stored pair data
    @NotNullByDefault
    private record PairMatch(PairKey key, PairData data) {
        /// Creates a pair match.
        private PairMatch {
        }
    }

    /// Stores packed distinguishing bytes and category preferences.
    ///
    /// @param distinguishing    distinguishing-byte mask
    /// @param firstPreferences  first-encoding preference table
    /// @param secondPreferences second-encoding preference table
    @NotNullByDefault
    private record PairData(
            byte @Unmodifiable [] distinguishing,
            byte @Unmodifiable [] firstPreferences,
            byte @Unmodifiable [] secondPreferences
    ) {
        /// Creates immutable pair data owned by the resource parser.
        private PairData {
        }

        /// Tests whether a byte distinguishes the pair.
        ///
        /// @param value unsigned byte
        /// @return whether the byte differs between encodings
        private boolean distinguishes(int value) {
            return (Byte.toUnsignedInt(distinguishing[value >>> 3]) & (1 << (value & 7))) != 0;
        }

        /// Returns the first encoding's category preference for a byte.
        ///
        /// @param value unsigned byte
        /// @return preference score
        private int firstPreference(int value) {
            return Byte.toUnsignedInt(firstPreferences[value]);
        }

        /// Returns the second encoding's category preference for a byte.
        ///
        /// @param value unsigned byte
        /// @return preference score
        private int secondPreference(int value) {
            return Byte.toUnsignedInt(secondPreferences[value]);
        }
    }
}
