// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.EncodingDetector;
import kala.encdet.EncodingDetector.Encoding;
import kala.encdet.EncodingDetector.Era;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/// Provides the detector's provider-independent ordered encoding registry.
@NotNullByDefault
public final class EncodingRegistry {
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
        RegistryData data = createRegistry();
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

    /// Builds and validates registry indexes from the encoding enum.
    ///
    /// @return immutable registry data
    private static RegistryData createRegistry() {
        Encoding[] encodings = Encoding.values();
        if (encodings.length != EXPECTED_ENTRY_COUNT) {
            throw new IllegalStateException(
                    "Expected " + EXPECTED_ENTRY_COUNT + " Encoding values but found "
                            + encodings.length
            );
        }

        ArrayList<Info> entries = new ArrayList<>(EXPECTED_ENTRY_COUNT);
        EnumMap<Encoding, Info> byEncoding = new EnumMap<>(Encoding.class);
        LinkedHashMap<String, Encoding> exactAliases = new LinkedHashMap<>();
        LinkedHashMap<String, Encoding> normalizedAliases = new LinkedHashMap<>();
        LinkedHashSet<Encoding> supported = new LinkedHashSet<>(EXPECTED_ENTRY_COUNT);
        HashSet<String> canonicalNames = new HashSet<>(EXPECTED_ENTRY_COUNT);
        for (Encoding encoding : encodings) {
            if (!canonicalNames.add(encoding.canonicalName())) {
                throw new IllegalStateException(
                        "Duplicate Encoding canonical name: " + encoding.canonicalName()
                );
            }
            Info info = new Info(
                    encoding,
                    encoding.aliases(),
                    encoding.era(),
                    encoding.isMultibyte(),
                    encoding.languages()
            );
            if (byEncoding.put(encoding, info) != null) {
                throw new IllegalStateException("Duplicate Encoding value: " + encoding);
            }
            entries.add(info);
            supported.add(encoding);
            addAlias(exactAliases, normalizedAliases, encoding.canonicalName(), encoding);
            for (String alias : encoding.aliases()) {
                addAlias(exactAliases, normalizedAliases, alias, encoding);
            }
        }
        return new RegistryData(
                List.copyOf(entries),
                Collections.unmodifiableMap(byEncoding),
                Collections.unmodifiableMap(exactAliases),
                Collections.unmodifiableMap(normalizedAliases),
                Collections.unmodifiableSet(supported)
        );
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

    /// Holds all immutable indexes built from enum metadata.
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
