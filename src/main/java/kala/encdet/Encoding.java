// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import org.jetbrains.annotations.NotNullByDefault;

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
