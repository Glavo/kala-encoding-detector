// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import kala.encdet.internal.DetectionEngine;
import kala.encdet.internal.EncodingRegistry;
import kala.encdet.internal.ByteBufferSupport;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Detects character encodings using immutable reusable configuration.
///
/// Instances are safe for concurrent use. Shared registry and model state is
/// immutable after lazy initialization, and every detection invocation uses a
/// separate context. Configuration methods return independent detector
/// instances and never modify their receiver.
@NotNullByDefault
public final class EncodingDetector {
    /// Confidence threshold used by [#detectAll(byte[])].
    public static final double MINIMUM_THRESHOLD = 0.20;

    /// Default detector matching the reference implementation.
    public static final EncodingDetector DEFAULT = new EncodingDetector();

    /// Eligible encoding eras in enum declaration order.
    private final @Unmodifiable Set<EncodingEra> encodingEras;

    /// Maximum number of leading input bytes examined.
    private final int maxBytes;

    /// Whether subset encodings are renamed to preferred supersets.
    private final boolean preferSuperset;

    /// Naming convention applied to public results.
    private final EncodingNameStyle nameStyle;

    /// Optional canonical encoding inclusion filter.
    private final @Nullable
    @Unmodifiable Set<String> includeEncodings;

    /// Optional canonical encoding exclusion filter.
    private final @Nullable
    @Unmodifiable Set<String> excludeEncodings;

    /// Canonical low-confidence fallback used when no candidate survives.
    private final String noMatchEncoding;

    /// Canonical low-confidence fallback used for empty input.
    private final String emptyInputEncoding;

    /// Creates a detector with the default configuration.
    public EncodingDetector() {
        this(
                EnumSet.allOf(EncodingEra.class),
                200_000,
                false,
                EncodingNameStyle.CHARDET_COMPATIBLE,
                null,
                null,
                "cp1252",
                "utf-8"
        );
    }

    /// Creates and validates one immutable detector configuration.
    ///
    /// @param encodingEras       eligible encoding eras
    /// @param maxBytes           maximum leading input bytes examined
    /// @param preferSuperset     whether to rename subset encodings
    /// @param nameStyle          public result-name style
    /// @param includeEncodings   optional inclusion filter
    /// @param excludeEncodings   optional exclusion filter
    /// @param noMatchEncoding    no-match fallback
    /// @param emptyInputEncoding empty-input fallback
    /// @throws NullPointerException     if a required value, collection element,
    /// or encoding name is `null`
    /// @throws IllegalArgumentException if an era set or supplied encoding
    /// filter is empty, `maxBytes` is not positive, or an encoding is unknown
    private EncodingDetector(
            Set<EncodingEra> encodingEras,
            int maxBytes,
            boolean preferSuperset,
            EncodingNameStyle nameStyle,
            @Nullable Set<String> includeEncodings,
            @Nullable Set<String> excludeEncodings,
            String noMatchEncoding,
            String emptyInputEncoding
    ) {
        Objects.requireNonNull(encodingEras, "encodingEras");
        Objects.requireNonNull(nameStyle, "nameStyle");
        if (encodingEras.isEmpty()) {
            throw new IllegalArgumentException("encodingEras must not be empty");
        }
        if (maxBytes < 1) {
            throw new IllegalArgumentException("maxBytes must be a positive integer");
        }

        this.encodingEras = immutableEras(encodingEras);
        this.maxBytes = maxBytes;
        this.preferSuperset = preferSuperset;
        this.nameStyle = nameStyle;
        this.includeEncodings = normalizeEncodings(includeEncodings, "includeEncodings");
        this.excludeEncodings = normalizeEncodings(excludeEncodings, "excludeEncodings");
        this.noMatchEncoding = normalizeEncoding(noMatchEncoding, "noMatchEncoding");
        this.emptyInputEncoding = normalizeEncoding(emptyInputEncoding, "emptyInputEncoding");
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

    /// Reports whether subset encodings are renamed to preferred supersets.
    ///
    /// @return whether preferred-superset remapping is enabled
    public boolean preferSuperset() {
        return preferSuperset;
    }

    /// Returns the public result-name style.
    ///
    /// @return configured name style
    public EncodingNameStyle nameStyle() {
        return nameStyle;
    }

    /// Returns the canonical encoding inclusion filter.
    ///
    /// @return immutable canonical names, or `null` when unrestricted
    public @Nullable @Unmodifiable Set<String> includeEncodings() {
        return includeEncodings;
    }

    /// Returns the canonical encoding exclusion filter.
    ///
    /// @return immutable canonical names, or `null` when disabled
    public @Nullable @Unmodifiable Set<String> excludeEncodings() {
        return excludeEncodings;
    }

    /// Returns the canonical no-match fallback.
    ///
    /// @return canonical encoding name
    public String noMatchEncoding() {
        return noMatchEncoding;
    }

    /// Returns the canonical empty-input fallback.
    ///
    /// @return canonical encoding name
    public String emptyInputEncoding() {
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
                Objects.requireNonNull(value, "value"),
                maxBytes,
                preferSuperset,
                nameStyle,
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
        return withEncodingEras(EnumSet.of(Objects.requireNonNull(value, "value")));
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
                nameStyle,
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
                nameStyle,
                includeEncodings,
                excludeEncodings,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector using the supplied result-name style.
    ///
    /// @param value requested style
    /// @return an independently configured detector
    /// @throws NullPointerException if `value` is `null`
    public EncodingDetector withNameStyle(EncodingNameStyle value) {
        return copy(
                encodingEras,
                maxBytes,
                preferSuperset,
                Objects.requireNonNull(value, "value"),
                includeEncodings,
                excludeEncodings,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector using an optional encoding inclusion filter.
    ///
    /// @param value names to include, or `null` to disable the filter
    /// @return an independently configured detector
    /// @throws NullPointerException     if an element is `null`
    /// @throws IllegalArgumentException if the set is empty or a name is unknown
    public EncodingDetector withIncludedEncodings(@Nullable Set<String> value) {
        return copy(
                encodingEras,
                maxBytes,
                preferSuperset,
                nameStyle,
                value,
                excludeEncodings,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector using an optional encoding exclusion filter.
    ///
    /// @param value names to exclude, or `null` to disable the filter
    /// @return an independently configured detector
    /// @throws NullPointerException     if an element is `null`
    /// @throws IllegalArgumentException if the set is empty or a name is unknown
    public EncodingDetector withExcludedEncodings(@Nullable Set<String> value) {
        return copy(
                encodingEras,
                maxBytes,
                preferSuperset,
                nameStyle,
                includeEncodings,
                value,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector using the supplied no-match fallback.
    ///
    /// @param value an encoding name or alias
    /// @return an independently configured detector
    /// @throws NullPointerException     if `value` is `null`
    /// @throws IllegalArgumentException if `value` is unknown
    public EncodingDetector withNoMatchEncoding(String value) {
        return copy(
                encodingEras,
                maxBytes,
                preferSuperset,
                nameStyle,
                includeEncodings,
                excludeEncodings,
                Objects.requireNonNull(value, "value"),
                emptyInputEncoding
        );
    }

    /// Returns a detector using the supplied empty-input fallback.
    ///
    /// @param value an encoding name or alias
    /// @return an independently configured detector
    /// @throws NullPointerException     if `value` is `null`
    /// @throws IllegalArgumentException if `value` is unknown
    public EncodingDetector withEmptyInputEncoding(String value) {
        return copy(
                encodingEras,
                maxBytes,
                preferSuperset,
                nameStyle,
                includeEncodings,
                excludeEncodings,
                noMatchEncoding,
                Objects.requireNonNull(value, "value")
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
        return detectNormalized(ByteBufferSupport.wrap(Objects.requireNonNull(input, "input")));
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
                ByteBufferSupport.wrap(Objects.requireNonNull(input, "input"))
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
        return DetectionEngine.detect(Objects.requireNonNull(input, "input"), this).get(0);
    }

    /// Returns every candidate for a normalized zero-copy buffer view.
    ///
    /// @param input bytes to examine
    /// @return immutable unfiltered candidates
    private @Unmodifiable List<DetectionResult> detectAllUnfilteredNormalized(
            @UnmodifiableView ByteBuffer input
    ) {
        return DetectionEngine.detect(Objects.requireNonNull(input, "input"), this).stream()
                .sorted(Comparator.comparingDouble(DetectionResult::confidence).reversed())
                .toList();
    }

    /// Resolves a canonical encoding name from a canonical name or alias.
    ///
    /// Resolution is case-insensitive and does not consult installed Java
    /// charset providers.
    ///
    /// @param name name to resolve
    /// @return canonical registry name, or `null` if unknown
    /// @throws NullPointerException if `name` is `null`
    public static @Nullable String lookupEncoding(String name) {
        Objects.requireNonNull(name, "name");
        return EncodingRegistry.lookup(name);
    }

    /// Returns all canonical encoding names in registry order.
    ///
    /// @return an immutable ordered set
    public static @Unmodifiable Set<String> supportedEncodings() {
        return EncodingRegistry.supportedEncodings();
    }

    /// Creates an immutable copy with all supplied configuration values.
    ///
    /// @param eras              eligible encoding eras
    /// @param maximumBytes      maximum leading bytes
    /// @param preferredSuperset preferred-superset setting
    /// @param style             public name style
    /// @param included          optional inclusion filter
    /// @param excluded          optional exclusion filter
    /// @param noMatch           no-match fallback
    /// @param emptyInput        empty-input fallback
    /// @return a validated independent detector
    private static EncodingDetector copy(
            Set<EncodingEra> eras,
            int maximumBytes,
            boolean preferredSuperset,
            EncodingNameStyle style,
            @Nullable Set<String> included,
            @Nullable Set<String> excluded,
            String noMatch,
            String emptyInput
    ) {
        return new EncodingDetector(
                eras,
                maximumBytes,
                preferredSuperset,
                style,
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

    /// Resolves one encoding name through the detector registry.
    ///
    /// @param name          supplied name
    /// @param parameterName parameter used in an error message
    /// @return canonical registry name
    private static String normalizeEncoding(String name, String parameterName) {
        Objects.requireNonNull(name, parameterName);
        @Nullable String canonical = lookupEncoding(name);
        if (canonical == null) {
            throw new IllegalArgumentException("Unknown encoding '" + name + "' in " + parameterName);
        }
        return canonical;
    }

    /// Resolves and copies an optional encoding-name filter.
    ///
    /// @param names         supplied names, or `null`
    /// @param parameterName parameter used in an error message
    /// @return immutable canonical-name set, or `null`
    private static @Nullable @Unmodifiable Set<String> normalizeEncodings(
            @Nullable Set<String> names,
            String parameterName
    ) {
        if (names == null) {
            return null;
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException(
                    parameterName + " must not be empty; pass null to disable filtering"
            );
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>(names.size());
        for (String name : names) {
            normalized.add(normalizeEncoding(name, parameterName));
        }
        return Collections.unmodifiableSet(normalized);
    }
}
