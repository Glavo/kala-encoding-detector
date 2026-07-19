// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Describes one character-encoding detection candidate.
///
/// @param encoding   the detected encoding, or `null` when the input is
///                 classified as binary or no permitted fallback exists
/// @param confidence the confidence in the range `[0.0, 1.0]`
/// @param language   the ISO 639 language code, or `null` when undetermined
/// @param mimeType   the detected or inferred MIME type, or `null` only for a
///                 result created directly by an application
/// @apiNote An [Encoding] value does not imply that the corresponding encoding
/// is available through `java.nio.charset.Charset`.
@NotNullByDefault
public record DetectionResult(
        @Nullable Encoding encoding,
        double confidence,
        @Nullable String language,
        @Nullable String mimeType
) {
    /// Creates a detection result after validating its confidence value.
    ///
    /// @throws IllegalArgumentException if `confidence` is not finite or is
    /// outside `[0.0, 1.0]`
    public DetectionResult {
        if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be finite and in the range [0.0, 1.0]");
        }
    }
}
