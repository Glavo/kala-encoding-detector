// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import org.jetbrains.annotations.NotNullByDefault;

/// Controls how detected encoding names are exposed by the public API.
@NotNullByDefault
public enum EncodingNameStyle {
    /// Uses names compatible with chardet 5.x and 6.x where they differ.
    CHARDET_COMPATIBLE,

    /// Uses the detector registry's canonical names.
    CANONICAL
}
