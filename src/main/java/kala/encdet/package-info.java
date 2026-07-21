// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

/// Provides pure-Java character-encoding detection APIs.
///
/// Detection results use [kala.encdet.EncodingDetector.Encoding] target
/// identities rather than exact decoder identities. Textual aliases may be
/// folded into a related detection target, and the target's names are intended
/// for interchange and presentation. Use
/// [kala.encdet.EncodingDetector.Encoding#charset()] to obtain an exact
/// `java.nio.charset.Charset` mapping when the current runtime provides one, or
/// [kala.encdet.EncodingDetector.Encoding#approximateCharset()] to permit a
/// configured related mapping and an ultimate US-ASCII fallback.
@org.jetbrains.annotations.NotNullByDefault

package kala.encdet;
