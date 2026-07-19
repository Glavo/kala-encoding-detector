// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.DetectionOptions;
import kala.encdet.EncodingDetector;
import kala.encdet.EncodingEra;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the complete ordered encoding registry and its alias indexes.
@NotNullByDefault
final class RegistryCoverageTest {
    /// Verifies all targets, aliases, eras, and languages from the pinned registry.
    @Test
    void registryHasCompletePinnedCoverage() {
        List<EncodingRegistry.Info> entries = EncodingRegistry.candidates(DetectionOptions.DEFAULT);
        assertEquals(86, entries.size());
        assertEquals(6, EncodingEra.values().length);

        LinkedHashSet<String> canonicalNames = new LinkedHashSet<>();
        HashSet<String> aliases = new HashSet<>();
        HashSet<String> languages = new HashSet<>();
        for (EncodingRegistry.Info entry : entries) {
            assertTrue(canonicalNames.add(entry.name()), entry.name());
            assertEquals(entry.name(), EncodingDetector.lookupEncoding(entry.name()));
            aliases.add(entry.name().toLowerCase(Locale.ROOT));
            languages.addAll(entry.languages());
            for (String alias : entry.aliases()) {
                assertEquals(entry.name(), EncodingDetector.lookupEncoding(alias), alias);
                assertEquals(
                        entry.name(),
                        EncodingDetector.lookupEncoding(alias.toUpperCase(Locale.ROOT)),
                        alias
                );
                aliases.add(alias.toLowerCase(Locale.ROOT));
            }
        }

        assertEquals(EncodingDetector.supportedEncodings(), canonicalNames);
        assertEquals(604, aliases.size());
        assertEquals(49, languages.size());
    }

    /// Verifies representative IANA, WHATWG, and Python punctuation variants.
    @Test
    void normalizedStandardsAliasesResolveWithoutCharsetProviders() {
        assertEquals("ascii", EncodingDetector.lookupEncoding("ANSI X3.4 1986"));
        assertEquals("iso8859-1", EncodingDetector.lookupEncoding("ISO 8859 1:1987"));
        assertEquals("cp932", EncodingDetector.lookupEncoding("windows 31j"));
        assertEquals("gb18030", EncodingDetector.lookupEncoding("gb 2312-80"));
        assertEquals("mac-cyrillic", EncodingDetector.lookupEncoding("x mac cyrillic"));
    }
}
