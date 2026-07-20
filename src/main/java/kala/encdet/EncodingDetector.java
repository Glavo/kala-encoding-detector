// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import kala.encdet.internal.ByteBufferSupport;
import kala.encdet.internal.DetectionEngine;
import kala.encdet.internal.EncodingRegistry;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Detects character encodings using immutable reusable configuration.
///
/// Instances are safe for concurrent use. Shared registry and model state is
/// immutable after lazy initialization, and every detection invocation uses a
/// separate context. Configuration methods return independent detector
/// instances and never modify their receiver.
///
/// Candidate eligibility is evaluated in era, inclusion, then exclusion
/// order. Exclusion therefore wins when the same encoding occurs in both
/// filters. The resulting eligibility set also gates BOM, markup, escape, and
/// fallback results; binary classifications have no encoding and are not
/// filtered. If a configured fallback is ineligible, the detector returns a
/// no-encoding result instead.
///
/// Preferred-superset remapping, when enabled, occurs after candidate
/// filtering. A reported superset may therefore be absent from the inclusion
/// filter or present in the exclusion filter.
@NotNullByDefault
public final class EncodingDetector {
    /// Confidence threshold used by [#detectAll(byte[])] and
    /// [#detectAll(ByteBuffer)].
    public static final double MINIMUM_THRESHOLD = 0.20;

    /// Default detector matching the configuration described by
    /// [#EncodingDetector()].
    public static final EncodingDetector DEFAULT = new EncodingDetector();

    /// Eligible encoding eras in enum declaration order.
    private final @Unmodifiable Set<EncodingEra> encodingEras;

    /// Maximum number of leading input bytes examined.
    private final int maxBytes;

    /// Whether subset encodings are remapped to preferred supersets.
    private final boolean preferSuperset;

    /// Optional encoding inclusion filter.
    private final @Nullable
    @Unmodifiable Set<Encoding> includeEncodings;

    /// Optional encoding exclusion filter.
    private final @Nullable
    @Unmodifiable Set<Encoding> excludeEncodings;

    /// Low-confidence fallback used when no candidate survives.
    private final Encoding noMatchEncoding;

    /// Low-confidence fallback used for empty input.
    private final Encoding emptyInputEncoding;

    /// Creates a detector with the default configuration.
    ///
    /// The default enables every encoding era, examines at most 200,000 bytes,
    /// disables preferred-superset remapping and both filters, uses [Encoding#CP1252]
    /// when no candidate survives, and uses [Encoding#UTF_8] for empty input.
    public EncodingDetector() {
        this(
                EnumSet.allOf(EncodingEra.class),
                200_000,
                false,
                null,
                null,
                Encoding.CP1252,
                Encoding.UTF_8
        );
    }

    /// Creates and validates one immutable detector configuration.
    ///
    /// @param encodingEras       eligible encoding eras
    /// @param maxBytes           maximum leading input bytes examined
    /// @param preferSuperset     whether to remap subset encodings
    /// @param includeEncodings   optional inclusion filter
    /// @param excludeEncodings   optional exclusion filter
    /// @param noMatchEncoding    no-match fallback
    /// @param emptyInputEncoding empty-input fallback
    /// @throws NullPointerException     if a required value, collection element,
    /// or encoding is `null`
    /// @throws IllegalArgumentException if an era set or supplied encoding
    /// filter is empty or `maxBytes` is not positive
    private EncodingDetector(
            Set<EncodingEra> encodingEras,
            int maxBytes,
            boolean preferSuperset,
            @Nullable Set<Encoding> includeEncodings,
            @Nullable Set<Encoding> excludeEncodings,
            Encoding noMatchEncoding,
            Encoding emptyInputEncoding
    ) {
        if (encodingEras.isEmpty()) {
            throw new IllegalArgumentException("encodingEras must not be empty");
        }
        if (maxBytes < 1) {
            throw new IllegalArgumentException("maxBytes must be a positive integer");
        }

        this.encodingEras = immutableEras(encodingEras);
        this.maxBytes = maxBytes;
        this.preferSuperset = preferSuperset;
        this.includeEncodings = immutableEncodings(includeEncodings, "includeEncodings");
        this.excludeEncodings = immutableEncodings(excludeEncodings, "excludeEncodings");
        this.noMatchEncoding = Objects.requireNonNull(noMatchEncoding, "noMatchEncoding");
        this.emptyInputEncoding = Objects.requireNonNull(emptyInputEncoding, "emptyInputEncoding");
    }

    /// Returns the eligible encoding eras.
    ///
    /// @return an immutable set in enum declaration order
    public @Unmodifiable Set<EncodingEra> encodingEras() {
        return encodingEras;
    }

    /// Returns the maximum number of leading bytes examined.
    ///
    /// @return a positive byte count
    public int maxBytes() {
        return maxBytes;
    }

    /// Reports whether subset encodings are remapped to preferred supersets.
    ///
    /// @return whether preferred-superset remapping is enabled
    public boolean preferSuperset() {
        return preferSuperset;
    }

    /// Returns the encoding inclusion filter.
    ///
    /// @return immutable encodings in enum declaration order, or `null` when unrestricted
    public @Nullable @Unmodifiable Set<Encoding> includeEncodings() {
        return includeEncodings;
    }

    /// Returns the encoding exclusion filter.
    ///
    /// @return immutable encodings in enum declaration order, or `null` when disabled
    public @Nullable @Unmodifiable Set<Encoding> excludeEncodings() {
        return excludeEncodings;
    }

    /// Returns the no-match fallback.
    ///
    /// @return fallback encoding
    public Encoding noMatchEncoding() {
        return noMatchEncoding;
    }

    /// Returns the empty-input fallback.
    ///
    /// @return fallback encoding
    public Encoding emptyInputEncoding() {
        return emptyInputEncoding;
    }

    /// Returns a detector restricted to the supplied encoding eras.
    ///
    /// @param value a nonempty era set
    /// @return an independently configured detector
    /// @throws NullPointerException     if `value` or an element is `null`
    /// @throws IllegalArgumentException if `value` is empty
    public EncodingDetector withEncodingEras(Set<EncodingEra> value) {
        return copy(
                value,
                maxBytes,
                preferSuperset,
                includeEncodings,
                excludeEncodings,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector restricted to one encoding era.
    ///
    /// @param value the sole eligible era
    /// @return an independently configured detector
    /// @throws NullPointerException if `value` is `null`
    public EncodingDetector withEncodingEra(EncodingEra value) {
        return withEncodingEras(EnumSet.of(value));
    }

    /// Returns a detector with a new maximum input length.
    ///
    /// @param value a positive byte count
    /// @return an independently configured detector
    /// @throws IllegalArgumentException if `value` is not positive
    public EncodingDetector withMaxBytes(int value) {
        return copy(
                encodingEras,
                value,
                preferSuperset,
                includeEncodings,
                excludeEncodings,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector with preferred-superset remapping enabled or disabled.
    ///
    /// @param value whether to remap subset encodings
    /// @return an independently configured detector
    public EncodingDetector withPreferredSuperset(boolean value) {
        return copy(
                encodingEras,
                maxBytes,
                value,
                includeEncodings,
                excludeEncodings,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector using an optional encoding inclusion filter.
    ///
    /// @param value encodings to include, or `null` to disable the filter
    /// @return an independently configured detector
    /// @throws NullPointerException     if an element is `null`
    /// @throws IllegalArgumentException if the set is empty
    public EncodingDetector withIncludedEncodings(@Nullable Set<Encoding> value) {
        return copy(
                encodingEras,
                maxBytes,
                preferSuperset,
                value,
                excludeEncodings,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector using an optional encoding exclusion filter.
    ///
    /// @param value encodings to exclude, or `null` to disable the filter
    /// @return an independently configured detector
    /// @throws NullPointerException     if an element is `null`
    /// @throws IllegalArgumentException if the set is empty
    public EncodingDetector withExcludedEncodings(@Nullable Set<Encoding> value) {
        return copy(
                encodingEras,
                maxBytes,
                preferSuperset,
                includeEncodings,
                value,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector using the supplied no-match fallback.
    ///
    /// @param value fallback encoding
    /// @return an independently configured detector
    /// @throws NullPointerException     if `value` is `null`
    public EncodingDetector withNoMatchEncoding(Encoding value) {
        return copy(
                encodingEras,
                maxBytes,
                preferSuperset,
                includeEncodings,
                excludeEncodings,
                value,
                emptyInputEncoding
        );
    }

    /// Returns a detector using the supplied empty-input fallback.
    ///
    /// @param value fallback encoding
    /// @return an independently configured detector
    /// @throws NullPointerException     if `value` is `null`
    public EncodingDetector withEmptyInputEncoding(Encoding value) {
        return copy(
                encodingEras,
                maxBytes,
                preferSuperset,
                includeEncodings,
                excludeEncodings,
                noMatchEncoding,
                value
        );
    }

    /// Returns the highest-ranked result.
    ///
    /// Only the first [#maxBytes()] bytes are examined. The input array is read
    /// directly without copying, is not retained after this method returns, and
    /// is never modified. Its contents must not be changed during detection.
    ///
    /// @param input bytes to examine
    /// @return highest-ranked result
    /// @throws NullPointerException if `input` is `null`
    public DetectionResult detect(byte[] input) {
        return detectNormalized(ByteBufferSupport.wrap(input));
    }

    /// Returns the highest-ranked result for the remaining buffer bytes.
    ///
    /// Only the first [#maxBytes()] bytes between the buffer's position and
    /// limit are examined. The buffer's content, position, limit, and mark are
    /// not modified, and the buffer is not retained. Its underlying bytes are
    /// read directly without copying and must not change during detection.
    ///
    /// @param input buffer whose remaining bytes are examined
    /// @return highest-ranked result
    /// @throws NullPointerException if `input` is `null`
    public DetectionResult detect(ByteBuffer input) {
        return detectNormalized(ByteBufferSupport.view(input));
    }

    /// Returns candidates above the default confidence threshold.
    ///
    /// If no candidate has confidence greater than `0.20`, the unfiltered
    /// candidates are returned. The result is sorted stably by descending
    /// confidence and cannot be modified. Only the first [#maxBytes()] input
    /// bytes are examined. The input array is read directly without copying,
    /// is not retained, and must not change during detection.
    ///
    /// @param input bytes to examine
    /// @return immutable filtered candidates
    /// @throws NullPointerException if `input` is `null`
    public @Unmodifiable List<DetectionResult> detectAll(byte[] input) {
        List<DetectionResult> all = detectAllUnfiltered(input);
        List<DetectionResult> filtered = all.stream()
                .filter(result -> result.confidence() > MINIMUM_THRESHOLD)
                .toList();
        return filtered.isEmpty() ? all : filtered;
    }

    /// Returns candidates above the default confidence threshold for a buffer.
    ///
    /// Only the first [#maxBytes()] remaining bytes are examined. The buffer's
    /// content, position, limit, and mark are not modified, and its underlying
    /// bytes are read directly without copying. If no candidate has confidence
    /// greater than `0.20`, the unfiltered candidates are returned. The result
    /// is sorted stably by descending confidence and cannot be modified. The
    /// underlying bytes must not change during detection.
    ///
    /// @param input buffer whose remaining bytes are examined
    /// @return immutable filtered candidates
    /// @throws NullPointerException if `input` is `null`
    public @Unmodifiable List<DetectionResult> detectAll(ByteBuffer input) {
        List<DetectionResult> all = detectAllUnfiltered(input);
        List<DetectionResult> filtered = all.stream()
                .filter(result -> result.confidence() > MINIMUM_THRESHOLD)
                .toList();
        return filtered.isEmpty() ? all : filtered;
    }

    /// Returns every detection candidate.
    ///
    /// The result is sorted stably by descending confidence and cannot be
    /// modified. Only the first [#maxBytes()] bytes are examined, and the
    /// input array is read directly without copying or modification, is not
    /// retained, and must not change during detection.
    ///
    /// @param input bytes to examine
    /// @return immutable unfiltered candidates
    /// @throws NullPointerException if `input` is `null`
    public @Unmodifiable List<DetectionResult> detectAllUnfiltered(byte[] input) {
        return detectAllUnfilteredNormalized(
                ByteBufferSupport.wrap(input)
        );
    }

    /// Returns every detection candidate for the remaining buffer bytes.
    ///
    /// The result is sorted stably by descending confidence and cannot be
    /// modified. Only the first [#maxBytes()] bytes between the buffer's
    /// position and limit are examined. The buffer's content, position, limit,
    /// and mark are not modified, and the buffer is not retained. Its
    /// underlying bytes are read directly without copying and must not change
    /// during detection.
    ///
    /// @param input buffer whose remaining bytes are examined
    /// @return immutable unfiltered candidates
    /// @throws NullPointerException if `input` is `null`
    public @Unmodifiable List<DetectionResult> detectAllUnfiltered(ByteBuffer input) {
        return detectAllUnfilteredNormalized(ByteBufferSupport.view(input));
    }

    /// Returns the highest-ranked result for a normalized zero-copy buffer view.
    ///
    /// @param input bytes to examine
    /// @return highest-ranked result
    private DetectionResult detectNormalized(@UnmodifiableView ByteBuffer input) {
        return DetectionEngine.detect(input, this).get(0);
    }

    /// Returns every candidate for a normalized zero-copy buffer view.
    ///
    /// @param input bytes to examine
    /// @return immutable unfiltered candidates
    private @Unmodifiable List<DetectionResult> detectAllUnfilteredNormalized(
            @UnmodifiableView ByteBuffer input
    ) {
        return DetectionEngine.detect(input, this).stream()
                .sorted(Comparator.comparingDouble(DetectionResult::confidence).reversed())
                .toList();
    }

    /// Resolves an encoding from a canonical name or alias.
    ///
    /// Resolution is case-insensitive and does not consult installed Java
    /// charset providers.
    ///
    /// @param name name to resolve
    /// @return resolved encoding, or `null` if unknown
    /// @throws NullPointerException if `name` is `null`
    public static @Nullable Encoding lookupEncoding(String name) {
        return EncodingRegistry.lookup(name);
    }

    /// Returns all supported encodings in registry order.
    ///
    /// @return an immutable ordered set
    public static @Unmodifiable Set<Encoding> supportedEncodings() {
        return EncodingRegistry.supportedEncodings();
    }

    /// Creates an immutable copy with all supplied configuration values.
    ///
    /// @param eras              eligible encoding eras
    /// @param maximumBytes      maximum leading bytes
    /// @param preferredSuperset preferred-superset setting
    /// @param included          optional inclusion filter
    /// @param excluded          optional exclusion filter
    /// @param noMatch           no-match fallback
    /// @param emptyInput        empty-input fallback
    /// @return a validated independent detector
    private static EncodingDetector copy(
            Set<EncodingEra> eras,
            int maximumBytes,
            boolean preferredSuperset,
            @Nullable Set<Encoding> included,
            @Nullable Set<Encoding> excluded,
            Encoding noMatch,
            Encoding emptyInput
    ) {
        return new EncodingDetector(
                eras,
                maximumBytes,
                preferredSuperset,
                included,
                excluded,
                noMatch,
                emptyInput
        );
    }

    /// Copies an era set while preserving enum declaration order.
    ///
    /// @param eras eras to copy
    /// @return an immutable set
    private static @Unmodifiable Set<EncodingEra> immutableEras(Set<EncodingEra> eras) {
        EnumSet<EncodingEra> copy = EnumSet.copyOf(eras);
        return Collections.unmodifiableSet(copy);
    }

    /// Copies an optional encoding filter.
    ///
    /// @param encodings     supplied encodings, or `null`
    /// @param parameterName parameter used in an error message
    /// @return immutable encoding set, or `null`
    private static @Nullable @Unmodifiable Set<Encoding> immutableEncodings(
            @Nullable Set<Encoding> encodings,
            String parameterName
    ) {
        if (encodings == null) {
            return null;
        }
        if (encodings.isEmpty()) {
            throw new IllegalArgumentException(
                    parameterName + " must not be empty; pass null to disable filtering"
            );
        }
        EnumSet<Encoding> copy = EnumSet.noneOf(Encoding.class);
        copy.addAll(encodings);
        return Collections.unmodifiableSet(copy);
    }
}
