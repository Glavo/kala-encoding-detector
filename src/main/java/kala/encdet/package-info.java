// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

/// Provides pure-Java character-encoding detection APIs.
///
/// Detection results use [kala.encdet.EncodingDetector.Encoding] target
/// identities rather than exact decoder identities. Textual aliases may be
/// folded into a related detection target, and the target's names are intended
/// for interchange and presentation. They are not guaranteed to be accepted by
/// `java.nio.charset.Charset.forName(String)`.
@org.jetbrains.annotations.NotNullByDefault

package kala.encdet;
