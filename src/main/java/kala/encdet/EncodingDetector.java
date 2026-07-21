// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import kala.encdet.internal.ByteBufferSupport;
import kala.encdet.internal.DetectionEngine;
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
/// Candidate eligibility is defined by the configured encoding set. Era-based
/// configuration methods replace that set with the encodings classified in the
/// requested eras. The set also gates BOM, markup, escape, and fallback results;
/// binary classifications have no encoding and are not filtered. If a
/// configured fallback is ineligible, the detector returns a no-encoding result
/// instead.
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
    /// alias resolution. Each constant also owns its fixed era, multibyte
    /// classification, language associations, and ordered aliases.
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
        ASCII(
                "ascii", Era.MODERN_WEB, false,
                List.of(),
                "us-ascii", "646", "ansi_x3.4_1968", "ansi_x3.4_1986", "ansi_x3_4_1968", "cp367", "csascii",
                "ibm367", "iso646_us", "iso_646.irv_1991", "iso_ir_6", "us", "us_ascii"
        ),

        /// UTF-8 without a BOM-specific result identity.
        UTF_8(
                "utf-8", Era.MODERN_WEB, false,
                List.of(),
                "utf-8", "utf8", "csutf8", "unicode-1-1-utf-8", "unicode11utf8", "unicode20utf8",
                "x-unicode20utf8", "cp65001", "u8", "utf", "utf8_ucs2", "utf8_ucs4", "utf_8"
        ),

        /// UTF-8 identified by its leading byte-order mark.
        UTF_8_SIG(
                "utf-8-sig", "UTF-8-SIG", Era.MODERN_WEB, false,
                List.of(),
                "UTF-8-SIG", "utf-8-bom"
        ),

        /// BOM-selected UTF-16 with no fixed byte order in the identity.
        UTF_16(
                "utf-16", "UTF-16", Era.MODERN_WEB, false,
                List.of(),
                "UTF-16", "utf16", "csutf16", "u16", "utf_16"
        ),

        /// Big-endian UTF-16 without a required byte-order mark.
        UTF_16_BE(
                "utf-16-be", Era.MODERN_WEB, false,
                List.of(),
                "UTF-16-BE", "utf-16be", "csutf16be", "unicodebigunmarked", "utf_16_be", "utf_16be"
        ),

        /// Little-endian UTF-16 without a required byte-order mark.
        UTF_16_LE(
                "utf-16-le", Era.MODERN_WEB, false,
                List.of(),
                "UTF-16-LE", "utf-16le", "csutf16le", "unicodelittleunmarked", "utf_16_le", "utf_16le"
        ),

        /// BOM-selected UTF-32 with no fixed byte order in the identity.
        UTF_32(
                "utf-32", "UTF-32", Era.MODERN_WEB, false,
                List.of(),
                "UTF-32", "utf32", "csutf32", "u32", "utf_32"
        ),

        /// Big-endian UTF-32 without a required byte-order mark.
        UTF_32_BE(
                "utf-32-be", Era.MODERN_WEB, false,
                List.of(),
                "UTF-32-BE", "utf-32be", "csutf32be", "utf_32_be", "utf_32be"
        ),

        /// Little-endian UTF-32 without a required byte-order mark.
        UTF_32_LE(
                "utf-32-le", Era.MODERN_WEB, false,
                List.of(),
                "UTF-32-LE", "utf-32le", "csutf32le", "utf_32_le", "utf_32le"
        ),

        /// The stateful seven-bit UTF-7 encoding.
        UTF_7(
                "utf-7", Era.LEGACY_REGIONAL, false,
                List.of(),
                "UTF-7", "utf7", "csutf7", "u7", "unicode_1_1_utf_7", "utf_7"
        ),

        /// The Big5-HKSCS multibyte encoding for Traditional Chinese text.
        BIG5_HKSCS(
                "big5hkscs", "Big5", Era.MODERN_WEB, true,
                List.of("zh"),
                "Big5-HKSCS", "Big5HKSCS", "big5", "big5-tw", "csbig5", "cp950", "cn-big5", "x-x-big5",
                "csbig5hkscs", "950", "big5_hkscs", "big5_tw", "hkscs", "ms950", "x_mac_trad_chinese"
        ),

        /// Microsoft's Japanese CP932 multibyte encoding.
        CP932(
                "cp932", Era.MODERN_WEB, true,
                List.of("ja"),
                "CP932", "ms932", "mskanji", "ms-kanji", "cswindows31j", "windows-31j", "932", "windows_31j"
        ),

        /// Microsoft's Unified Hangul Code CP949 multibyte encoding.
        CP949(
                "cp949", "CP949", Era.MODERN_WEB, true,
                List.of("ko"),
                "CP949", "ms949", "uhc", "windows-949", "csksc56011987", "iso-ir-149", "ks_c_5601-1987",
                "ks_c_5601-1989", "ksc5601", "ksc_5601", "949"
        ),

        /// The EUC-JIS-2004 multibyte encoding for Japanese text.
        EUC_JIS_2004(
                "euc_jis_2004", "EUC-JP", Era.MODERN_WEB, true,
                List.of("ja"),
                "EUC-JIS-2004", "euc-jp", "eucjp", "ujis", "u-jis", "euc-jisx0213", "cseucpkdfmtjapanese",
                "x-euc-jp", "euc_jis2004", "eucjis2004", "jisx0213"
        ),

        /// The EUC-KR multibyte encoding for Korean text.
        EUC_KR(
                "euc_kr", "EUC-KR", Era.MODERN_WEB, true,
                List.of("ko"),
                "EUC-KR", "euckr", "cseuckr", "korean", "ks_c_5601", "ks_c_5601_1987", "ks_x_1001", "ksx1001",
                "x_mac_korean"
        ),

        /// The GB 18030 multibyte encoding for Chinese text.
        GB18030(
                "gb18030", "GB18030", Era.MODERN_WEB, true,
                List.of("zh"),
                "GB18030", "gb-18030", "gb2312", "gbk", "csgb2312", "gb_2312", "gb_2312-80", "x-gbk",
                "csiso58gb231280", "iso-ir-58", "csgb18030", "csgbk", "cp936", "ms936", "windows-936", "936",
                "chinese", "euc_cn", "euccn", "eucgb2312_cn", "gb18030_2000", "gb2312_1980", "gb2312_80",
                "iso_ir_58", "x_mac_simp_chinese"
        ),

        /// The stateful HZ-GB-2312 encoding for Chinese text.
        HZ(
                "hz", "HZ-GB-2312", Era.LEGACY_REGIONAL, true,
                List.of("zh"),
                "HZ-GB-2312", "hz", "hz_gb", "hz_gb_2312", "hzgb"
        ),

        /// The ISO-2022-JP-2 stateful encoding for Japanese text.
        ISO_2022_JP_2(
                "iso2022_jp_2", "ISO-2022-JP", Era.MODERN_WEB, true,
                List.of("ja"),
                "ISO-2022-JP-2", "iso-2022-jp", "csiso2022jp", "iso2022-jp-1", "csiso2022jp2", "iso2022jp_2",
                "iso_2022_jp_2"
        ),

        /// The ISO-2022-JP-2004 stateful encoding for Japanese text.
        ISO_2022_JP_2004(
                "iso2022_jp_2004", Era.MODERN_WEB, true,
                List.of("ja"),
                "ISO-2022-JP-2004", "iso2022-jp-3", "iso2022jp_2004", "iso_2022_jp_2004"
        ),

        /// The extended ISO-2022-JP stateful encoding for Japanese text.
        ISO_2022_JP_EXT(
                "iso2022_jp_ext", Era.MODERN_WEB, true,
                List.of("ja"),
                "ISO-2022-JP-EXT", "iso2022jp_ext", "iso_2022_jp_ext"
        ),

        /// The ISO-2022-KR stateful encoding for Korean text.
        ISO_2022_KR(
                "iso2022_kr", "ISO-2022-KR", Era.LEGACY_REGIONAL, true,
                List.of("ko"),
                "ISO-2022-KR", "csiso2022kr", "iso2022kr", "iso_2022_kr"
        ),

        /// The Shift_JIS-2004 multibyte encoding for Japanese text.
        SHIFT_JIS_2004(
                "shift_jis_2004", "SHIFT_JIS", Era.MODERN_WEB, true,
                List.of("ja"),
                "Shift-JIS-2004", "Shift_JIS_2004", "shift_jis", "sjis", "shiftjis", "s_jis", "shift-jisx0213",
                "x-sjis", "csshiftjis", "ms_kanji", "s_jis_2004", "shiftjis2004", "sjis_2004", "x_mac_japanese"
        ),

        /// Microsoft's single-byte Windows encoding for Thai text.
        CP874(
                "cp874", Era.MODERN_WEB, false,
                List.of("th"),
                "CP874", "windows-874", "dos-874", "874", "ms874", "windows_874"
        ),

        /// Microsoft's single-byte Windows encoding for Central European text.
        CP1250(
                "cp1250", Era.MODERN_WEB, false,
                List.of("pl", "cs", "hu", "hr", "ro", "sk", "sl", "sr"),
                "Windows-1250", "cp1250", "x-cp1250", "cswindows1250", "1250", "windows_1250"
        ),

        /// Microsoft's single-byte Windows encoding for Cyrillic text.
        CP1251(
                "cp1251", "Windows-1251", Era.MODERN_WEB, false,
                List.of("ru", "bg", "uk", "sr", "mk", "be"),
                "Windows-1251", "cp1251", "x-cp1251", "cswindows1251", "1251", "windows_1251"
        ),

        /// Microsoft's single-byte Windows encoding for Western European text.
        CP1252(
                "cp1252", "Windows-1252", Era.MODERN_WEB, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                "Windows-1252", "cp1252", "x-cp1252", "cswindows1252", "1252", "windows_1252"
        ),

        /// Microsoft's single-byte Windows encoding for Greek text.
        CP1253(
                "cp1253", "Windows-1253", Era.MODERN_WEB, false,
                List.of("el"),
                "Windows-1253", "cp1253", "x-cp1253", "cswindows1253", "1253", "windows_1253"
        ),

        /// Microsoft's single-byte Windows encoding for Turkish text.
        CP1254(
                "cp1254", "Windows-1254", Era.MODERN_WEB, false,
                List.of("tr"),
                "Windows-1254", "cp1254", "x-cp1254", "cswindows1254", "1254", "windows_1254"
        ),

        /// Microsoft's single-byte Windows encoding for Hebrew text.
        CP1255(
                "cp1255", "Windows-1255", Era.MODERN_WEB, false,
                List.of("he"),
                "Windows-1255", "cp1255", "x-cp1255", "cswindows1255", "1255", "windows_1255"
        ),

        /// Microsoft's single-byte Windows encoding for Arabic and Persian text.
        CP1256(
                "cp1256", Era.MODERN_WEB, false,
                List.of("ar", "fa"),
                "Windows-1256", "cp1256", "x-cp1256", "cswindows1256", "1256", "windows_1256"
        ),

        /// Microsoft's single-byte Windows encoding for Baltic text.
        CP1257(
                "cp1257", Era.MODERN_WEB, false,
                List.of("et", "lt", "lv"),
                "Windows-1257", "cp1257", "x-cp1257", "cswindows1257", "1257", "windows_1257"
        ),

        /// Microsoft's single-byte Windows encoding for Vietnamese text.
        CP1258(
                "cp1258", Era.MODERN_WEB, false,
                List.of("vi"),
                "Windows-1258", "cp1258", "x-cp1258", "cswindows1258", "1258", "windows_1258"
        ),

        /// The single-byte KOI8-R encoding for Russian text.
        KOI8_R(
                "koi8-r", "KOI8-R", Era.MODERN_WEB, false,
                List.of("ru"),
                "KOI8-R", "koi8r", "koi", "koi8", "cskoi8r", "koi8_r"
        ),

        /// The single-byte KOI8-U encoding for Ukrainian text.
        KOI8_U(
                "koi8-u", Era.MODERN_WEB, false,
                List.of("uk"),
                "KOI8-U", "koi8u", "koi8-ru", "cskoi8u"
        ),

        /// The single-byte TIS-620 encoding for Thai text.
        TIS_620(
                "tis-620", "TIS-620", Era.MODERN_WEB, false,
                List.of("th"),
                "TIS-620", "tis620", "iso-8859-11", "iso8859-11", "iso885911", "cstis620", "iso8859_11",
                "iso_8859_11", "iso_8859_11_2001", "iso_ir_166", "thai", "tis_620", "tis_620_0",
                "tis_620_2529_0", "tis_620_2529_1"
        ),

        /// ISO 8859-1, the single-byte Latin-1 encoding for Western European text.
        ISO_8859_1(
                "iso8859-1", "ISO-8859-1", Era.LEGACY_ISO, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                "ISO-8859-1", "latin-1", "latin1", "iso8859-1", "iso88591", "8859", "cp819", "csisolatin1",
                "ibm819", "iso8859", "iso8859_1", "iso_8859_1", "iso_8859_1_1987", "iso_ir_100", "l1", "latin",
                "latin_1"
        ),

        /// ISO 8859-2, the single-byte Latin-2 encoding for Central European text.
        ISO_8859_2(
                "iso8859-2", Era.LEGACY_ISO, false,
                List.of("pl", "cs", "hu", "hr", "ro", "sk", "sl"),
                "ISO-8859-2", "latin-2", "latin2", "iso8859-2", "iso88592", "csisolatin2", "iso8859_2",
                "iso_8859_2", "iso_8859_2_1987", "iso_ir_101", "l2"
        ),

        /// ISO 8859-3, the single-byte Latin-3 encoding for South European text.
        ISO_8859_3(
                "iso8859-3", Era.LEGACY_ISO, false,
                List.of("eo", "mt", "tr"),
                "ISO-8859-3", "latin-3", "latin3", "iso8859-3", "iso88593", "csisolatin3", "iso8859_3",
                "iso_8859_3", "iso_8859_3_1988", "iso_ir_109", "l3"
        ),

        /// ISO 8859-4, the single-byte Latin-4 encoding for North European text.
        ISO_8859_4(
                "iso8859-4", Era.LEGACY_ISO, false,
                List.of("et", "lt", "lv"),
                "ISO-8859-4", "latin-4", "latin4", "iso8859-4", "iso88594", "csisolatin4", "iso8859_4",
                "iso_8859_4", "iso_8859_4_1988", "iso_ir_110", "l4"
        ),

        /// ISO 8859-5, the single-byte encoding for Cyrillic text.
        ISO_8859_5(
                "iso8859-5", "ISO-8859-5", Era.LEGACY_ISO, false,
                List.of("ru", "bg", "uk", "sr", "mk", "be"),
                "ISO-8859-5", "iso8859-5", "cyrillic", "iso88595", "csisolatincyrillic", "iso8859_5",
                "iso_8859_5", "iso_8859_5_1988", "iso_ir_144"
        ),

        /// ISO 8859-6, the single-byte encoding for Arabic text.
        ISO_8859_6(
                "iso8859-6", Era.LEGACY_ISO, false,
                List.of("ar", "fa"),
                "ISO-8859-6", "iso8859-6", "arabic", "iso88596", "iso-8859-6-e", "iso-8859-6-i", "csiso88596e",
                "csiso88596i", "asmo_708", "csisolatinarabic", "ecma_114", "iso8859_6", "iso_8859_6",
                "iso_8859_6_1987", "iso_ir_127"
        ),

        /// ISO 8859-7, the single-byte encoding for Greek text.
        ISO_8859_7(
                "iso8859-7", "ISO-8859-7", Era.LEGACY_ISO, false,
                List.of("el"),
                "ISO-8859-7", "iso8859-7", "greek", "iso88597", "sun_eu_greek", "csisolatingreek", "ecma_118",
                "elot_928", "greek8", "iso8859_7", "iso_8859_7", "iso_8859_7_1987", "iso_ir_126"
        ),

        /// ISO 8859-8, the single-byte encoding for Hebrew text.
        ISO_8859_8(
                "iso8859-8", "ISO-8859-8", Era.LEGACY_ISO, false,
                List.of("he"),
                "ISO-8859-8", "iso8859-8", "hebrew", "iso88598", "iso-8859-8-e", "iso-8859-8-i", "csiso88598e",
                "csiso88598i", "visual", "logical", "csisolatinhebrew", "iso8859_8", "iso_8859_8",
                "iso_8859_8_1988", "iso_8859_8_e", "iso_8859_8_i", "iso_ir_138"
        ),

        /// ISO 8859-9, the single-byte Latin-5 encoding for Turkish text.
        ISO_8859_9(
                "iso8859-9", "ISO-8859-9", Era.LEGACY_ISO, false,
                List.of("tr"),
                "ISO-8859-9", "latin-5", "latin5", "iso8859-9", "iso88599", "csisolatin5", "iso8859_9",
                "iso_8859_9", "iso_8859_9_1989", "iso_ir_148", "l5"
        ),

        /// ISO 8859-10, the single-byte Latin-6 encoding for Nordic text.
        ISO_8859_10(
                "iso8859-10", Era.LEGACY_ISO, false,
                List.of("is", "fi"),
                "ISO-8859-10", "latin-6", "latin6", "iso8859-10", "iso885910", "csisolatin6", "iso8859_10",
                "iso_8859_10", "iso_8859_10_1992", "iso_ir_157", "l6"
        ),

        /// ISO 8859-13, the single-byte Latin-7 encoding for Baltic text.
        ISO_8859_13(
                "iso8859-13", Era.LEGACY_ISO, false,
                List.of("et", "lt", "lv"),
                "ISO-8859-13", "latin-7", "latin7", "iso8859-13", "iso885913", "csiso885913", "iso8859_13",
                "iso_8859_13", "l7"
        ),

        /// ISO 8859-14, the single-byte Latin-8 encoding for Celtic text.
        ISO_8859_14(
                "iso8859-14", Era.LEGACY_ISO, false,
                List.of("cy", "ga", "br", "gd"),
                "ISO-8859-14", "latin-8", "latin8", "iso8859-14", "iso885914", "csiso885914", "iso-ir-199",
                "iso-celtic", "l8", "iso8859_14", "iso_8859_14", "iso_8859_14_1998", "iso_celtic", "iso_ir_199"
        ),

        /// ISO 8859-15, the single-byte Latin-9 encoding with the euro sign.
        ISO_8859_15(
                "iso8859-15", Era.LEGACY_ISO, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                "ISO-8859-15", "latin-9", "latin9", "iso8859-15", "iso885915", "csisolatin9", "csiso885915",
                "l9", "iso8859_15", "iso_8859_15"
        ),

        /// ISO 8859-16, the single-byte Latin-10 encoding for South-Eastern European text.
        ISO_8859_16(
                "iso8859-16", Era.LEGACY_ISO, false,
                List.of("ro", "pl", "hr", "hu", "sk", "sl"),
                "ISO-8859-16", "latin-10", "latin10", "iso8859-16", "iso885916", "csiso885916", "iso-ir-226",
                "l10", "iso8859_16", "iso_8859_16", "iso_8859_16_2001", "iso_ir_226"
        ),

        /// The Johab multibyte encoding for Korean text.
        JOHAB(
                "johab", "Johab", Era.LEGACY_ISO, true,
                List.of("ko"),
                "Johab", "cp1361", "ms1361"
        ),

        /// The classic Mac OS single-byte encoding for Cyrillic text.
        MAC_CYRILLIC(
                "mac-cyrillic", "MacCyrillic", Era.LEGACY_MAC, false,
                List.of("ru", "bg", "uk", "sr", "mk", "be"),
                "Mac-Cyrillic", "MacCyrillic", "maccyrillic", "x-mac-cyrillic", "x-mac-ukrainian",
                "mac_cyrillic"
        ),

        /// The classic Mac OS single-byte encoding for Greek text.
        MAC_GREEK(
                "mac-greek", "MacGreek", Era.LEGACY_MAC, false,
                List.of("el"),
                "Mac-Greek", "MacGreek", "macgreek", "mac_greek"
        ),

        /// The classic Mac OS single-byte encoding for Icelandic text.
        MAC_ICELAND(
                "mac-iceland", "MacIceland", Era.LEGACY_MAC, false,
                List.of("is"),
                "Mac-Iceland", "MacIceland", "maciceland", "mac_iceland"
        ),

        /// The classic Mac OS single-byte encoding for Central European text.
        MAC_LATIN2(
                "mac-latin2", "MacLatin2", Era.LEGACY_MAC, false,
                List.of("pl", "cs", "hu", "hr", "sk", "sl"),
                "Mac-Latin2", "MacLatin2", "maclatin2", "maccentraleurope", "mac_centeuro", "mac_latin2"
        ),

        /// The classic Mac OS single-byte Roman encoding for Western text.
        MAC_ROMAN(
                "mac-roman", "MacRoman", Era.LEGACY_MAC, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                "Mac-Roman", "MacRoman", "macroman", "macintosh", "csmacintosh", "mac", "x-mac-roman",
                "mac_roman"
        ),

        /// The classic Mac OS single-byte encoding for Turkish text.
        MAC_TURKISH(
                "mac-turkish", "MacTurkish", Era.LEGACY_MAC, false,
                List.of("tr"),
                "Mac-Turkish", "MacTurkish", "macturkish", "mac_turkish"
        ),

        /// The DOS CP720 single-byte encoding for Arabic text.
        CP720(
                "cp720", Era.LEGACY_REGIONAL, false,
                List.of("ar", "fa"),
                "CP720"
        ),

        /// The single-byte CP1006 encoding for Urdu text.
        CP1006(
                "cp1006", Era.LEGACY_REGIONAL, false,
                List.of("ur"),
                "CP1006"
        ),

        /// The DOS CP1125 single-byte encoding for Ukrainian text.
        CP1125(
                "cp1125", Era.LEGACY_REGIONAL, false,
                List.of("uk"),
                "CP1125", "1125", "cp866u", "ibm1125", "ruscii"
        ),

        /// The single-byte KOI8-T encoding for Tajik text.
        KOI8_T(
                "koi8-t", Era.LEGACY_REGIONAL, false,
                List.of("tg"),
                "KOI8-T"
        ),

        /// The single-byte KZ-1048 encoding for Kazakh text.
        KZ1048(
                "kz1048", "KZ1048", Era.LEGACY_REGIONAL, false,
                List.of("kk"),
                "KZ-1048", "kz1048", "strk1048-2002", "rk1048", "kz_1048", "strk1048_2002"
        ),

        /// The single-byte PTCP154 encoding for Kazakh text.
        PTCP154(
                "ptcp154", Era.LEGACY_REGIONAL, false,
                List.of("kk"),
                "PTCP154", "pt154", "cp154", "csptcp154", "cyrillic_asian"
        ),

        /// Hewlett-Packard's single-byte Roman8 encoding for Western text.
        HP_ROMAN8(
                "hp-roman8", Era.LEGACY_REGIONAL, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                "HP-Roman8", "roman8", "r8", "csHPRoman8", "cp1051", "hp_roman8", "ibm1051"
        ),

        /// The original IBM PC OEM single-byte code page for United States text.
        CP437(
                "cp437", Era.DOS, false,
                List.of("en", "fr", "de", "es", "pt", "it", "nl", "da", "sv", "fi", "ga"),
                "CP437", "437", "cspc8codepage437", "ibm437"
        ),

        /// The DOS CP737 single-byte encoding for Greek text.
        CP737(
                "cp737", Era.DOS, false,
                List.of("el"),
                "CP737"
        ),

        /// The DOS CP775 single-byte encoding for Baltic text.
        CP775(
                "cp775", Era.DOS, false,
                List.of("et", "lt", "lv"),
                "CP775", "775", "cspc775baltic", "ibm775"
        ),

        /// The DOS CP850 single-byte encoding for Western European text.
        CP850(
                "cp850", Era.DOS, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                "CP850", "850", "cspc850multilingual", "ibm850"
        ),

        /// The DOS CP852 single-byte encoding for Central European text.
        CP852(
                "cp852", Era.DOS, false,
                List.of("pl", "cs", "hu", "hr", "ro", "sk", "sl"),
                "CP852", "852", "cspcp852", "ibm852"
        ),

        /// The DOS CP855 single-byte encoding for Cyrillic text.
        CP855(
                "cp855", "IBM855", Era.DOS, false,
                List.of("ru", "bg", "uk", "sr", "mk", "be"),
                "CP855", "855", "csibm855", "ibm855"
        ),

        /// The DOS CP856 single-byte encoding for Hebrew text.
        CP856(
                "cp856", Era.DOS, false,
                List.of("he"),
                "CP856"
        ),

        /// The DOS CP857 single-byte encoding for Turkish text.
        CP857(
                "cp857", Era.DOS, false,
                List.of("tr"),
                "CP857", "857", "csibm857", "ibm857"
        ),

        /// The euro-enabled DOS CP858 variant of CP850.
        CP858(
                "cp858", Era.DOS, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                "CP858", "858", "cp00858", "csibm00858", "csibm858", "ibm00858", "ibm858",
                "pc_multilingual_850_euro"
        ),

        /// The DOS CP860 single-byte encoding for Portuguese text.
        CP860(
                "cp860", Era.DOS, false,
                List.of("pt"),
                "CP860", "860", "csibm860", "ibm860"
        ),

        /// The DOS CP861 single-byte encoding for Icelandic text.
        CP861(
                "cp861", Era.DOS, false,
                List.of("is"),
                "CP861", "861", "cp_is", "csibm861", "ibm861"
        ),

        /// The DOS CP862 single-byte encoding for Hebrew text.
        CP862(
                "cp862", Era.DOS, false,
                List.of("he"),
                "CP862", "862", "cspc862latinhebrew", "ibm862"
        ),

        /// The DOS CP863 single-byte encoding for Canadian French text.
        CP863(
                "cp863", Era.DOS, false,
                List.of("fr"),
                "CP863", "863", "csibm863", "ibm863"
        ),

        /// The DOS CP864 single-byte encoding for Arabic text.
        CP864(
                "cp864", Era.DOS, false,
                List.of("ar"),
                "CP864", "864", "csibm864", "ibm864"
        ),

        /// The DOS CP865 single-byte encoding for Nordic text.
        CP865(
                "cp865", Era.DOS, false,
                List.of("da", "no"),
                "CP865", "865", "csibm865", "ibm865"
        ),

        /// The DOS CP866 single-byte encoding for Cyrillic text.
        CP866(
                "cp866", "IBM866", Era.DOS, false,
                List.of("ru", "bg", "uk", "sr", "mk", "be"),
                "CP866", "866", "csibm866", "ibm866"
        ),

        /// The DOS CP869 single-byte encoding for Greek text.
        CP869(
                "cp869", Era.DOS, false,
                List.of("el"),
                "CP869", "869", "cp_gr", "csibm869", "ibm869"
        ),

        /// The euro-enabled EBCDIC CP1140 encoding derived from code page 37.
        CP1140(
                "cp1140", Era.MAINFRAME, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv", "tr"),
                "CP1140", "cp037", "cp01140", "ibm01140", "ibm1140", "csibm01140", "037", "1140", "csibm037",
                "ebcdic_cp_ca", "ebcdic_cp_nl", "ebcdic_cp_us", "ebcdic_cp_wt", "ebcdic_us_37_euro", "ibm037",
                "ibm039"
        ),

        /// The EBCDIC CP424 single-byte encoding for Hebrew text.
        CP424(
                "cp424", Era.MAINFRAME, false,
                List.of("he"),
                "CP424", "424", "csibm424", "ebcdic_cp_he", "ibm424"
        ),

        /// The EBCDIC CP500 single-byte encoding for international Latin text.
        CP500(
                "cp500", Era.MAINFRAME, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                "CP500", "500", "csibm500", "ebcdic_cp_be", "ebcdic_cp_ch", "ibm500"
        ),

        /// The EBCDIC CP875 single-byte encoding for Greek text.
        CP875(
                "cp875", Era.MAINFRAME, false,
                List.of("el"),
                "CP875"
        ),

        /// The EBCDIC CP1026 single-byte encoding for Turkish text.
        CP1026(
                "cp1026", Era.MAINFRAME, false,
                List.of("tr"),
                "CP1026", "1026", "csibm1026", "ibm1026"
        ),

        /// The EBCDIC CP273 single-byte encoding for German text.
        CP273(
                "cp273", Era.MAINFRAME, false,
                List.of("de"),
                "CP273", "273", "csibm273", "ibm273"
        );

        /// Immutable set containing every encoding target.
        private static final @Unmodifiable Set<Encoding> ALL =
                Collections.unmodifiableSet(EnumSet.allOf(Encoding.class));

        /// Canonical registry name.
        private final String canonicalName;

        /// Chardet-compatible display name.
        private final String displayName;

        /// Historical or operational encoding group.
        private final Era era;

        /// Whether chardet applies its multibyte detection stages.
        private final boolean multibyte;

        /// Possible ISO 639 language codes.
        private final @Unmodifiable List<String> languages;

        /// Canonical, standards, and codec aliases in lookup order.
        private final @Unmodifiable List<String> aliases;

        /// Creates an encoding whose display and canonical names are identical.
        ///
        /// @param canonicalName canonical registry name
        /// @param era           historical or operational group
        /// @param multibyte     whether multibyte detection stages apply
        /// @param languages     possible ISO 639 language codes
        /// @param aliases       recognized aliases in lookup order
        Encoding(
                String canonicalName,
                Era era,
                boolean multibyte,
                @Unmodifiable List<String> languages,
                String... aliases
        ) {
            this(canonicalName, canonicalName, era, multibyte, languages, aliases);
        }

        /// Creates an encoding with distinct canonical and display names.
        ///
        /// @param canonicalName canonical registry name
        /// @param displayName   chardet-compatible display name
        /// @param era           historical or operational group
        /// @param multibyte     whether multibyte detection stages apply
        /// @param languages     possible ISO 639 language codes
        /// @param aliases       recognized aliases in lookup order
        Encoding(
                String canonicalName,
                String displayName,
                Era era,
                boolean multibyte,
                @Unmodifiable List<String> languages,
                String... aliases
        ) {
            this.canonicalName = canonicalName;
            this.displayName = displayName;
            this.era = era;
            this.multibyte = multibyte;
            this.languages = List.copyOf(languages);
            this.aliases = List.of(aliases);
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

        /// Returns the historical or operational group assigned to this target.
        ///
        /// @return era used by era-based encoding selection
        public Era era() {
            return era;
        }

        /// Reports whether chardet applies its multibyte detection stages.
        ///
        /// This classification controls CJK-oriented validity and structural
        /// scoring. It is not a general statement about how many bytes an
        /// encoded character may occupy; Unicode targets use separate handling.
        ///
        /// @return whether multibyte detection stages apply
        public boolean isMultibyte() {
            return multibyte;
        }

        /// Returns the possible languages associated with this target.
        ///
        /// @return immutable ISO 639 language codes in declared order
        public @Unmodifiable List<String> languages() {
            return languages;
        }

        /// Returns the names recognized as aliases of this target.
        ///
        /// The canonical name returned by [#canonicalName()] may also occur in
        /// this list when it was explicitly declared as an upstream alias.
        ///
        /// @return immutable aliases in lookup order
        public @Unmodifiable List<String> aliases() {
            return aliases;
        }

        /// Returns all supported encoding targets in enum declaration order.
        ///
        /// @return immutable ordered set containing every enum value
        public static @Unmodifiable Set<Encoding> all() {
            return ALL;
        }

        /// Resolves a canonical name or alias.
        ///
        /// Resolution is case-insensitive and does not consult installed Java
        /// charset providers.
        ///
        /// @param name name to resolve
        /// @return matching encoding, or `null` if unknown
        /// @throws NullPointerException if `name` is `null`
        public static @Nullable Encoding lookup(String name) {
            if (name.indexOf('\0') >= 0) {
                return null;
            }
            @Nullable Encoding exact = AliasIndex.EXACT_ALIASES.get(
                    name.toLowerCase(Locale.ROOT)
            );
            if (exact != null) {
                return exact;
            }
            return AliasIndex.NORMALIZED_ALIASES.get(AliasIndex.normalizeCodecName(name));
        }

        /// Holds alias indexes derived from the enum constants.
        @NotNullByDefault
        private static final class AliasIndex {
            /// Number of canonical targets in the pinned upstream registry.
            private static final int EXPECTED_ENCODING_COUNT = 86;

            /// Exact case-folded canonical names and aliases.
            private static final @Unmodifiable Map<String, Encoding> EXACT_ALIASES;

            /// Python-codec-normalized canonical names and aliases.
            private static final @Unmodifiable Map<String, Encoding> NORMALIZED_ALIASES;

            static {
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
                    addAlias(
                            exactAliases,
                            normalizedAliases,
                            encoding.canonicalName(),
                            encoding
                    );
                    for (String alias : encoding.aliases()) {
                        addAlias(exactAliases, normalizedAliases, alias, encoding);
                    }
                }
                EXACT_ALIASES = Collections.unmodifiableMap(exactAliases);
                NORMALIZED_ALIASES = Collections.unmodifiableMap(normalizedAliases);
            }

            /// Prevents instantiation of this static index.
            private AliasIndex() {
            }

            /// Adds exact and normalized forms of one alias.
            ///
            /// Exact aliases preserve enum declaration priority. Normalized aliases
            /// preserve the first mapping, matching ordered codec lookup for collisions.
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
        }
    }

    /// Classifies a character encoding by its historical or operational group.
    ///
    /// Each [Encoding] has exactly one era. The era-selection methods on
    /// [EncodingDetector] expand their arguments into the effective encoding
    /// set.
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

    /// Describes one candidate classification produced by encoding detection.
    ///
    /// @param encoding   the detected encoding, or `null` when the input is
    ///                 classified as binary or no permitted fallback exists
    /// @param confidence the confidence in the range `[0.0, 1.0]`
    /// @param language   the ISO 639 language code, or `null` when undetermined
    /// @param mimeType   the detected or inferred MIME type, or `null` only for a
    ///                 candidate created directly by an application
    /// @apiNote An [Encoding] value does not imply that the corresponding encoding
    /// is available through `java.nio.charset.Charset`.
    @NotNullByDefault
    public record Candidate(
            @Nullable Encoding encoding,
            double confidence,
            @Nullable String language,
            @Nullable String mimeType
    ) {
        /// Creates a candidate after validating its confidence value.
        ///
        /// @throws IllegalArgumentException if `confidence` is not finite or is
        /// outside `[0.0, 1.0]`
        public Candidate {
            if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
                throw new IllegalArgumentException("confidence must be finite and in the range [0.0, 1.0]");
            }
        }
    }

    /// Contains the complete and likely candidate lists for one detection outcome.
    ///
    /// Both lists are immutable snapshots. The complete list is nonempty and
    /// sorted by descending confidence, and the likely list is a nonempty prefix
    /// of it. Results returned by [#detect(byte[])] and [#detect(ByteBuffer)] use
    /// stable ordering for equal confidences and select the likely prefix using
    /// the detector's [#minimumConfidence()] value. When no candidate reaches
    /// that threshold, the likely list contains every candidate.
    ///
    /// @param candidates       all candidates in descending-confidence order
    /// @param likelyCandidates a nonempty prefix of `candidates`
    @NotNullByDefault
    public record Result(
            @Unmodifiable List<Candidate> candidates,
            @Unmodifiable List<Candidate> likelyCandidates
    ) {
        /// Creates a result after copying and validating its candidate lists.
        ///
        /// @throws NullPointerException if either list or an element is `null`
        /// @throws IllegalArgumentException if either list is empty, the complete
        /// list is not in descending-confidence order, or the likely list is not
        /// a prefix of the complete list
        public Result {
            candidates = List.copyOf(candidates);
            likelyCandidates = List.copyOf(likelyCandidates);
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("candidates must not be empty");
            }
            if (likelyCandidates.isEmpty()) {
                throw new IllegalArgumentException("likelyCandidates must not be empty");
            }
            for (int index = 1; index < candidates.size(); index++) {
                if (candidates.get(index - 1).confidence()
                        < candidates.get(index).confidence()) {
                    throw new IllegalArgumentException(
                            "candidates must be in descending-confidence order"
                    );
                }
            }
            if (likelyCandidates.size() > candidates.size()
                    || !candidates.subList(0, likelyCandidates.size())
                    .equals(likelyCandidates)) {
                throw new IllegalArgumentException(
                        "likelyCandidates must be a prefix of candidates"
                );
            }
        }

        /// Returns the highest-ranked candidate.
        ///
        /// @return first candidate in the complete list
        public Candidate bestCandidate() {
            return candidates.get(0);
        }
    }

    /// Default inclusive confidence threshold used by
    /// [Result#likelyCandidates()].
    public static final double DEFAULT_MINIMUM_CONFIDENCE = 0.20;

    /// Default detector with every encoding target enabled.
    ///
    /// It examines at most 200,000 bytes, reports candidates with confidence of
    /// at least `0.20` as likely, disables preferred-superset remapping, allows
    /// every encoding target, uses [Encoding#CP1252] when no candidate survives,
    /// and uses [Encoding#UTF_8] for empty input.
    public static final EncodingDetector DEFAULT = new EncodingDetector(
            200_000,
            DEFAULT_MINIMUM_CONFIDENCE,
            false,
            EnumSet.allOf(Encoding.class),
            Encoding.CP1252,
            Encoding.UTF_8
    );

    /// Maximum number of leading input bytes examined.
    private final int maxBytes;

    /// Inclusive lower confidence bound applied to likely candidate lists.
    private final double minimumConfidence;

    /// Whether subset encodings are remapped to preferred supersets.
    private final boolean preferSuperset;

    /// Encoding targets permitted to participate in detection.
    private final @Unmodifiable EnumSet<Encoding> encodings;

    /// Low-confidence fallback used when no candidate survives.
    private final Encoding noMatchEncoding;

    /// Low-confidence fallback used for empty input.
    private final Encoding emptyInputEncoding;

    /// Creates a detector from validated immutable configuration state.
    ///
    /// @param maxBytes           maximum leading input bytes examined
    /// @param minimumConfidence  inclusive likely-candidate confidence bound
    /// @param preferSuperset     whether to remap subset encodings
    /// @param encodings          immutable permitted encoding targets
    /// @param noMatchEncoding    no-match fallback
    /// @param emptyInputEncoding empty-input fallback
    private EncodingDetector(
            int maxBytes,
            double minimumConfidence,
            boolean preferSuperset,
            @Unmodifiable EnumSet<Encoding> encodings,
            Encoding noMatchEncoding,
            Encoding emptyInputEncoding
    ) {
        this.maxBytes = maxBytes;
        this.minimumConfidence = minimumConfidence;
        this.preferSuperset = preferSuperset;
        this.encodings = encodings;
        this.noMatchEncoding = noMatchEncoding;
        this.emptyInputEncoding = emptyInputEncoding;
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
    /// This is the complete effective selection. Any encoding- or era-selection
    /// method replaces it rather than adding another filter. The returned view
    /// cannot be modified.
    ///
    /// An empty set permits no text encoding or configured fallback, but does
    /// not suppress binary classifications. Preferred-superset remapping may
    /// produce a result absent from this set.
    ///
    /// @return an immutable view of the permitted encodings in enum declaration
    /// order
    public @UnmodifiableView Set<Encoding> encodings() {
        //noinspection RedundantUnmodifiable
        return Collections.unmodifiableSet(encodings);
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

    /// Returns a detector permitting every encoding in any supplied era.
    ///
    /// This method has the same selection semantics as
    /// [#withEncodingEras(Collection)]. Argument order and duplicate eras have
    /// no effect. The argument array is read during this invocation and is not
    /// retained.
    ///
    /// @param value zero or more eras whose encodings are permitted
    /// @return this detector if its effective encoding set is unchanged;
    /// otherwise a new detector
    /// @throws NullPointerException if `value` or an element is `null`
    public EncodingDetector withEncodingEras(Era... value) {
        if (value.length == 0) {
            return withEncodingSet(EnumSet.noneOf(Encoding.class));
        }

        EnumSet<Era> eras = EnumSet.noneOf(Era.class);
        Collections.addAll(eras, value);

        EnumSet<Encoding> result = EnumSet.noneOf(Encoding.class);
        for (Encoding encoding : Encoding.values()) {
            if (eras.contains(encoding.era())) {
                result.add(encoding);
            }
        }
        return withEncodingSet(result);
    }

    /// Returns a detector permitting every encoding in any supplied era.
    ///
    /// This method replaces the same effective encoding set as
    /// [#withEncodings(Collection)]. Argument order and duplicate eras have no
    /// effect. An empty collection permits no text encoding. The collection is
    /// copied and is not retained.
    ///
    /// @param value eras whose encodings are permitted; may be empty
    /// @return this detector if its effective encoding set is unchanged;
    /// otherwise a new detector
    /// @throws NullPointerException if `value` or an element is `null`
    public EncodingDetector withEncodingEras(Collection<Era> value) {
        EnumSet<Era> eras = EnumSet.noneOf(Era.class);
        eras.addAll(value);

        EnumSet<Encoding> result = EnumSet.noneOf(Encoding.class);
        for (Encoding encoding : Encoding.values()) {
            if (eras.contains(encoding.era())) {
                result.add(encoding);
            }
        }
        return withEncodingSet(result);
    }

    /// Returns a detector permitting every encoding in one era.
    ///
    /// This method replaces the same effective encoding set as
    /// [#withEncodings(Collection)].
    ///
    /// @param value the sole eligible era
    /// @return this detector if its effective encoding set is unchanged;
    /// otherwise a new detector
    /// @throws NullPointerException if `value` is `null`
    public EncodingDetector withEncodingEra(Era value) {
        EnumSet<Encoding> result = EnumSet.noneOf(Encoding.class);
        for (Encoding encoding : Encoding.values()) {
            if (value.equals(encoding.era())) {
                result.add(encoding);
            }
        }
        return withEncodingSet(result);
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
                value,
                minimumConfidence,
                preferSuperset,
                encodings,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector with a new likely-candidate confidence threshold.
    ///
    /// [Result#likelyCandidates()] contains candidates whose confidence is
    /// greater than or equal to this value. If none qualify, it contains every
    /// candidate.
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
                maxBytes,
                minimumConfidence,
                value,
                encodings,
                noMatchEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector permitting exactly the supplied encoding targets.
    ///
    /// This method has the same selection semantics as
    /// [#withEncodings(Collection)]. Argument order and duplicate encodings have
    /// no effect. The argument array is read during this invocation and is not
    /// retained.
    ///
    /// @param value zero or more permitted encodings
    /// @return this detector if its effective encoding set is unchanged;
    /// otherwise a new detector
    /// @throws NullPointerException if `value` or an element is `null`
    public EncodingDetector withEncodings(Encoding... value) {
        EnumSet<Encoding> encodings = EnumSet.noneOf(Encoding.class);
        Collections.addAll(encodings, value);
        return withEncodingSet(encodings);
    }

    /// Returns a detector permitting exactly the supplied encoding targets.
    ///
    /// This method replaces the same effective encoding set as
    /// [#withEncodingEras(Collection)]. Argument order and duplicate encodings
    /// have no effect. An empty collection permits no text encoding or
    /// configured fallback, but does not suppress binary classifications. The
    /// collection is copied and is not retained.
    ///
    /// @param value permitted encodings; may be empty
    /// @return this detector if its effective encoding set is unchanged;
    /// otherwise a new detector
    /// @throws NullPointerException if `value` or an element is `null`
    public EncodingDetector withEncodings(Collection<Encoding> value) {
        EnumSet<Encoding> copy = EnumSet.noneOf(Encoding.class);
        copy.addAll(value);
        return withEncodingSet(copy);
    }

    /// Returns a detector using the supplied no-match fallback.
    ///
    /// @param value fallback encoding
    /// @return this detector if unchanged; otherwise a new detector
    /// @throws NullPointerException if `value` is `null`
    public EncodingDetector withNoMatchEncoding(Encoding value) {
        Objects.requireNonNull(value, "value");
        if (noMatchEncoding == value) {
            return this;
        }
        return new EncodingDetector(
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
    /// @throws NullPointerException if `value` is `null`
    public EncodingDetector withEmptyInputEncoding(Encoding value) {
        Objects.requireNonNull(value, "value");
        if (emptyInputEncoding == value) {
            return this;
        }
        return new EncodingDetector(
                maxBytes,
                minimumConfidence,
                preferSuperset,
                encodings,
                noMatchEncoding,
                value
        );
    }

    /// Detects the candidate classifications for an input array.
    ///
    /// Only the first [#maxBytes()] bytes are examined. The input array is read
    /// directly without copying, is not retained after this method returns, and
    /// is never modified. Its contents must not be changed during detection.
    ///
    /// @param input bytes to examine
    /// @return immutable result containing every candidate and the likely
    /// candidate prefix
    /// @throws NullPointerException if `input` is `null`
    public Result detect(byte[] input) {
        return detectNormalized(ByteBufferSupport.wrap(input));
    }

    /// Detects the candidate classifications for the remaining buffer bytes.
    ///
    /// Only the first [#maxBytes()] bytes between the buffer's position and
    /// limit are examined. The buffer's content, position, limit, and mark are
    /// not modified, and the buffer is not retained. Its underlying bytes are
    /// read directly without copying and must not change during detection.
    ///
    /// @param input buffer whose remaining bytes are examined
    /// @return immutable result containing every candidate and the likely
    /// candidate prefix
    /// @throws NullPointerException if `input` is `null`
    public Result detect(ByteBuffer input) {
        return detectNormalized(ByteBufferSupport.view(input));
    }

    /// Detects candidates for a normalized zero-copy buffer view.
    ///
    /// @param input bytes to examine
    /// @return immutable aggregate result
    private Result detectNormalized(@UnmodifiableView ByteBuffer input) {
        List<Candidate> candidates = DetectionEngine.detect(input, this).stream()
                .sorted(Comparator.comparingDouble(Candidate::confidence).reversed())
                .toList();
        int likelyCount = 0;
        while (likelyCount < candidates.size()
                && candidates.get(likelyCount).confidence() >= minimumConfidence) {
            likelyCount++;
        }
        List<Candidate> likelyCandidates = likelyCount == 0
                || likelyCount == candidates.size()
                ? candidates
                : candidates.subList(0, likelyCount);
        return new Result(candidates, likelyCandidates);
    }

    /// Returns a detector using an already validated independent encoding set.
    ///
    /// @param value internally immutable permitted encodings
    /// @return this detector if unchanged; otherwise a new detector
    private EncodingDetector withEncodingSet(@Unmodifiable EnumSet<Encoding> value) {
        if (encodings.equals(value)) {
            return this;
        }
        return new EncodingDetector(
                maxBytes,
                minimumConfidence,
                preferSuperset,
                value,
                noMatchEncoding,
                emptyInputEncoding
        );
    }
}
