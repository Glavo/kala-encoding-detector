// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.Encoding;
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
        List<EncodingRegistry.Info> entries = EncodingRegistry.candidates(EncodingDetector.DEFAULT);
        assertEquals(86, entries.size());
        assertEquals(6, EncodingEra.values().length);

        LinkedHashSet<Encoding> encodings = new LinkedHashSet<>();
        HashSet<String> aliases = new HashSet<>();
        HashSet<String> languages = new HashSet<>();
        for (EncodingRegistry.Info entry : entries) {
            Encoding encoding = entry.encoding();
            assertTrue(encodings.add(encoding), encoding.canonicalName());
            assertEquals(encoding, EncodingDetector.lookupEncoding(encoding.canonicalName()));
            aliases.add(encoding.canonicalName().toLowerCase(Locale.ROOT));
            languages.addAll(entry.languages());
            for (String alias : entry.aliases()) {
                assertEquals(encoding, EncodingDetector.lookupEncoding(alias), alias);
                assertEquals(
                        encoding,
                        EncodingDetector.lookupEncoding(alias.toUpperCase(Locale.ROOT)),
                        alias
                );
                aliases.add(alias.toLowerCase(Locale.ROOT));
            }
        }

        assertEquals(EncodingDetector.supportedEncodings(), encodings);
        assertEquals(
                List.of(Encoding.values()),
                entries.stream().map(EncodingRegistry.Info::encoding).toList()
        );
        assertEquals(604, aliases.size());
        assertEquals(49, languages.size());
    }

    /// Verifies representative IANA, WHATWG, and Python punctuation variants.
    @Test
    void normalizedStandardsAliasesResolveWithoutCharsetProviders() {
        assertEquals(Encoding.ASCII, EncodingDetector.lookupEncoding("ANSI X3.4 1986"));
        assertEquals(Encoding.ISO_8859_1, EncodingDetector.lookupEncoding("ISO 8859 1:1987"));
        assertEquals(Encoding.CP932, EncodingDetector.lookupEncoding("windows 31j"));
        assertEquals(Encoding.GB18030, EncodingDetector.lookupEncoding("gb 2312-80"));
        assertEquals(Encoding.MAC_CYRILLIC, EncodingDetector.lookupEncoding("x mac cyrillic"));
    }
}
