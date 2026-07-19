// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.EncodingDetector;
import kala.encdet.EncodingEra;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/// Provides the detector's provider-independent ordered encoding registry.
@NotNullByDefault
public final class EncodingRegistry {
    /// Classpath location of the generated registry table.
    private static final String RESOURCE = "/kala/encdet/internal/registry.tsv";

    /// Number of canonical targets in the pinned upstream registry.
    private static final int EXPECTED_ENTRY_COUNT = 86;

    /// Canonical registry entries in detection order.
    private static final @Unmodifiable List<Info> ENTRIES;

    /// Canonical entries indexed by canonical name.
    private static final @Unmodifiable Map<String, Info> BY_NAME;

    /// Exact case-folded canonical names and aliases.
    private static final @Unmodifiable Map<String, String> EXACT_ALIASES;

    /// Python-codec-normalized canonical names and aliases.
    private static final @Unmodifiable Map<String, String> NORMALIZED_ALIASES;

    /// Canonical names in registry order.
    private static final @Unmodifiable Set<String> SUPPORTED_ENCODINGS;

    static {
        RegistryData data = readRegistry();
        ENTRIES = data.entries();
        BY_NAME = data.byName();
        EXACT_ALIASES = data.exactAliases();
        NORMALIZED_ALIASES = data.normalizedAliases();
        SUPPORTED_ENCODINGS = data.supportedEncodings();
    }

    /// Prevents instantiation of this static registry.
    private EncodingRegistry() {
    }

    /// Returns the canonical name for a name or alias.
    ///
    /// @param name the name to resolve
    /// @return the canonical name, or `null` if unknown
    public static @Nullable String lookup(String name) {
        if (name.indexOf('\0') >= 0) {
            return null;
        }
        String exact = EXACT_ALIASES.get(name.toLowerCase(Locale.ROOT));
        if (exact != null) {
            return exact;
        }
        return NORMALIZED_ALIASES.get(normalizeCodecName(name));
    }

    /// Returns the immutable canonical-name set in registry order.
    ///
    /// @return canonical names
    public static @Unmodifiable Set<String> supportedEncodings() {
        return SUPPORTED_ENCODINGS;
    }

    /// Returns the canonical registry entry for a known name.
    ///
    /// @param canonicalName a canonical name
    /// @return the matching entry, or `null` if unknown
    static @Nullable Info get(String canonicalName) {
        return BY_NAME.get(canonicalName);
    }

    /// Returns candidates after applying era, include, and exclude filters.
    ///
    /// @param detector immutable detector configuration
    /// @return immutable entries in registry order
    static @Unmodifiable List<Info> candidates(EncodingDetector detector) {
        Set<EncodingEra> eras = detector.encodingEras();
        @Nullable Set<String> included = detector.includeEncodings();
        @Nullable Set<String> excluded = detector.excludeEncodings();
        ArrayList<Info> result = new ArrayList<>(ENTRIES.size());
        for (Info entry : ENTRIES) {
            if (!eras.contains(entry.era())) {
                continue;
            }
            if (included != null && !included.contains(entry.name())) {
                continue;
            }
            if (excluded != null && excluded.contains(entry.name())) {
                continue;
            }
            result.add(entry);
        }
        return List.copyOf(result);
    }

    /// Reads and validates the generated registry resource.
    ///
    /// @return immutable parsed registry data
    private static RegistryData readRegistry() {
        @Nullable InputStream input = EncodingRegistry.class.getResourceAsStream(RESOURCE);
        if (input == null) {
            throw new IllegalStateException("Missing encoding registry resource: " + RESOURCE);
        }

        ArrayList<Info> entries = new ArrayList<>(EXPECTED_ENTRY_COUNT);
        LinkedHashMap<String, Info> byName = new LinkedHashMap<>();
        LinkedHashMap<String, String> exactAliases = new LinkedHashMap<>();
        LinkedHashMap<String, String> normalizedAliases = new LinkedHashMap<>();
        try (input; BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8)
        )) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                if (fields.length != 5) {
                    throw malformed(lineNumber, "expected five tab-separated fields");
                }
                String name = fields[0];
                EncodingEra era;
                try {
                    era = EncodingEra.valueOf(fields[1]);
                } catch (IllegalArgumentException exception) {
                    throw malformed(lineNumber, "unknown era " + fields[1], exception);
                }
                boolean multibyte;
                if (fields[2].equals("true")) {
                    multibyte = true;
                } else if (fields[2].equals("false")) {
                    multibyte = false;
                } else {
                    throw malformed(lineNumber, "invalid multibyte flag " + fields[2]);
                }
                List<String> languages = splitList(fields[3], ",");
                List<String> aliases = splitList(fields[4], "\u001f");
                Info info = new Info(name, aliases, era, multibyte, languages);
                if (byName.putIfAbsent(name, info) != null) {
                    throw malformed(lineNumber, "duplicate canonical name " + name);
                }
                entries.add(info);
                addAlias(exactAliases, normalizedAliases, name, name);
                for (String alias : aliases) {
                    addAlias(exactAliases, normalizedAliases, alias, name);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read encoding registry resource: " + RESOURCE, exception);
        }

        if (entries.size() != EXPECTED_ENTRY_COUNT) {
            throw new IllegalStateException(
                    "Malformed encoding registry resource: expected " + EXPECTED_ENTRY_COUNT
                            + " entries but found " + entries.size()
            );
        }
        LinkedHashSet<String> supported = new LinkedHashSet<>(byName.keySet());
        return new RegistryData(
                List.copyOf(entries),
                Collections.unmodifiableMap(byName),
                Collections.unmodifiableMap(exactAliases),
                Collections.unmodifiableMap(normalizedAliases),
                Collections.unmodifiableSet(supported)
        );
    }

    /// Splits one resource field into an immutable list.
    ///
    /// @param field     field contents
    /// @param delimiter literal delimiter
    /// @return immutable field values
    private static @Unmodifiable List<String> splitList(String field, String delimiter) {
        if (field.isEmpty()) {
            return List.of();
        }
        return List.of(field.split(java.util.regex.Pattern.quote(delimiter), -1));
    }

    /// Adds exact and normalized forms of one alias.
    ///
    /// Exact aliases preserve registry priority. Normalized aliases preserve
    /// the first mapping, matching ordered codec lookup for collisions.
    ///
    /// @param exactAliases      exact alias map
    /// @param normalizedAliases normalized alias map
    /// @param alias             alias to add
    /// @param canonicalName     canonical target name
    private static void addAlias(
            Map<String, String> exactAliases,
            Map<String, String> normalizedAliases,
            String alias,
            String canonicalName
    ) {
        exactAliases.putIfAbsent(alias.toLowerCase(Locale.ROOT), canonicalName);
        String normalized = normalizeCodecName(alias);
        if (!normalized.isEmpty()) {
            normalizedAliases.putIfAbsent(normalized, canonicalName);
        }
    }

    /// Normalizes a name like Python's codec registry.
    ///
    /// Runs of non-ASCII-alphanumeric characters other than `.` become a
    /// single underscore between retained groups; leading and trailing runs
    /// are discarded.
    ///
    /// @param name the supplied name
    /// @return the normalized lower-case lookup key
    private static String normalizeCodecName(String name) {
        StringBuilder result = new StringBuilder(name.length());
        boolean punctuation = false;
        for (int index = 0; index < name.length(); index++) {
            char value = name.charAt(index);
            boolean retained = value <= 0x7f
                    && ((value >= 'a' && value <= 'z')
                    || (value >= 'A' && value <= 'Z')
                    || (value >= '0' && value <= '9')
                    || value == '.');
            if (retained) {
                if (punctuation && !result.isEmpty()) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(value));
                punctuation = false;
            } else {
                punctuation = true;
            }
        }
        return result.toString();
    }

    /// Creates a malformed-registry exception.
    ///
    /// @param lineNumber one-based source line
    /// @param detail     error detail
    /// @return the exception
    private static IllegalStateException malformed(int lineNumber, String detail) {
        return new IllegalStateException(
                "Malformed encoding registry resource at line " + lineNumber + ": " + detail
        );
    }

    /// Creates a malformed-registry exception with a cause.
    ///
    /// @param lineNumber one-based source line
    /// @param detail     error detail
    /// @param cause      underlying parse failure
    /// @return the exception
    private static IllegalStateException malformed(
            int lineNumber,
            String detail,
            RuntimeException cause
    ) {
        return new IllegalStateException(
                "Malformed encoding registry resource at line " + lineNumber + ": " + detail,
                cause
        );
    }

    /// Stores metadata for one canonical encoding.
    ///
    /// @param name      canonical registry name
    /// @param aliases   immutable aliases
    /// @param era       assigned encoding era
    /// @param multibyte whether CJK structural gating applies
    /// @param languages immutable possible ISO 639 language codes
    @NotNullByDefault
    record Info(
            String name,
            @Unmodifiable List<String> aliases,
            EncodingEra era,
            boolean multibyte,
            @Unmodifiable List<String> languages
    ) {
        /// Creates immutable encoding metadata.
        Info {
            aliases = List.copyOf(aliases);
            languages = List.copyOf(languages);
        }
    }

    /// Holds all immutable indexes built from the registry resource.
    ///
    /// @param entries            ordered canonical entries
    /// @param byName             entries indexed by canonical name
    /// @param exactAliases       exact case-folded aliases
    /// @param normalizedAliases  codec-normalized aliases
    /// @param supportedEncodings ordered canonical names
    @NotNullByDefault
    private record RegistryData(
            @Unmodifiable List<Info> entries,
            @Unmodifiable Map<String, Info> byName,
            @Unmodifiable Map<String, String> exactAliases,
            @Unmodifiable Map<String, String> normalizedAliases,
            @Unmodifiable Set<String> supportedEncodings
    ) {
        /// Creates immutable registry indexes.
        private RegistryData {
            entries = List.copyOf(entries);
        }
    }
}
