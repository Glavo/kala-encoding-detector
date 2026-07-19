// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.Encoding;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/// Lazily loads and scores the bundled IDF-weighted byte-bigram models.
@NotNullByDefault
final class ModelStore {
    /// Logger used for reference-compatible resource degradation warnings.
    private static final System.Logger LOGGER = System.getLogger(ModelStore.class.getName());

    /// Model resource location.
    private static final String MODELS_RESOURCE = "/kala/encdet/internal/models.bin";

    /// IDF resource location.
    private static final String IDF_RESOURCE = "/kala/encdet/internal/idf.bin";

    /// V2 model-file magic interpreted as a big-endian integer.
    private static final int MODEL_MAGIC = 0x434d4432;

    /// Number of byte bigrams in one dense model.
    private static final int MODEL_SIZE = 65_536;

    /// Maximum accepted profile count from the reference format.
    private static final int MAX_MODELS = 10_000;

    /// Prevents instantiation of this static model store.
    private ModelStore() {
    }

    /// Reports whether language variants exist for an encoding.
    ///
    /// @param encoding encoding identity
    /// @return whether a statistical model is available
    static boolean hasVariants(Encoding encoding) {
        return ModelsHolder.MODELS.index().containsKey(encoding);
    }

    /// Infers a language for an encoding having exactly one registered language.
    ///
    /// @param encoding encoding identity
    /// @return the sole language, or `null`
    static @Nullable String inferSingleLanguage(Encoding encoding) {
        @Nullable EncodingRegistry.Info info = EncodingRegistry.get(encoding);
        if (info == null || info.languages().size() != 1) {
            return null;
        }
        return info.languages().get(0);
    }

    /// Creates an IDF-weighted bigram profile for a byte sequence.
    ///
    /// @param data normalized read-only bytes to profile
    /// @return reusable profile
    static Profile profile(@UnmodifiableView ByteBuffer data) {
        return new Profile(data, IdfHolder.WEIGHTS);
    }

    /// Creates a profile from a dense weighted-frequency table.
    ///
    /// @param frequencies a 65,536-entry table whose contents are copied
    /// @return reusable profile
    static Profile profileFromWeightedFrequencies(int @Unmodifiable [] frequencies) {
        return new Profile(frequencies);
    }

    /// Returns one unsigned IDF weight.
    ///
    /// @param bigram byte-bigram index
    /// @return weight in `[0, 255]`
    static int idfWeight(int bigram) {
        return Byte.toUnsignedInt(IdfHolder.WEIGHTS[bigram]);
    }

    /// Scores one encoding's language variants against a profile.
    ///
    /// @param encoding encoding identity
    /// @param profile  reusable input profile
    /// @return best score and corresponding language
    static Score scoreBestLanguage(Encoding encoding, Profile profile) {
        @Nullable List<Model> variants = ModelsHolder.MODELS.index().get(encoding);
        if (variants == null || profile.inputNorm() == 0.0) {
            return Score.NONE;
        }

        double bestScore = 0.0;
        @Nullable String bestLanguage = null;
        for (Model model : variants) {
            double score = score(profile, model);
            if (score > bestScore) {
                bestScore = score;
                bestLanguage = model.language();
            }
        }
        return new Score(bestScore, bestLanguage);
    }

    /// Computes cosine similarity between one profile and model.
    ///
    /// @param profile input profile
    /// @param model   trained model
    /// @return cosine similarity, or zero for a zero-norm vector
    private static double score(Profile profile, Model model) {
        if (profile.inputNorm() == 0.0 || model.norm() == 0.0) {
            return 0.0;
        }
        long dot = 0L;
        int[] frequencies = profile.frequencies();
        for (int index : profile.nonzero()) {
            dot += (long) model.weight(index) * frequencies[index];
        }
        return dot / (model.norm() * profile.inputNorm());
    }

    /// Loads and parses all bundled models.
    ///
    /// Missing or empty data disables statistical scoring with a warning;
    /// malformed nonempty data is a fatal installation error.
    ///
    /// @return immutable model indexes
    private static Models loadModels() {
        byte @Nullable [] raw = readResource(MODELS_RESOURCE);
        if (raw == null || raw.length == 0) {
            LOGGER.log(
                    System.Logger.Level.WARNING,
                    "Bundled models.bin is missing or empty; statistical encoding detection is disabled"
            );
            return Models.EMPTY;
        }
        try {
            return parseModels(raw);
        } catch (IllegalArgumentException | DataFormatException exception) {
            throw new IllegalStateException("Corrupt models.bin: " + exception.getMessage(), exception);
        }
    }

    /// Parses the dense v2 zlib-compressed model format.
    ///
    /// @param raw complete resource contents
    /// @return immutable model indexes
    /// @throws DataFormatException if the zlib stream is invalid
    private static Models parseModels(byte @Unmodifiable [] raw) throws DataFormatException {
        ByteBuffer header = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);
        requireRemaining(header, Integer.BYTES, "missing CMD2 magic");
        if (header.getInt() != MODEL_MAGIC) {
            throw new IllegalArgumentException("missing CMD2 magic");
        }
        requireRemaining(header, Integer.BYTES, "missing model count");
        long unsignedCount = Integer.toUnsignedLong(header.getInt());
        if (unsignedCount > MAX_MODELS) {
            throw new IllegalArgumentException("model count " + unsignedCount + " exceeds " + MAX_MODELS);
        }
        int count = (int) unsignedCount;

        ArrayList<ModelHeader> headers = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            requireRemaining(header, Integer.BYTES, "truncated model name length");
            long unsignedLength = Integer.toUnsignedLong(header.getInt());
            if (unsignedLength > 256) {
                throw new IllegalArgumentException("model name length " + unsignedLength + " exceeds 256");
            }
            int length = (int) unsignedLength;
            requireRemaining(header, length + Double.BYTES, "truncated model header");
            byte[] encodedName = new byte[length];
            header.get(encodedName);
            String name;
            try {
                name = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(encodedName))
                        .toString();
            } catch (CharacterCodingException exception) {
                throw new IllegalArgumentException("model name is not valid UTF-8", exception);
            }
            int separator = name.indexOf('/');
            if (separator <= 0 || separator == name.length() - 1) {
                throw new IllegalArgumentException("invalid model key " + name);
            }
            String language = name.substring(0, separator);
            String encodingName = name.substring(separator + 1);
            @Nullable Encoding encoding = EncodingRegistry.lookup(encodingName);
            if (encoding == null) {
                throw new IllegalArgumentException(
                        "unknown encoding " + encodingName + " in model key " + name
                );
            }
            headers.add(new ModelHeader(language, encoding, header.getDouble()));
        }

        int expectedSize;
        try {
            expectedSize = Math.multiplyExact(count, MODEL_SIZE);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("decompressed model data is too large", exception);
        }
        byte[] dense = inflate(raw, header.position(), expectedSize);

        EnumMap<Encoding, List<Model>> mutableIndex = new EnumMap<>(Encoding.class);
        for (int index = 0; index < headers.size(); index++) {
            ModelHeader modelHeader = headers.get(index);
            Model model = new Model(
                    modelHeader.language(),
                    dense,
                    index * MODEL_SIZE,
                    modelHeader.norm()
            );
            mutableIndex.computeIfAbsent(
                    modelHeader.encoding(),
                    ignored -> new ArrayList<>()
            ).add(model);
        }

        EnumMap<Encoding, List<Model>> modelIndex = new EnumMap<>(Encoding.class);
        for (Map.Entry<Encoding, List<Model>> entry : mutableIndex.entrySet()) {
            modelIndex.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return new Models(Collections.unmodifiableMap(modelIndex), dense);
    }

    /// Inflates a zlib stream to an exact expected length.
    ///
    /// @param raw          resource contents
    /// @param offset       first compressed byte
    /// @param expectedSize required output length
    /// @return decompressed dense models
    /// @throws DataFormatException if decompression fails or length differs
    private static byte[] inflate(
            byte @Unmodifiable [] raw,
            int offset,
            int expectedSize
    ) throws DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(raw, offset, raw.length - offset);
        byte[] result = new byte[expectedSize];
        int written = 0;
        try {
            while (!inflater.finished() && written < result.length) {
                int count = inflater.inflate(result, written, result.length - written);
                if (count == 0) {
                    if (inflater.needsDictionary()) {
                        throw new DataFormatException("zlib stream requires a dictionary");
                    }
                    if (inflater.needsInput()) {
                        break;
                    }
                }
                written += count;
            }
            if (!inflater.finished() || written != expectedSize) {
                throw new DataFormatException(
                        "decompressed size " + written + " does not equal expected " + expectedSize
                );
            }
            return result;
        } finally {
            inflater.end();
        }
    }

    /// Loads the IDF table or reference-compatible uniform fallback weights.
    ///
    /// @return an immutable 65,536-byte table
    private static byte @Unmodifiable [] loadIdfWeights() {
        byte @Nullable [] raw = readResource(IDF_RESOURCE);
        if (raw != null && raw.length == MODEL_SIZE) {
            return raw;
        }
        int length = raw == null ? -1 : raw.length;
        LOGGER.log(
                System.Logger.Level.WARNING,
                "Bundled idf.bin has size " + length + "; uniform statistical weights will be used"
        );
        byte[] fallback = new byte[MODEL_SIZE];
        Arrays.fill(fallback, (byte) 1);
        return fallback;
    }

    /// Reads a complete classpath resource.
    ///
    /// @param path absolute classpath resource path
    /// @return resource bytes, or `null` when absent
    private static byte @Nullable [] readResource(String path) {
        @Nullable InputStream input = ModelStore.class.getResourceAsStream(path);
        if (input == null) {
            return null;
        }
        try (input) {
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read bundled resource " + path, exception);
        }
    }

    /// Validates a minimum number of remaining bytes in a header buffer.
    ///
    /// @param buffer   header buffer
    /// @param required required remaining bytes
    /// @param message  corruption detail
    private static void requireRemaining(ByteBuffer buffer, int required, String message) {
        if (required < 0 || buffer.remaining() < required) {
            throw new IllegalArgumentException(message);
        }
    }

    /// Initialization-on-demand holder for parsed models.
    @NotNullByDefault
    private static final class ModelsHolder {
        /// Lazily initialized immutable models.
        private static final Models MODELS = loadModels();

        /// Prevents holder instantiation.
        private ModelsHolder() {
        }
    }

    /// Initialization-on-demand holder for IDF weights.
    @NotNullByDefault
    private static final class IdfHolder {
        /// Lazily initialized immutable weights.
        private static final byte @Unmodifiable [] WEIGHTS = loadIdfWeights();

        /// Prevents holder instantiation.
        private IdfHolder() {
        }
    }

    /// Holds one parsed model header before dense data is indexed.
    ///
    /// @param language model language
    /// @param encoding encoding identity
    /// @param norm     precomputed L2 model norm
    @NotNullByDefault
    private record ModelHeader(String language, Encoding encoding, double norm) {
        /// Creates a parsed model header.
        private ModelHeader {
        }
    }

    /// Provides a view of one 65,536-byte model inside the shared dense blob.
    ///
    /// @param language model language
    /// @param dense    shared dense blob
    /// @param offset   model offset
    /// @param norm     precomputed model norm
    @NotNullByDefault
    private record Model(
            String language,
            byte @Unmodifiable [] dense,
            int offset,
            double norm
    ) {
        /// Creates a model view.
        private Model {
        }

        /// Returns one unsigned model weight.
        ///
        /// @param index byte-bigram index
        /// @return weight in `[0, 255]`
        private int weight(int index) {
            return Byte.toUnsignedInt(dense[offset + index]);
        }
    }

    /// Holds immutable model indexes and retains their shared dense blob.
    ///
    /// @param index encoding-to-variant index
    /// @param dense shared immutable dense data retained by all model views
    @NotNullByDefault
    private record Models(
            @Unmodifiable Map<Encoding, @Unmodifiable List<Model>> index,
            byte @Unmodifiable [] dense
    ) {
        /// Empty models used when the resource is absent or empty.
        private static final Models EMPTY = new Models(
                Collections.unmodifiableMap(new EnumMap<>(Encoding.class)),
                new byte[0]
        );

        /// Creates immutable model storage.
        private Models {
        }
    }

    /// Stores a reusable IDF-weighted input profile.
    @NotNullByDefault
    static final class Profile {
        /// Dense weighted bigram frequencies.
        private final int @Unmodifiable [] frequencies;

        /// Indices whose frequencies are nonzero, in first-seen order.
        private final int @Unmodifiable [] nonzero;

        /// L2 input norm.
        private final double inputNorm;

        /// Creates a weighted input profile.
        ///
        /// @param data bytes to profile
        /// @param idf  unsigned IDF weights
        private Profile(
                @UnmodifiableView ByteBuffer data,
                byte @Unmodifiable [] idf
        ) {
            int bigramCount = data.limit() - 1;
            if (bigramCount <= 0) {
                this.frequencies = new int[0];
                this.nonzero = new int[0];
                this.inputNorm = 0.0;
                return;
            }

            int[] mutableFrequencies = new int[MODEL_SIZE];
            int[] mutableNonzero = new int[Math.min(bigramCount, MODEL_SIZE)];
            int distinct = 0;
            for (int index = 0; index < bigramCount; index++) {
                int bigram = (Byte.toUnsignedInt(data.get(index)) << 8)
                        | Byte.toUnsignedInt(data.get(index + 1));
                if (mutableFrequencies[bigram] == 0) {
                    mutableNonzero[distinct++] = bigram;
                }
                mutableFrequencies[bigram] += Byte.toUnsignedInt(idf[bigram]);
            }
            long normSquared = 0L;
            for (int index = 0; index < distinct; index++) {
                long value = mutableFrequencies[mutableNonzero[index]];
                normSquared += value * value;
            }
            this.frequencies = mutableFrequencies;
            this.nonzero = Arrays.copyOf(mutableNonzero, distinct);
            this.inputNorm = Math.sqrt(normSquared);
        }

        /// Creates a profile by copying preweighted dense frequencies.
        ///
        /// @param weightedFrequencies 65,536 weighted bigram counts
        /// @throws IllegalArgumentException if the table length is not 65,536
        private Profile(int @Unmodifiable [] weightedFrequencies) {
            if (weightedFrequencies.length != MODEL_SIZE) {
                throw new IllegalArgumentException("weightedFrequencies must contain 65,536 entries");
            }
            int[] mutableFrequencies = weightedFrequencies.clone();
            int distinct = 0;
            for (int value : mutableFrequencies) {
                if (value != 0) {
                    distinct++;
                }
            }
            int[] mutableNonzero = new int[distinct];
            long normSquared = 0L;
            int output = 0;
            for (int index = 0; index < mutableFrequencies.length; index++) {
                int value = mutableFrequencies[index];
                if (value != 0) {
                    mutableNonzero[output++] = index;
                    normSquared += (long) value * value;
                }
            }
            this.frequencies = mutableFrequencies;
            this.nonzero = mutableNonzero;
            this.inputNorm = Math.sqrt(normSquared);
        }

        /// Returns dense weighted frequencies.
        ///
        /// @return immutable internal frequency array
        private int @Unmodifiable [] frequencies() {
            return frequencies;
        }

        /// Returns indices with nonzero frequencies.
        ///
        /// @return immutable internal index array
        private int @Unmodifiable [] nonzero() {
            return nonzero;
        }

        /// Returns the profile norm.
        ///
        /// @return L2 norm
        private double inputNorm() {
            return inputNorm;
        }
    }

    /// Returns one statistical score and its best language.
    ///
    /// @param score    cosine similarity
    /// @param language best ISO 639 language code, or `null`
    @NotNullByDefault
    record Score(double score, @Nullable String language) {
        /// Zero score used when no model applies.
        private static final Score NONE = new Score(0.0, null);

        /// Creates a model score.
        Score {
        }
    }
}
