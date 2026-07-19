// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies a historical or operational group of character encodings.
///
/// Multiple eras are selected by placing values in a set. The default
/// detection options select every value.
@NotNullByDefault
public enum EncodingEra {
    /// Encodings commonly used by contemporary software and web content.
    MODERN_WEB,

    /// Legacy ISO-family encodings and closely related standards.
    LEGACY_ISO,

    /// Legacy encodings used by classic Macintosh systems.
    LEGACY_MAC,

    /// Other legacy encodings associated with a region or language.
    LEGACY_REGIONAL,

    /// Encodings historically used by DOS-compatible systems.
    DOS,

    /// Encodings historically used by mainframe systems.
    MAINFRAME
}
