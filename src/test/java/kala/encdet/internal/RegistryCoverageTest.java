// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.EncodingDetector;
import kala.encdet.EncodingDetector.Encoding;
import kala.encdet.EncodingDetector.Era;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the complete ordered encoding registry and its alias indexes.
@NotNullByDefault
final class RegistryCoverageTest {
    /// Verifies all targets, aliases, eras, and languages from the pinned registry.
    @Test
    void registryHasCompletePinnedCoverage() {
        List<EncodingRegistry.Info> entries = EncodingRegistry.candidates(EncodingDetector.DEFAULT);
        assertEquals(86, entries.size());
        assertEquals(6, Era.values().length);

        LinkedHashSet<Encoding> encodings = new LinkedHashSet<>();
        HashSet<String> aliases = new HashSet<>();
        HashSet<String> languages = new HashSet<>();
        for (EncodingRegistry.Info entry : entries) {
            Encoding encoding = entry.encoding();
            assertTrue(encodings.add(encoding), encoding.canonicalName());
            assertEquals(encoding.aliases(), entry.aliases());
            assertEquals(encoding.era(), entry.era());
            assertEquals(encoding.isMultibyte(), entry.multibyte());
            assertEquals(encoding.languages(), entry.languages());
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

    /// Verifies every embedded metadata value against the pinned registry identity.
    @Test
    void enumMetadataMatchesPinnedRegistryDigest() {
        StringBuilder data = new StringBuilder(32_768);
        data.append("# Generated from chardet e3dfaa1c75256c9d2a06103b566ea92997844f70\n");
        data.append("# name\tera\tmultibyte\tlanguages\taliases\n");
        int aliasCount = 0;
        for (Encoding encoding : Encoding.values()) {
            aliasCount += encoding.aliases().size();
            data.append(encoding.canonicalName()).append('\t')
                    .append(encoding.era()).append('\t')
                    .append(encoding.isMultibyte()).append('\t')
                    .append(String.join(",", encoding.languages())).append('\t')
                    .append(String.join("\u001f", encoding.aliases())).append('\n');
        }

        assertEquals(603, aliasCount);
        assertEquals(
                "75d70081930d8ca2960debaf642fbd76d1b3e01534cc3e6254983780426bcc2d",
                sha256(data.toString())
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> Encoding.ASCII.aliases().add("unsupported")
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> Encoding.ASCII.languages().add("en")
        );
    }

    /// Verifies representative IANA, WHATWG, and Python punctuation variants.
    @Test
    void normalizedStandardsAliasesResolveWithoutCharsetProviders() {
        assertEquals(Encoding.ASCII, EncodingDetector.lookupEncoding("ANSI X3.4 1986"));
        assertEquals(Encoding.ISO_8859_1, EncodingDetector.lookupEncoding("ISO 8859 1:1987"));
        assertEquals(Encoding.CP932, EncodingDetector.lookupEncoding("windows 31j"));
        assertEquals(Encoding.GB18030, EncodingDetector.lookupEncoding("gb 2312-80"));
        assertEquals(Encoding.MAC_CYRILLIC, EncodingDetector.lookupEncoding("x mac cyrillic"));
        assertEquals(Encoding.SHIFT_JIS_2004, EncodingDetector.lookupEncoding("ms_kanji"));
        assertEquals(Encoding.CP932, EncodingDetector.lookupEncoding("ms-kanji"));
        assertEquals(Encoding.CP932, EncodingDetector.lookupEncoding("ms kanji"));
        assertEquals(Encoding.EUC_KR, EncodingDetector.lookupEncoding("ks_c_5601_1987"));
        assertEquals(Encoding.CP949, EncodingDetector.lookupEncoding("ks_c_5601-1987"));
        assertEquals(Encoding.CP949, EncodingDetector.lookupEncoding("ks c 5601 1987"));
    }

    /// Computes the SHA-256 digest of UTF-8 text.
    ///
    /// @param data text to hash
    /// @return lower-case hexadecimal digest
    private static String sha256(String data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
