// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/// Configures one encoding-detection operation.
///
/// All collection components are immutable defensive copies. Encoding names
/// supplied for filters or fallbacks are resolved to canonical registry names
/// when the options are created.
///
/// @param encodingEras       the nonempty set of eras whose encodings are eligible
/// @param maxBytes           the maximum number of leading input bytes to examine
/// @param preferSuperset     whether subset encodings are renamed to preferred
///                       Windows or code-page supersets before name styling
/// @param nameStyle          the naming convention applied to public results
/// @param includeEncodings   the canonical encodings to include, or `null` to
///                         include every encoding selected by the eras
/// @param excludeEncodings   the canonical encodings to exclude, or `null` to
///                         exclude none
/// @param noMatchEncoding    the canonical low-confidence fallback used when no
///                        candidate survives
/// @param emptyInputEncoding the canonical low-confidence fallback used for
///                           empty input
@NotNullByDefault
public record DetectionOptions(
        @Unmodifiable Set<EncodingEra> encodingEras,
        int maxBytes,
        boolean preferSuperset,
        EncodingNameStyle nameStyle,
        @Nullable @Unmodifiable Set<String> includeEncodings,
        @Nullable @Unmodifiable Set<String> excludeEncodings,
        String noMatchEncoding,
        String emptyInputEncoding
) {
    /// The default options matching the reference detector.
    public static final DetectionOptions DEFAULT = new DetectionOptions(
            EnumSet.allOf(EncodingEra.class),
            200_000,
            false,
            EncodingNameStyle.CHARDET_COMPATIBLE,
            null,
            null,
            "cp1252",
            "utf-8"
    );

    /// Creates and validates detection options.
    ///
    /// @throws NullPointerException     if a required value, collection element,
    /// or encoding name is `null`
    /// @throws IllegalArgumentException if an era set or supplied encoding
    /// filter is empty, `maxBytes` is not positive, or an encoding is unknown
    public DetectionOptions {
        Objects.requireNonNull(encodingEras, "encodingEras");
        Objects.requireNonNull(nameStyle, "nameStyle");
        if (encodingEras.isEmpty()) {
            throw new IllegalArgumentException("encodingEras must not be empty");
        }
        if (maxBytes < 1) {
            throw new IllegalArgumentException("maxBytes must be a positive integer");
        }

        encodingEras = immutableEras(encodingEras);
        includeEncodings = normalizeEncodings(includeEncodings, "includeEncodings");
        excludeEncodings = normalizeEncodings(excludeEncodings, "excludeEncodings");
        noMatchEncoding = normalizeEncoding(noMatchEncoding, "noMatchEncoding");
        emptyInputEncoding = normalizeEncoding(emptyInputEncoding, "emptyInputEncoding");
    }

    /// Returns a builder initialized with the default option values.
    ///
    /// @return a new mutable builder
    public static Builder builder() {
        return new Builder();
    }

    /// Returns a builder initialized from this value.
    ///
    /// @return a new mutable builder
    public Builder toBuilder() {
        return new Builder(this);
    }

    /// Copies an era set while preserving enum declaration order.
    ///
    /// @param eras the eras to copy
    /// @return an immutable set
    private static @Unmodifiable Set<EncodingEra> immutableEras(Set<EncodingEra> eras) {
        EnumSet<EncodingEra> copy = EnumSet.copyOf(eras);
        return Collections.unmodifiableSet(copy);
    }

    /// Resolves one encoding name through the detector registry.
    ///
    /// @param name          the supplied name
    /// @param parameterName the parameter used in an error message
    /// @return the canonical registry name
    private static String normalizeEncoding(String name, String parameterName) {
        Objects.requireNonNull(name, parameterName);
        String canonical = EncodingDetector.lookupEncoding(name);
        if (canonical == null) {
            throw new IllegalArgumentException("Unknown encoding '" + name + "' in " + parameterName);
        }
        return canonical;
    }

    /// Resolves and copies an optional encoding-name filter.
    ///
    /// @param names         the supplied names, or `null`
    /// @param parameterName the parameter used in an error message
    /// @return an immutable canonical-name set, or `null`
    private static @Nullable @Unmodifiable Set<String> normalizeEncodings(
            @Nullable Set<String> names,
            String parameterName
    ) {
        if (names == null) {
            return null;
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException(
                    parameterName + " must not be empty; pass null to disable filtering"
            );
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>(names.size());
        for (String name : names) {
            normalized.add(normalizeEncoding(name, parameterName));
        }
        return Collections.unmodifiableSet(normalized);
    }

    /// Builds immutable detection options one field at a time.
    ///
    /// Builder instances are mutable and are not thread-safe.
    @NotNullByDefault
    public static final class Builder {
        /// Eras selected by the builder.
        private Set<EncodingEra> encodingEras = EnumSet.allOf(EncodingEra.class);

        /// Maximum input length selected by the builder.
        private int maxBytes = 200_000;

        /// Whether preferred supersets are requested.
        private boolean preferSuperset;

        /// Result-name style selected by the builder.
        private EncodingNameStyle nameStyle = EncodingNameStyle.CHARDET_COMPATIBLE;

        /// Optional include filter selected by the builder.
        private @Nullable Set<String> includeEncodings;

        /// Optional exclude filter selected by the builder.
        private @Nullable Set<String> excludeEncodings;

        /// No-match fallback selected by the builder.
        private String noMatchEncoding = "cp1252";

        /// Empty-input fallback selected by the builder.
        private String emptyInputEncoding = "utf-8";

        /// Creates a builder initialized with default values.
        public Builder() {
        }

        /// Creates a builder initialized from existing options.
        ///
        /// @param options the options to copy
        private Builder(DetectionOptions options) {
            this.encodingEras = EnumSet.copyOf(options.encodingEras);
            this.maxBytes = options.maxBytes;
            this.preferSuperset = options.preferSuperset;
            this.nameStyle = options.nameStyle;
            this.includeEncodings = copyNullableSet(options.includeEncodings);
            this.excludeEncodings = copyNullableSet(options.excludeEncodings);
            this.noMatchEncoding = options.noMatchEncoding;
            this.emptyInputEncoding = options.emptyInputEncoding;
        }

        /// Selects the eligible encoding eras.
        ///
        /// @param value a nonempty era set
        /// @return this builder
        /// @throws NullPointerException     if `value` or an element is `null`
        /// @throws IllegalArgumentException if `value` is empty
        public Builder encodingEras(Set<EncodingEra> value) {
            this.encodingEras = EnumSet.copyOf(Objects.requireNonNull(value, "value"));
            return this;
        }

        /// Selects one eligible encoding era.
        ///
        /// @param value the era to select
        /// @return this builder
        /// @throws NullPointerException if `value` is `null`
        public Builder encodingEra(EncodingEra value) {
            this.encodingEras = EnumSet.of(Objects.requireNonNull(value, "value"));
            return this;
        }

        /// Sets the maximum number of leading bytes examined.
        ///
        /// The value is validated by [#build()].
        ///
        /// @param value a byte count that must be positive when built
        /// @return this builder
        public Builder maxBytes(int value) {
            this.maxBytes = value;
            return this;
        }

        /// Enables or disables preferred-superset name remapping.
        ///
        /// @param value whether to remap subsets
        /// @return this builder
        public Builder preferSuperset(boolean value) {
            this.preferSuperset = value;
            return this;
        }

        /// Selects the public encoding-name style.
        ///
        /// @param value the requested style
        /// @return this builder
        /// @throws NullPointerException if `value` is `null`
        public Builder nameStyle(EncodingNameStyle value) {
            this.nameStyle = Objects.requireNonNull(value, "value");
            return this;
        }

        /// Sets the optional encoding inclusion filter.
        ///
        /// @param value the names to include, or `null` to disable the filter
        /// @return this builder
        public Builder includeEncodings(@Nullable Set<String> value) {
            this.includeEncodings = copyNullableSet(value);
            return this;
        }

        /// Sets the optional encoding exclusion filter.
        ///
        /// @param value the names to exclude, or `null` to disable the filter
        /// @return this builder
        public Builder excludeEncodings(@Nullable Set<String> value) {
            this.excludeEncodings = copyNullableSet(value);
            return this;
        }

        /// Sets the no-match fallback encoding.
        ///
        /// @param value an encoding name or alias
        /// @return this builder
        /// @throws NullPointerException if `value` is `null`
        public Builder noMatchEncoding(String value) {
            this.noMatchEncoding = Objects.requireNonNull(value, "value");
            return this;
        }

        /// Sets the empty-input fallback encoding.
        ///
        /// @param value an encoding name or alias
        /// @return this builder
        /// @throws NullPointerException if `value` is `null`
        public Builder emptyInputEncoding(String value) {
            this.emptyInputEncoding = Objects.requireNonNull(value, "value");
            return this;
        }

        /// Creates immutable validated options from the current values.
        ///
        /// @return the new options
        /// @throws NullPointerException     if a required value, collection
        /// element, or encoding name is `null`
        /// @throws IllegalArgumentException if the era set or a supplied
        /// encoding filter is empty, `maxBytes` is not positive, or an
        /// encoding name is unknown
        public DetectionOptions build() {
            return new DetectionOptions(
                    encodingEras,
                    maxBytes,
                    preferSuperset,
                    nameStyle,
                    includeEncodings,
                    excludeEncodings,
                    noMatchEncoding,
                    emptyInputEncoding
            );
        }

        /// Copies a nullable set for builder isolation.
        ///
        /// @param value the source set, or `null`
        /// @return a mutable copy, or `null`
        private static @Nullable Set<String> copyNullableSet(@Nullable Set<String> value) {
            return value == null ? null : new LinkedHashSet<>(value);
        }
    }
}
