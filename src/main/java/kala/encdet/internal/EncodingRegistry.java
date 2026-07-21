// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.EncodingDetector;
import kala.encdet.EncodingDetector.Encoding;
import kala.encdet.EncodingDetector.Era;
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
import java.util.EnumMap;
import java.util.HashMap;
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

    /// Canonical entries indexed by encoding identity.
    private static final @Unmodifiable Map<Encoding, Info> BY_ENCODING;

    /// Exact case-folded canonical names and aliases.
    private static final @Unmodifiable Map<String, Encoding> EXACT_ALIASES;

    /// Python-codec-normalized canonical names and aliases.
    private static final @Unmodifiable Map<String, Encoding> NORMALIZED_ALIASES;

    /// Supported encodings in registry order.
    private static final @Unmodifiable Set<Encoding> SUPPORTED_ENCODINGS;

    static {
        RegistryData data = readRegistry();
        ENTRIES = data.entries();
        BY_ENCODING = data.byEncoding();
        EXACT_ALIASES = data.exactAliases();
        NORMALIZED_ALIASES = data.normalizedAliases();
        SUPPORTED_ENCODINGS = data.supportedEncodings();
    }

    /// Prevents instantiation of this static registry.
    private EncodingRegistry() {
    }

    /// Returns the encoding for a canonical name or alias.
    ///
    /// @param name the name to resolve
    /// @return the encoding, or `null` if unknown
    public static @Nullable Encoding lookup(String name) {
        if (name.indexOf('\0') >= 0) {
            return null;
        }
        @Nullable Encoding exact = EXACT_ALIASES.get(name.toLowerCase(Locale.ROOT));
        if (exact != null) {
            return exact;
        }
        return NORMALIZED_ALIASES.get(normalizeCodecName(name));
    }

    /// Returns the immutable encoding set in registry order.
    ///
    /// @return supported encodings
    public static @Unmodifiable Set<Encoding> supportedEncodings() {
        return SUPPORTED_ENCODINGS;
    }

    /// Returns the registry entry for an encoding.
    ///
    /// @param encoding encoding identity
    /// @return the matching entry, or `null` if unknown
    static @Nullable Info get(Encoding encoding) {
        return BY_ENCODING.get(encoding);
    }

    /// Returns candidates present in the configured encoding and era sets.
    ///
    /// @param detector immutable detector configuration
    /// @return immutable entries in registry order
    static @Unmodifiable List<Info> candidates(EncodingDetector detector) {
        Set<Era> eras = detector.encodingEras();
        Set<Encoding> encodings = detector.encodings();
        ArrayList<Info> result = new ArrayList<>(ENTRIES.size());
        for (Info entry : ENTRIES) {
            if (!eras.contains(entry.era())) {
                continue;
            }
            if (!encodings.contains(entry.encoding())) {
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
        EnumMap<Encoding, Info> byEncoding = new EnumMap<>(Encoding.class);
        LinkedHashMap<String, Encoding> exactAliases = new LinkedHashMap<>();
        LinkedHashMap<String, Encoding> normalizedAliases = new LinkedHashMap<>();
        HashMap<String, Encoding> unregisteredEnums = new HashMap<>();
        for (Encoding encoding : Encoding.values()) {
            @Nullable Encoding previous = unregisteredEnums.put(
                    encoding.canonicalName(),
                    encoding
            );
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate Encoding canonical name: " + encoding.canonicalName()
                );
            }
        }
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
                @Nullable Encoding encoding = unregisteredEnums.remove(name);
                if (encoding == null) {
                    throw malformed(lineNumber, "unknown or duplicate canonical name " + name);
                }
                Era era;
                try {
                    era = Era.valueOf(fields[1]);
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
                Info info = new Info(encoding, aliases, era, multibyte, languages);
                if (byEncoding.putIfAbsent(encoding, info) != null) {
                    throw malformed(lineNumber, "duplicate encoding " + name);
                }
                entries.add(info);
                addAlias(exactAliases, normalizedAliases, name, encoding);
                for (String alias : aliases) {
                    addAlias(exactAliases, normalizedAliases, alias, encoding);
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
        if (!unregisteredEnums.isEmpty()) {
            throw new IllegalStateException(
                    "Encoding enum values missing from registry resource: "
                            + unregisteredEnums.keySet()
            );
        }
        LinkedHashSet<Encoding> supported = new LinkedHashSet<>(entries.size());
        for (Info entry : entries) {
            supported.add(entry.encoding());
        }
        return new RegistryData(
                List.copyOf(entries),
                Collections.unmodifiableMap(byEncoding),
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
    /// @param encoding          encoding identity
    private static void addAlias(
            Map<String, Encoding> exactAliases,
            Map<String, Encoding> normalizedAliases,
            String alias,
            Encoding encoding
    ) {
        exactAliases.putIfAbsent(alias.toLowerCase(Locale.ROOT), encoding);
        String normalized = normalizeCodecName(alias);
        if (!normalized.isEmpty()) {
            normalizedAliases.putIfAbsent(normalized, encoding);
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
    /// @param encoding encoding identity
    /// @param aliases   immutable aliases
    /// @param era       assigned encoding era
    /// @param multibyte whether CJK structural gating applies
    /// @param languages immutable possible ISO 639 language codes
    @NotNullByDefault
    record Info(
            Encoding encoding,
            @Unmodifiable List<String> aliases,
            Era era,
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
    /// @param byEncoding         entries indexed by encoding identity
    /// @param exactAliases       exact case-folded aliases
    /// @param normalizedAliases  codec-normalized aliases
    /// @param supportedEncodings ordered encoding identities
    @NotNullByDefault
    private record RegistryData(
            @Unmodifiable List<Info> entries,
            @Unmodifiable Map<Encoding, Info> byEncoding,
            @Unmodifiable Map<String, Encoding> exactAliases,
            @Unmodifiable Map<String, Encoding> normalizedAliases,
            @Unmodifiable Set<Encoding> supportedEncodings
    ) {
        /// Creates immutable registry indexes.
        private RegistryData {
            entries = List.copyOf(entries);
        }
    }
}
