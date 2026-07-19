// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

/// Provides pure-Java character-encoding detection APIs.
///
/// Detection results use encoding-name strings because several supported
/// encodings are not required to be available from Java's charset providers.
/// A returned name therefore is not guaranteed to be accepted by
/// `java.nio.charset.Charset.forName(String)`.
@org.jetbrains.annotations.NotNullByDefault

package kala.encdet;
