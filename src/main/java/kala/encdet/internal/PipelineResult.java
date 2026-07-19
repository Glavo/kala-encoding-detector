// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Holds one internal pipeline candidate before public confidence clamping.
///
/// @param encoding   canonical encoding name, or `null` for binary or no result
/// @param confidence internal ranking confidence, which may exceed one
/// @param language   ISO 639 language code, or `null`
/// @param mimeType   MIME type, or `null` before defaulting
@NotNullByDefault
record PipelineResult(
        @Nullable String encoding,
        double confidence,
        @Nullable String language,
        @Nullable String mimeType
) {
    /// Creates an internal result without clamping its ranking score.
    PipelineResult {
    }
}
