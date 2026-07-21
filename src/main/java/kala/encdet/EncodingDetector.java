// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import kala.encdet.internal.ByteBufferSupport;
import kala.encdet.internal.DetectionEngine;
import kala.encdet.internal.EncodingRegistry;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.ByteBuffer;
import java.util.*;

/// Detects character encodings using immutable reusable configuration.
///
/// Instances are safe for concurrent use. Shared registry and model state is
/// immutable after lazy initialization, and every detection invocation uses a
/// separate context. Configuration methods never modify their receiver. They
/// return it when the requested value is already configured and otherwise
/// return an independently configured detector.
///
/// Candidate eligibility is the intersection of the configured encoding and
/// era sets. The resulting eligibility set also gates BOM, markup, escape, and
/// fallback results; binary classifications have no encoding and are not
/// filtered. If a configured fallback is ineligible, the detector returns a
/// no-encoding result instead.
///
/// Preferred-superset remapping, when enabled, occurs after candidate
/// filtering. A reported superset may therefore be absent from the configured
/// encoding set.
@NotNullByDefault
public final class EncodingDetector {
    /// Represents one ordered character-encoding detection target.
    ///
    /// Enum identity is the authoritative encoding representation used by the
    /// public API. Textual names are provided only for interchange, display, and
    /// alias resolution.
    ///
    /// A target is not necessarily an exact decoder identity. Alias lookup may
    /// intentionally fold related codec names into one detectable target; for
    /// example, `cp037` resolves to [#CP1140] and `big5` resolves to
    /// [#BIG5_HKSCS]. The original alias cannot be recovered from the enum value.
    ///
    /// @apiNote An encoding target is not guaranteed to be available from
    /// `java.nio.charset.Charset`.
    @NotNullByDefault
    public enum Encoding {
        /// Seven-bit US-ASCII.
        ASCII("ascii"),

        /// UTF-8 without a BOM-specific result identity.
        UTF_8("utf-8"),

        /// UTF-8 identified by its leading byte-order mark.
        UTF_8_SIG("utf-8-sig", "UTF-8-SIG"),

        /// BOM-selected UTF-16 with no fixed byte order in the identity.
        UTF_16("utf-16", "UTF-16"),

        /// Big-endian UTF-16 without a required byte-order mark.
        UTF_16_BE("utf-16-be"),

        /// Little-endian UTF-16 without a required byte-order mark.
        UTF_16_LE("utf-16-le"),

        /// BOM-selected UTF-32 with no fixed byte order in the identity.
        UTF_32("utf-32", "UTF-32"),

        /// Big-endian UTF-32 without a required byte-order mark.
        UTF_32_BE("utf-32-be"),

        /// Little-endian UTF-32 without a required byte-order mark.
        UTF_32_LE("utf-32-le"),

        /// The stateful seven-bit UTF-7 encoding.
        UTF_7("utf-7"),

        /// The Big5-HKSCS multibyte encoding for Traditional Chinese text.
        BIG5_HKSCS("big5hkscs", "Big5"),

        /// Microsoft's Japanese CP932 multibyte encoding.
        CP932("cp932"),

        /// Microsoft's Unified Hangul Code CP949 multibyte encoding.
        CP949("cp949", "CP949"),

        /// The EUC-JIS-2004 multibyte encoding for Japanese text.
        EUC_JIS_2004("euc_jis_2004", "EUC-JP"),

        /// The EUC-KR multibyte encoding for Korean text.
        EUC_KR("euc_kr", "EUC-KR"),

        /// The GB 18030 multibyte encoding for Chinese text.
        GB18030("gb18030", "GB18030"),

        /// The stateful HZ-GB-2312 encoding for Chinese text.
        HZ("hz", "HZ-GB-2312"),

        /// The ISO-2022-JP-2 stateful encoding for Japanese text.
        ISO_2022_JP_2("iso2022_jp_2", "ISO-2022-JP"),

        /// The ISO-2022-JP-2004 stateful encoding for Japanese text.
        ISO_2022_JP_2004("iso2022_jp_2004"),

        /// The extended ISO-2022-JP stateful encoding for Japanese text.
        ISO_2022_JP_EXT("iso2022_jp_ext"),

        /// The ISO-2022-KR stateful encoding for Korean text.
        ISO_2022_KR("iso2022_kr", "ISO-2022-KR"),

        /// The Shift_JIS-2004 multibyte encoding for Japanese text.
        SHIFT_JIS_2004("shift_jis_2004", "SHIFT_JIS"),

        /// Microsoft's single-byte Windows encoding for Thai text.
        CP874("cp874"),

        /// Microsoft's single-byte Windows encoding for Central European text.
        CP1250("cp1250"),

        /// Microsoft's single-byte Windows encoding for Cyrillic text.
        CP1251("cp1251", "Windows-1251"),

        /// Microsoft's single-byte Windows encoding for Western European text.
        CP1252("cp1252", "Windows-1252"),

        /// Microsoft's single-byte Windows encoding for Greek text.
        CP1253("cp1253", "Windows-1253"),

        /// Microsoft's single-byte Windows encoding for Turkish text.
        CP1254("cp1254", "Windows-1254"),

        /// Microsoft's single-byte Windows encoding for Hebrew text.
        CP1255("cp1255", "Windows-1255"),

        /// Microsoft's single-byte Windows encoding for Arabic and Persian text.
        CP1256("cp1256"),

        /// Microsoft's single-byte Windows encoding for Baltic text.
        CP1257("cp1257"),

        /// Microsoft's single-byte Windows encoding for Vietnamese text.
        CP1258("cp1258"),

        /// The single-byte KOI8-R encoding for Russian text.
        KOI8_R("koi8-r", "KOI8-R"),

        /// The single-byte KOI8-U encoding for Ukrainian text.
        KOI8_U("koi8-u"),

        /// The single-byte TIS-620 encoding for Thai text.
        TIS_620("tis-620", "TIS-620"),

        /// ISO 8859-1, the single-byte Latin-1 encoding for Western European text.
        ISO_8859_1("iso8859-1", "ISO-8859-1"),

        /// ISO 8859-2, the single-byte Latin-2 encoding for Central European text.
        ISO_8859_2("iso8859-2"),

        /// ISO 8859-3, the single-byte Latin-3 encoding for South European text.
        ISO_8859_3("iso8859-3"),

        /// ISO 8859-4, the single-byte Latin-4 encoding for North European text.
        ISO_8859_4("iso8859-4"),

        /// ISO 8859-5, the single-byte encoding for Cyrillic text.
        ISO_8859_5("iso8859-5", "ISO-8859-5"),

        /// ISO 8859-6, the single-byte encoding for Arabic text.
        ISO_8859_6("iso8859-6"),

        /// ISO 8859-7, the single-byte encoding for Greek text.
        ISO_8859_7("iso8859-7", "ISO-8859-7"),

        /// ISO 8859-8, the single-byte encoding for Hebrew text.
        ISO_8859_8("iso8859-8", "ISO-8859-8"),

        /// ISO 8859-9, the single-byte Latin-5 encoding for Turkish text.
        ISO_8859_9("iso8859-9", "ISO-8859-9"),

        /// ISO 8859-10, the single-byte Latin-6 encoding for Nordic text.
        ISO_8859_10("iso8859-10"),

        /// ISO 8859-13, the single-byte Latin-7 encoding for Baltic text.
        ISO_8859_13("iso8859-13"),

        /// ISO 8859-14, the single-byte Latin-8 encoding for Celtic text.
        ISO_8859_14("iso8859-14"),

        /// ISO 8859-15, the single-byte Latin-9 encoding with the euro sign.
        ISO_8859_15("iso8859-15"),

        /// ISO 8859-16, the single-byte Latin-10 encoding for South-Eastern European text.
        ISO_8859_16("iso8859-16"),

        /// The Johab multibyte encoding for Korean text.
        JOHAB("johab", "Johab"),

        /// The classic Mac OS single-byte encoding for Cyrillic text.
        MAC_CYRILLIC("mac-cyrillic", "MacCyrillic"),

        /// The classic Mac OS single-byte encoding for Greek text.
        MAC_GREEK("mac-greek", "MacGreek"),

        /// The classic Mac OS single-byte encoding for Icelandic text.
        MAC_ICELAND("mac-iceland", "MacIceland"),

        /// The classic Mac OS single-byte encoding for Central European text.
        MAC_LATIN2("mac-latin2", "MacLatin2"),

        /// The classic Mac OS single-byte Roman encoding for Western text.
        MAC_ROMAN("mac-roman", "MacRoman"),

        /// The classic Mac OS single-byte encoding for Turkish text.
        MAC_TURKISH("mac-turkish", "MacTurkish"),

        /// The DOS CP720 single-byte encoding for Arabic text.
        CP720("cp720"),

        /// The single-byte CP1006 encoding for Urdu text.
        CP1006("cp1006"),

        /// The DOS CP1125 single-byte encoding for Ukrainian text.
        CP1125("cp1125"),

        /// The single-byte KOI8-T encoding for Tajik text.
        KOI8_T("koi8-t"),

        /// The single-byte KZ-1048 encoding for Kazakh text.
        KZ1048("kz1048", "KZ1048"),

        /// The single-byte PTCP154 encoding for Kazakh text.
        PTCP154("ptcp154"),

        /// Hewlett-Packard's single-byte Roman8 encoding for Western text.
        HP_ROMAN8("hp-roman8"),

        /// The original IBM PC OEM single-byte code page for United States text.
        CP437("cp437"),

        /// The DOS CP737 single-byte encoding for Greek text.
        CP737("cp737"),

        /// The DOS CP775 single-byte encoding for Baltic text.
        CP775("cp775"),

        /// The DOS CP850 single-byte encoding for Western European text.
        CP850("cp850"),

        /// The DOS CP852 single-byte encoding for Central European text.
        CP852("cp852"),

        /// The DOS CP855 single-byte encoding for Cyrillic text.
        CP855("cp855", "IBM855"),

        /// The DOS CP856 single-byte encoding for Hebrew text.
        CP856("cp856"),

        /// The DOS CP857 single-byte encoding for Turkish text.
        CP857("cp857"),

        /// The euro-enabled DOS CP858 variant of CP850.
        CP858("cp858"),

        /// The DOS CP860 single-byte encoding for Portuguese text.
        CP860("cp860"),

        /// The DOS CP861 single-byte encoding for Icelandic text.
        CP861("cp861"),

        /// The DOS CP862 single-byte encoding for Hebrew text.
        CP862("cp862"),

        /// The DOS CP863 single-byte encoding for Canadian French text.
        CP863("cp863"),

        /// The DOS CP864 single-byte encoding for Arabic text.
        CP864("cp864"),

        /// The DOS CP865 single-byte encoding for Nordic text.
        CP865("cp865"),

        /// The DOS CP866 single-byte encoding for Cyrillic text.
        CP866("cp866", "IBM866"),

        /// The DOS CP869 single-byte encoding for Greek text.
        CP869("cp869"),

        /// The euro-enabled EBCDIC CP1140 encoding derived from code page 37.
        CP1140("cp1140"),

        /// The EBCDIC CP424 single-byte encoding for Hebrew text.
        CP424("cp424"),

        /// The EBCDIC CP500 single-byte encoding for international Latin text.
        CP500("cp500"),

        /// The EBCDIC CP875 single-byte encoding for Greek text.
        CP875("cp875"),

        /// The EBCDIC CP1026 single-byte encoding for Turkish text.
        CP1026("cp1026"),

        /// The EBCDIC CP273 single-byte encoding for German text.
        CP273("cp273");

        /// Canonical registry name.
        private final String canonicalName;

        /// Chardet-compatible display name.
        private final String displayName;

        /// Creates an encoding whose display and canonical names are identical.
        ///
        /// @param canonicalName canonical registry name
        Encoding(String canonicalName) {
            this(canonicalName, canonicalName);
        }

        /// Creates an encoding with distinct canonical and display names.
        ///
        /// @param canonicalName canonical registry name
        /// @param displayName   chardet-compatible display name
        Encoding(String canonicalName, String displayName) {
            this.canonicalName = canonicalName;
            this.displayName = displayName;
        }

        /// Returns the stable canonical name of this detection target.
        ///
        /// @return canonical registry name used for interchange and alias lookup
        public String canonicalName() {
            return canonicalName;
        }

        /// Returns the chardet-compatible display name.
        ///
        /// @return name used by the command-line interface
        public String displayName() {
            return displayName;
        }
    }

    /// Identifies a historical or operational group of character encodings.
    ///
    /// Multiple eras are selected by placing values in a set. The default
    /// detection options select every value.
    @NotNullByDefault
    public enum Era {
        /// Encodings commonly used by contemporary software and web content.
        MODERN_WEB,

        /// Legacy ISO-family encodings and closely related standards.
        LEGACY_ISO,

        /// Legacy encodings used by classic Macintosh systems.
        LEGACY_MAC,

        /// Other legacy encodings associated with a region or language.
        LEGACY_REGIONAL,

        /// Encodings historically used by DOS-compatible systems.
        DOS,

        /// Encodings historically used by mainframe systems.
        MAINFRAME
    }

    /// Describes one character-encoding detection candidate.
    ///
    /// @param encoding   the detected encoding, or `null` when the input is
    ///                 classified as binary or no permitted fallback exists
    /// @param confidence the confidence in the range `[0.0, 1.0]`
    /// @param language   the ISO 639 language code, or `null` when undetermined
    /// @param mimeType   the detected or inferred MIME type, or `null` only for a
    ///                 result created directly by an application
    /// @apiNote An [Encoding] value does not imply that the corresponding encoding
    /// is available through `java.nio.charset.Charset`.
    @NotNullByDefault
    public record Result(
            @Nullable Encoding encoding,
            double confidence,
            @Nullable String language,
            @Nullable String mimeType
    ) {
        /// Creates a detection result after validating its confidence value.
        ///
        /// @throws IllegalArgumentException if `confidence` is not finite or is
        /// outside `[0.0, 1.0]`
        public Result {
            if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
                throw new IllegalArgumentException("confidence must be finite and in the range [0.0, 1.0]");
            }
        }
    }

    /// Default minimum confidence used by [#detectAll(byte[])] and
    /// [#detectAll(ByteBuffer)].
    public static final double DEFAULT_MINIMUM_CONFIDENCE = 0.20;

    /// Immutable set containing every encoding target.
    private static final @Unmodifiable Set<Encoding> ALL_ENCODINGS =
            Collections.unmodifiableSet(EnumSet.allOf(Encoding.class));

    /// Default detector with every encoding era enabled.
    ///
    /// It examines at most 200,000 bytes, retains candidates with confidence
    /// of at least `0.20`, disables preferred-superset remapping, allows every
    /// encoding target, uses [Encoding#CP1252] when no candidate survives, and
    /// uses [Encoding#UTF_8] for empty input.
    public static final EncodingDetector DEFAULT = new EncodingDetector(
            Collections.unmodifiableSet(EnumSet.allOf(Era.class)),
            200_000,
            DEFAULT_MINIMUM_CONFIDENCE,
            false,
            ALL_ENCODINGS,
            Encoding.CP1252,
            Encoding.UTF_8
    );

    /// Eligible encoding eras in enum declaration order.
    private final @Unmodifiable Set<Era> encodingEras;

    /// Maximum number of leading input bytes examined.
    private final int maxBytes;

    /// Inclusive lower confidence bound applied to filtered candidate lists.
    private final double minimumConfidence;

    /// Whether subset encodings are remapped to preferred supersets.
    private final boolean preferSuperset;

    /// Encoding targets permitted to participate in detection.
    private final @Unmodifiable Set<Encoding> encodings;

    /// Low-confidence fallback used when no candidate survives.
    private final Encoding noMatchEncoding;

    /// Low-confidence fallback used for empty input.
    private final Encoding emptyInputEncoding;

    /// Creates a detector from validated immutable configuration state.
    ///
    /// @param encodingEras       immutable eligible encoding eras
    /// @param maxBytes           maximum leading input bytes examined
    /// @param minimumConfidence  inclusive filtered-candidate confidence bound
    /// @param preferSuperset     whether to remap subset encodings
    /// @param encodings          immutable permitted encoding targets
    /// @param noMatchEncoding    no-match fallback
    /// @param emptyInputEncoding empty-input fallback
    private EncodingDetector(
            @Unmodifiable Set<Era> encodingEras,
            int maxBytes,
            double minimumConfidence,
            boolean preferSuperset,
            @Unmodifiable Set<Encoding> encodings,
            Encoding noMatchEncoding,
            Encoding emptyInputEncoding
    ) {
        this.encodingEras = encodingEras;
        this.maxBytes = maxBytes;
        this.minimumConfidence = minimumConfidence;
        this.preferSuperset = preferSuperset;
        this.encodings = encodings;
        this.noMatchEncoding = noMatchEncoding;
        this.emptyInputEncoding = emptyInputEncoding;
    }

    /// Returns the eligible encoding eras.
    ///
    /// @return an immutable set in enum declaration order
    public @Unmodifiable Set<Era> encodingEras() {
        return encodingEras;
    }

    /// Returns the maximum number of leading bytes examined.
    ///
    /// @return a positive byte count
    public int maxBytes() {
        return maxBytes;
    }

    /// Returns the inclusive lower confidence bound used to filter candidates.
    ///
    /// @return a finite value in `[0.0, 1.0]`
    public double minimumConfidence() {
        return minimumConfidence;
    }

    /// Reports whether subset encodings are remapped to preferred supersets.
    ///
    /// @return whether preferred-superset remapping is enabled
    public boolean preferSuperset() {
        return preferSuperset;
    }

    /// Returns the encoding targets permitted to participate in detection.
    ///
    /// An encoding is eligible only when this set contains it and
    /// [#encodingEras()] contains its era. An empty set permits no text encoding
    /// or configured fallback, but does not suppress binary classifications.
    /// Preferred-superset remapping may produce a result absent from this set.
    ///
    /// @return immutable permitted encodings in enum declaration order
    public @Unmodifiable Set<Encoding> encodings() {
        return encodings;
    }

    /// Returns the no-match fallback.
    ///
    /// @return fallback encoding
    public Encoding noMatchEncoding() {
        return noMatchEncoding;
    }

    /// Returns the empty-input fallback.
    ///
    /// @return fallback encoding
    public Encoding emptyInputEncoding() {
        return emptyInputEncoding;
    }

    /// Returns a detector restricted to the supplied encoding eras.
    ///
    /// @param value a nonempty era set
    /// @return this detector if unchanged; otherwise a new detector
    /// @throws NullPointerException     if `value` or an element is `null`
    /// @throws IllegalArgumentException if `value` is empty
    public EncodingDetector withEncodingEras(Set<Era> value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be empty");
        }
        if (encodingEras.equals(value)) {
            return this;
        }
        return new EncodingDetector(
                immutableEras(value),
                maxBytes,
                minimumConfidence,
                preferSuperset,
                encodings,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector restricted to one encoding era.
    ///
    /// @param value the sole eligible era
    /// @return this detector if unchanged; otherwise a new detector
    /// @throws NullPointerException if `value` is `null`
    public EncodingDetector withEncodingEra(Era value) {
        Objects.requireNonNull(value, "value");
        if (encodingEras.size() == 1 && encodingEras.contains(value)) {
            return this;
        }
        return new EncodingDetector(
                Collections.unmodifiableSet(EnumSet.of(value)),
                maxBytes,
                minimumConfidence,
                preferSuperset,
                encodings,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector with a new maximum input length.
    ///
    /// @param value a positive byte count
    /// @return this detector if unchanged; otherwise a new detector
    /// @throws IllegalArgumentException if `value` is not positive
    public EncodingDetector withMaxBytes(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("value must be a positive integer");
        }
        if (maxBytes == value) {
            return this;
        }
        return new EncodingDetector(
                encodingEras,
                value,
                minimumConfidence,
                preferSuperset,
                encodings,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector with a new filtered-candidate confidence threshold.
    ///
    /// [#detectAll(byte[])] and [#detectAll(ByteBuffer)] retain candidates
    /// whose confidence is greater than or equal to this value. If none
    /// qualify, they return the unfiltered candidates.
    ///
    /// @param value a finite value in `[0.0, 1.0]`
    /// @return this detector if unchanged; otherwise a new detector
    /// @throws IllegalArgumentException if `value` is not finite or outside
    /// `[0.0, 1.0]`
    public EncodingDetector withMinimumConfidence(double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(
                    "value must be a finite value between 0.0 and 1.0"
            );
        }
        if (minimumConfidence == value) {
            return this;
        }
        return new EncodingDetector(
                encodingEras,
                maxBytes,
                value,
                preferSuperset,
                encodings,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector with preferred-superset remapping enabled or disabled.
    ///
    /// @param value whether to remap subset encodings
    /// @return this detector if unchanged; otherwise a new detector
    public EncodingDetector withPreferredSuperset(boolean value) {
        if (preferSuperset == value) {
            return this;
        }
        return new EncodingDetector(
                encodingEras,
                maxBytes,
                minimumConfidence,
                value,
                encodings,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector using the supplied set of permitted encoding targets.
    ///
    /// The supplied set is applied together with [#encodingEras()]. An empty set
    /// permits no text encoding or configured fallback, but does not suppress
    /// binary classifications.
    ///
    /// @param value permitted encodings; may be empty
    /// @return this detector if unchanged; otherwise a new detector
    /// @throws NullPointerException if `value` or an element is `null`
    public EncodingDetector withEncodings(Set<Encoding> value) {
        if (encodings.equals(value)) {
            return this;
        }
        return new EncodingDetector(
                encodingEras,
                maxBytes,
                minimumConfidence,
                preferSuperset,
                immutableEncodings(value),
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector using the supplied no-match fallback.
    ///
    /// @param value fallback encoding
    /// @return this detector if unchanged; otherwise a new detector
    /// @throws NullPointerException     if `value` is `null`
    public EncodingDetector withNoMatchEncoding(Encoding value) {
        Objects.requireNonNull(value, "value");
        if (noMatchEncoding == value) {
            return this;
        }
        return new EncodingDetector(
                encodingEras,
                maxBytes,
                minimumConfidence,
                preferSuperset,
                encodings,
                value,
                emptyInputEncoding
        );
    }

    /// Returns a detector using the supplied empty-input fallback.
    ///
    /// @param value fallback encoding
    /// @return this detector if unchanged; otherwise a new detector
    /// @throws NullPointerException     if `value` is `null`
    public EncodingDetector withEmptyInputEncoding(Encoding value) {
        Objects.requireNonNull(value, "value");
        if (emptyInputEncoding == value) {
            return this;
        }
        return new EncodingDetector(
                encodingEras,
                maxBytes,
                minimumConfidence,
                preferSuperset,
                encodings,
                noMatchEncoding,
                value
        );
    }

    /// Returns the highest-ranked result.
    ///
    /// Only the first [#maxBytes()] bytes are examined. The input array is read
    /// directly without copying, is not retained after this method returns, and
    /// is never modified. Its contents must not be changed during detection.
    ///
    /// @param input bytes to examine
    /// @return highest-ranked result
    /// @throws NullPointerException if `input` is `null`
    public Result detect(byte[] input) {
        return detectNormalized(ByteBufferSupport.wrap(input));
    }

    /// Returns the highest-ranked result for the remaining buffer bytes.
    ///
    /// Only the first [#maxBytes()] bytes between the buffer's position and
    /// limit are examined. The buffer's content, position, limit, and mark are
    /// not modified, and the buffer is not retained. Its underlying bytes are
    /// read directly without copying and must not change during detection.
    ///
    /// @param input buffer whose remaining bytes are examined
    /// @return highest-ranked result
    /// @throws NullPointerException if `input` is `null`
    public Result detect(ByteBuffer input) {
        return detectNormalized(ByteBufferSupport.view(input));
    }

    /// Returns candidates above the configured confidence threshold.
    ///
    /// If no candidate has confidence greater than or equal to
    /// [#minimumConfidence()], the unfiltered candidates are returned. The
    /// result is sorted stably by descending confidence and cannot be modified.
    /// Only the first [#maxBytes()] input bytes are examined. The input array is
    /// read directly without copying, is not retained, and must not change
    /// during detection.
    ///
    /// @param input bytes to examine
    /// @return immutable filtered candidates
    /// @throws NullPointerException if `input` is `null`
    public @Unmodifiable List<Result> detectAll(byte[] input) {
        List<Result> all = detectAllUnfiltered(input);
        List<Result> filtered = all.stream()
                .filter(result -> result.confidence() >= minimumConfidence)
                .toList();
        return filtered.isEmpty() ? all : filtered;
    }

    /// Returns candidates above the configured confidence threshold for a buffer.
    ///
    /// Only the first [#maxBytes()] remaining bytes are examined. The buffer's
    /// content, position, limit, and mark are not modified, and its underlying
    /// bytes are read directly without copying. If no candidate has confidence
    /// greater than or equal to [#minimumConfidence()], the unfiltered
    /// candidates are returned. The result is sorted stably by descending
    /// confidence and cannot be modified. The underlying bytes must not change
    /// during detection.
    ///
    /// @param input buffer whose remaining bytes are examined
    /// @return immutable filtered candidates
    /// @throws NullPointerException if `input` is `null`
    public @Unmodifiable List<Result> detectAll(ByteBuffer input) {
        List<Result> all = detectAllUnfiltered(input);
        List<Result> filtered = all.stream()
                .filter(result -> result.confidence() >= minimumConfidence)
                .toList();
        return filtered.isEmpty() ? all : filtered;
    }

    /// Returns every detection candidate.
    ///
    /// The result is sorted stably by descending confidence and cannot be
    /// modified. Only the first [#maxBytes()] bytes are examined, and the
    /// input array is read directly without copying or modification, is not
    /// retained, and must not change during detection.
    ///
    /// @param input bytes to examine
    /// @return immutable unfiltered candidates
    /// @throws NullPointerException if `input` is `null`
    public @Unmodifiable List<Result> detectAllUnfiltered(byte[] input) {
        return detectAllUnfilteredNormalized(
                ByteBufferSupport.wrap(input)
        );
    }

    /// Returns every detection candidate for the remaining buffer bytes.
    ///
    /// The result is sorted stably by descending confidence and cannot be
    /// modified. Only the first [#maxBytes()] bytes between the buffer's
    /// position and limit are examined. The buffer's content, position, limit,
    /// and mark are not modified, and the buffer is not retained. Its
    /// underlying bytes are read directly without copying and must not change
    /// during detection.
    ///
    /// @param input buffer whose remaining bytes are examined
    /// @return immutable unfiltered candidates
    /// @throws NullPointerException if `input` is `null`
    public @Unmodifiable List<Result> detectAllUnfiltered(ByteBuffer input) {
        return detectAllUnfilteredNormalized(ByteBufferSupport.view(input));
    }

    /// Returns the highest-ranked result for a normalized zero-copy buffer view.
    ///
    /// @param input bytes to examine
    /// @return highest-ranked result
    private Result detectNormalized(@UnmodifiableView ByteBuffer input) {
        return DetectionEngine.detect(input, this).get(0);
    }

    /// Returns every candidate for a normalized zero-copy buffer view.
    ///
    /// @param input bytes to examine
    /// @return immutable unfiltered candidates
    private @Unmodifiable List<Result> detectAllUnfilteredNormalized(
            @UnmodifiableView ByteBuffer input
    ) {
        return DetectionEngine.detect(input, this).stream()
                .sorted(Comparator.comparingDouble(Result::confidence).reversed())
                .toList();
    }

    /// Resolves an encoding from a canonical name or alias.
    ///
    /// Resolution is case-insensitive and does not consult installed Java
    /// charset providers.
    ///
    /// @param name name to resolve
    /// @return resolved encoding, or `null` if unknown
    /// @throws NullPointerException if `name` is `null`
    public static @Nullable Encoding lookupEncoding(String name) {
        return EncodingRegistry.lookup(name);
    }

    /// Returns all supported encodings in registry order.
    ///
    /// @return an immutable ordered set
    public static @Unmodifiable Set<Encoding> supportedEncodings() {
        return EncodingRegistry.supportedEncodings();
    }

    /// Copies an era set while preserving enum declaration order.
    ///
    /// @param eras eras to copy
    /// @return an immutable set
    private static @Unmodifiable Set<Era> immutableEras(Set<Era> eras) {
        EnumSet<Era> copy = EnumSet.copyOf(eras);
        return Collections.unmodifiableSet(copy);
    }

    /// Copies an encoding set while preserving enum declaration order.
    ///
    /// @param encodings supplied encodings
    /// @return immutable encoding set
    private static @Unmodifiable Set<Encoding> immutableEncodings(Set<Encoding> encodings) {
        EnumSet<Encoding> copy = EnumSet.noneOf(Encoding.class);
        copy.addAll(encodings);
        return Collections.unmodifiableSet(copy);
    }
}
