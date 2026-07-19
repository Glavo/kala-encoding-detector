// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.Encoding;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Holds one internal pipeline candidate before public confidence clamping.
///
/// @param encoding   encoding, or `null` for binary or no result
/// @param confidence internal ranking confidence, which may exceed one
/// @param language   ISO 639 language code, or `null`
/// @param mimeType   MIME type, or `null` before defaulting
@NotNullByDefault
record PipelineResult(
        @Nullable Encoding encoding,
        double confidence,
        @Nullable String language,
        @Nullable String mimeType
) {
    /// Creates an internal result without clamping its ranking score.
    PipelineResult {
    }
}
