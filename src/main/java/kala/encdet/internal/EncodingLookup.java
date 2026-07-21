// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.EncodingDetector.Encoding;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/// Resolves canonical encoding names and aliases without charset providers.
@NotNullByDefault
public final class EncodingLookup {
    /// Number of canonical targets in the pinned upstream registry.
    private static final int EXPECTED_ENCODING_COUNT = 86;

    /// Exact case-folded canonical names and aliases.
    private static final @Unmodifiable Map<String, Encoding> EXACT_ALIASES;

    /// Python-codec-normalized canonical names and aliases.
    private static final @Unmodifiable Map<String, Encoding> NORMALIZED_ALIASES;

    static {
        AliasData data = createAliases();
        EXACT_ALIASES = data.exactAliases();
        NORMALIZED_ALIASES = data.normalizedAliases();
    }

    /// Prevents instantiation of this static index.
    private EncodingLookup() {
    }

    /// Returns the encoding for a canonical name or alias.
    ///
    /// @param name name to resolve
    /// @return the encoding, or `null` if unknown
    /// @throws NullPointerException if `name` is `null`
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

    /// Builds and validates alias indexes from enum metadata.
    ///
    /// @return immutable alias indexes
    private static AliasData createAliases() {
        Encoding[] encodings = Encoding.values();
        if (encodings.length != EXPECTED_ENCODING_COUNT) {
            throw new IllegalStateException(
                    "Expected " + EXPECTED_ENCODING_COUNT + " Encoding values but found "
                            + encodings.length
            );
        }

        LinkedHashMap<String, Encoding> exactAliases = new LinkedHashMap<>();
        LinkedHashMap<String, Encoding> normalizedAliases = new LinkedHashMap<>();
        HashSet<String> canonicalNames = new HashSet<>(EXPECTED_ENCODING_COUNT);
        for (Encoding encoding : encodings) {
            if (!canonicalNames.add(encoding.canonicalName())) {
                throw new IllegalStateException(
                        "Duplicate Encoding canonical name: " + encoding.canonicalName()
                );
            }
            addAlias(exactAliases, normalizedAliases, encoding.canonicalName(), encoding);
            for (String alias : encoding.aliases()) {
                addAlias(exactAliases, normalizedAliases, alias, encoding);
            }
        }
        return new AliasData(
                Collections.unmodifiableMap(exactAliases),
                Collections.unmodifiableMap(normalizedAliases)
        );
    }

    /// Adds exact and normalized forms of one alias.
    ///
    /// Exact aliases preserve enum declaration priority. Normalized aliases
    /// preserve the first mapping, matching ordered codec lookup for collisions.
    ///
    /// @param exactAliases exact alias map
    /// @param normalizedAliases normalized alias map
    /// @param alias alias to add
    /// @param encoding encoding identity
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
    /// @param name supplied name
    /// @return normalized lower-case lookup key
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

    /// Holds immutable exact and normalized alias indexes.
    ///
    /// @param exactAliases exact case-folded aliases
    /// @param normalizedAliases codec-normalized aliases
    @NotNullByDefault
    private record AliasData(
            @Unmodifiable Map<String, Encoding> exactAliases,
            @Unmodifiable Map<String, Encoding> normalizedAliases
    ) {
        /// Creates immutable alias indexes.
        private AliasData {
        }
    }
}
