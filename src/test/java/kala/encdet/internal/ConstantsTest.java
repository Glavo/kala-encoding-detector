// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies shared detector constants and lookup-table sentinels.
@NotNullByDefault
final class ConstantsTest {
    /// Verifies every UTF-7 Base64 lookup entry.
    @Test
    void mapsUtf7Base64BytesToSignedSextets() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        for (int value = 0; value < Constants.UTF7_BASE64_VALUES.length; value++) {
            assertEquals(alphabet.indexOf(value), Constants.UTF7_BASE64_VALUES[value]);
        }
    }
}
