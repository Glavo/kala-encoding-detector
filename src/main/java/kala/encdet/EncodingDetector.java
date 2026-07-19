// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import kala.encdet.internal.DetectionEngine;
import kala.encdet.internal.EncodingRegistry;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Detects character encodings from complete byte sequences.
///
/// Static operations are safe for concurrent use. Shared registry and model
/// state is immutable after lazy initialization, and every invocation uses a
/// separate detection context.
@NotNullByDefault
public final class EncodingDetector {
    /// Confidence threshold used by [#detectAll(byte[])].
    public static final double MINIMUM_THRESHOLD = 0.20;

    /// Prevents instantiation of this static utility class.
    private EncodingDetector() {
    }

    /// Returns the highest-ranked result using default options.
    ///
    /// @param input the bytes to examine
    /// @return the highest-ranked result
    /// @throws NullPointerException if `input` is `null`
    public static DetectionResult detect(byte[] input) {
        return detect(input, DetectionOptions.DEFAULT);
    }

    /// Returns the highest-ranked result using the supplied options.
    ///
    /// Only the first `options.maxBytes()` bytes are examined. The input array
    /// is not retained or modified.
    ///
    /// @param input   the bytes to examine
    /// @param options detection options
    /// @return the highest-ranked result
    /// @throws NullPointerException if `input` or `options` is `null`
    public static DetectionResult detect(byte[] input, DetectionOptions options) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(options, "options");
        return DetectionEngine.detect(input, options).get(0);
    }

    /// Returns candidates above the default confidence threshold.
    ///
    /// If no candidate has confidence greater than `0.20`, the unfiltered
    /// candidates are returned. The result is sorted stably by descending
    /// confidence and cannot be modified.
    ///
    /// @param input the bytes to examine
    /// @return immutable filtered candidates
    /// @throws NullPointerException if `input` is `null`
    public static @Unmodifiable List<DetectionResult> detectAll(byte[] input) {
        return detectAll(input, DetectionOptions.DEFAULT);
    }

    /// Returns candidates above the default confidence threshold.
    ///
    /// If no candidate has confidence greater than `0.20`, the unfiltered
    /// candidates are returned. The result is sorted stably by descending
    /// confidence and cannot be modified.
    ///
    /// @param input   the bytes to examine
    /// @param options detection options
    /// @return immutable filtered candidates
    /// @throws NullPointerException if `input` or `options` is `null`
    public static @Unmodifiable List<DetectionResult> detectAll(
            byte[] input,
            DetectionOptions options
    ) {
        List<DetectionResult> all = detectAllUnfiltered(input, options);
        List<DetectionResult> filtered = all.stream()
                .filter(result -> result.confidence() > MINIMUM_THRESHOLD)
                .toList();
        return filtered.isEmpty() ? all : filtered;
    }

    /// Returns every candidate using default options.
    ///
    /// The result is sorted stably by descending confidence and cannot be
    /// modified.
    ///
    /// @param input the bytes to examine
    /// @return immutable unfiltered candidates
    /// @throws NullPointerException if `input` is `null`
    public static @Unmodifiable List<DetectionResult> detectAllUnfiltered(byte[] input) {
        return detectAllUnfiltered(input, DetectionOptions.DEFAULT);
    }

    /// Returns every candidate using the supplied options.
    ///
    /// The result is sorted stably by descending confidence and cannot be
    /// modified. Only the first `options.maxBytes()` bytes are examined, and
    /// the input array is not retained or modified.
    ///
    /// @param input   the bytes to examine
    /// @param options detection options
    /// @return immutable unfiltered candidates
    /// @throws NullPointerException if `input` or `options` is `null`
    public static @Unmodifiable List<DetectionResult> detectAllUnfiltered(
            byte[] input,
            DetectionOptions options
    ) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(options, "options");
        return DetectionEngine.detect(input, options).stream()
                .sorted(Comparator.comparingDouble(DetectionResult::confidence).reversed())
                .toList();
    }

    /// Resolves a canonical encoding name from a canonical name or alias.
    ///
    /// Resolution is case-insensitive and does not consult installed Java
    /// charset providers.
    ///
    /// @param name the name to resolve
    /// @return the canonical registry name, or `null` if the name is unknown
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
}
