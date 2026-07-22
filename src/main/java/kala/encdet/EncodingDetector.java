// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import kala.encdet.internal.ByteBufferSupport;
import kala.encdet.internal.DetectionEngine;
import kala.encdet.internal.EncodingReader;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/// Detects character encodings using immutable reusable configuration.
///
/// Instances are safe for concurrent use. Configuration methods never modify
/// their receiver. They return it when the requested value is already configured
/// and otherwise return an independently configured detector.
///
/// Candidate eligibility is defined by the configured encoding set. Era-based
/// configuration methods replace that set with the encodings classified in the
/// requested eras. The set also gates BOM, markup, escape, and configured
/// recommendations. Binary classifications have no encoding and are unaffected
/// by encoding-set selection. An absent or ineligible recommendation is
/// reported as `null`.
///
/// Preferred-superset remapping, when enabled, occurs after encoding-set
/// filtering. A reported superset may therefore be absent from the configured
/// encoding set.
@NotNullByDefault
public final class EncodingDetector {
    /// Optional preferred-superset remapping applied to detected encodings.
    private static final @Unmodifiable Map<Encoding, Encoding> PREFERRED_SUPERSETS = Map.ofEntries(
            Map.entry(Encoding.ASCII, Encoding.CP1252),
            Map.entry(Encoding.EUC_KR, Encoding.CP949),
            Map.entry(Encoding.ISO_8859_1, Encoding.CP1252),
            Map.entry(Encoding.ISO_8859_2, Encoding.CP1250),
            Map.entry(Encoding.ISO_8859_5, Encoding.CP1251),
            Map.entry(Encoding.ISO_8859_6, Encoding.CP1256),
            Map.entry(Encoding.ISO_8859_7, Encoding.CP1253),
            Map.entry(Encoding.ISO_8859_8, Encoding.CP1255),
            Map.entry(Encoding.ISO_8859_9, Encoding.CP1254),
            Map.entry(Encoding.ISO_8859_13, Encoding.CP1257),
            Map.entry(Encoding.TIS_620, Encoding.CP874)
    );

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
    /// ## Java charset support
    ///
    /// [Charset] availability depends on the runtime. Java SE requires the
    /// mappings identified on individual constants, while OpenJDK 17 supplies
    /// additional mappings through its standard charset providers. Installed
    /// [java.nio.charset.spi.CharsetProvider] implementations may add mappings,
    /// while custom runtime images may omit OpenJDK's extended providers.
    /// [#charset()] does not substitute a related charset with different
    /// character mappings. [#UTF_8_SIG] is the framing-only exception: its
    /// payload uses UTF-8, while its signature remains outside the returned
    /// charset. [#approximateCharset()] additionally permits predefined related
    /// mappings when an exact mapping is unavailable.
    ///
    /// @apiNote [#charset()] does not consume or emit external framing such as
    /// the signature associated with [#UTF_8_SIG].
    @NotNullByDefault
    public enum Encoding {
        /// Seven-bit US-ASCII.
        ///
        /// Every Java SE implementation must provide an exact [Charset] mapping
        /// for this encoding.
        ASCII(
                "ascii", Era.MODERN_WEB, false,
                List.of(),
                List.of(
                        "us-ascii", "646", "ansi_x3.4_1968", "ansi_x3.4_1986", "ansi_x3_4_1968", "cp367", "csascii",
                        "ibm367", "iso646_us", "iso_646.irv_1991", "iso_ir_6", "us", "us_ascii"
                )
        ),

        /// UTF-8 without a BOM-specific result identity.
        ///
        /// Every Java SE implementation must provide an exact [Charset] mapping
        /// for this encoding.
        UTF_8(
                "utf-8", Era.MODERN_WEB, false,
                List.of(),
                List.of(
                        "utf-8", "utf8", "csutf8", "unicode-1-1-utf-8", "unicode11utf8", "unicode20utf8",
                        "x-unicode20utf8", "cp65001", "u8", "utf", "utf8_ucs2", "utf8_ucs4", "utf_8"
                )
        ),

        /// UTF-8 text prefixed with the signature `EF BB BF`.
        ///
        /// [#charset()] returns [StandardCharsets#UTF_8], which maps the text
        /// after the signature. That charset does not implement UTF-8-SIG
        /// framing: its decoder exposes an initial signature as `U+FEFF`, and
        /// its encoder does not prepend one. Code processing the complete stream
        /// must consume or emit the signature separately.
        UTF_8_SIG(
                "utf-8-sig", "UTF-8-SIG", Era.MODERN_WEB, false,
                List.of(),
                List.of(
                        "UTF-8-SIG", "utf-8-bom"
                )
        ),

        /// BOM-selected UTF-16 with no fixed byte order in the identity.
        ///
        /// Every Java SE implementation must provide an exact [Charset] mapping
        /// for this encoding.
        UTF_16(
                "utf-16", "UTF-16", Era.MODERN_WEB, false,
                List.of(),
                List.of(
                        "UTF-16", "utf16", "csutf16", "u16", "utf_16"
                )
        ),

        /// Big-endian UTF-16 without a required byte-order mark.
        ///
        /// Every Java SE implementation must provide an exact [Charset] mapping
        /// for this encoding.
        UTF_16_BE(
                "utf-16-be", Era.MODERN_WEB, false,
                List.of(),
                List.of(
                        "UTF-16-BE", "utf-16be", "csutf16be", "unicodebigunmarked", "utf_16_be", "utf_16be"
                )
        ),

        /// Little-endian UTF-16 without a required byte-order mark.
        ///
        /// Every Java SE implementation must provide an exact [Charset] mapping
        /// for this encoding.
        UTF_16_LE(
                "utf-16-le", Era.MODERN_WEB, false,
                List.of(),
                List.of(
                        "UTF-16-LE", "utf-16le", "csutf16le", "unicodelittleunmarked", "utf_16_le", "utf_16le"
                )
        ),

        /// BOM-selected UTF-32 with no fixed byte order in the identity.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        UTF_32(
                "utf-32", "UTF-32", Era.MODERN_WEB, false,
                List.of(),
                List.of(
                        "UTF-32", "utf32", "csutf32", "u32", "utf_32"
                )
        ),

        /// Big-endian UTF-32 without a required byte-order mark.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        UTF_32_BE(
                "utf-32-be", Era.MODERN_WEB, false,
                List.of(),
                List.of(
                        "UTF-32-BE", "utf-32be", "csutf32be", "utf_32_be", "utf_32be"
                )
        ),

        /// Little-endian UTF-32 without a required byte-order mark.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        UTF_32_LE(
                "utf-32-le", Era.MODERN_WEB, false,
                List.of(),
                List.of(
                        "UTF-32-LE", "utf-32le", "csutf32le", "utf_32_le", "utf_32le"
                )
        ),

        /// The stateful seven-bit UTF-7 encoding.
        ///
        /// OpenJDK 17's standard charset providers do not include an exact
        /// [Charset] mapping for this encoding.
        UTF_7(
                "utf-7", Era.LEGACY_REGIONAL, false,
                List.of(),
                List.of(
                        "UTF-7", "utf7", "csutf7", "u7", "unicode_1_1_utf_7", "utf_7"
                )
        ),

        /// The Big5-HKSCS multibyte encoding for Traditional Chinese text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        BIG5_HKSCS(
                "big5hkscs", "Big5", Era.MODERN_WEB, true,
                List.of("zh"),
                List.of(
                        "Big5-HKSCS", "Big5HKSCS", "big5", "big5-tw", "csbig5", "cp950", "cn-big5", "x-x-big5",
                        "csbig5hkscs", "950", "big5_hkscs", "big5_tw", "hkscs", "ms950", "x_mac_trad_chinese"
                )
        ),

        /// Microsoft's Japanese CP932 multibyte encoding.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP932(
                "cp932", Era.MODERN_WEB, true,
                List.of("ja"),
                List.of(
                        "CP932", "ms932", "mskanji", "ms-kanji", "cswindows31j", "windows-31j", "932", "windows_31j"
                )
        ),

        /// Microsoft's Unified Hangul Code CP949 multibyte encoding.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP949(
                "cp949", "CP949", Era.MODERN_WEB, true,
                List.of("ko"),
                List.of(
                        "CP949", "ms949", "uhc", "windows-949", "csksc56011987", "iso-ir-149", "ks_c_5601-1987",
                        "ks_c_5601-1989", "ksc5601", "ksc_5601", "949"
                )
        ),

        /// The EUC-JIS-2004 multibyte encoding for Japanese text.
        ///
        /// OpenJDK 17's standard charset providers do not include an exact
        /// [Charset] mapping for this encoding.
        EUC_JIS_2004(
                "euc_jis_2004", "EUC-JP", Era.MODERN_WEB, true,
                List.of("ja"),
                List.of(
                        "EUC-JIS-2004", "euc-jp", "eucjp", "ujis", "u-jis", "euc-jisx0213", "cseucpkdfmtjapanese",
                        "x-euc-jp", "euc_jis2004", "eucjis2004", "jisx0213"
                )
        ),

        /// The EUC-KR multibyte encoding for Korean text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        EUC_KR(
                "euc_kr", "EUC-KR", Era.MODERN_WEB, true,
                List.of("ko"),
                List.of(
                        "EUC-KR", "euckr", "cseuckr", "korean", "ks_c_5601", "ks_c_5601_1987", "ks_x_1001", "ksx1001",
                        "x_mac_korean"
                )
        ),

        /// The GB 18030 multibyte encoding for Chinese text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        GB18030(
                "gb18030", "GB18030", Era.MODERN_WEB, true,
                List.of("zh"),
                List.of(
                        "GB18030", "gb-18030", "gb2312", "gbk", "csgb2312", "gb_2312", "gb_2312-80", "x-gbk",
                        "csiso58gb231280", "iso-ir-58", "csgb18030", "csgbk", "cp936", "ms936", "windows-936", "936",
                        "chinese", "euc_cn", "euccn", "eucgb2312_cn", "gb18030_2000", "gb2312_1980", "gb2312_80",
                        "iso_ir_58", "x_mac_simp_chinese"
                )
        ),

        /// The stateful HZ-GB-2312 encoding for Chinese text.
        ///
        /// OpenJDK 17's standard charset providers do not include an exact
        /// [Charset] mapping for this encoding.
        HZ(
                "hz", "HZ-GB-2312", Era.LEGACY_REGIONAL, true,
                List.of("zh"),
                List.of(
                        "HZ-GB-2312", "hz", "hz_gb", "hz_gb_2312", "hzgb"
                )
        ),

        /// The ISO-2022-JP-2 stateful encoding for Japanese text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        ISO_2022_JP_2(
                "iso2022_jp_2", "ISO-2022-JP", Era.MODERN_WEB, true,
                List.of("ja"),
                List.of(
                        "ISO-2022-JP-2", "iso-2022-jp", "csiso2022jp", "iso2022-jp-1", "csiso2022jp2", "iso2022jp_2",
                        "iso_2022_jp_2"
                )
        ),

        /// The ISO-2022-JP-2004 stateful encoding for Japanese text.
        ///
        /// OpenJDK 17's standard charset providers do not include an exact
        /// [Charset] mapping for this encoding.
        ISO_2022_JP_2004(
                "iso2022_jp_2004", Era.MODERN_WEB, true,
                List.of("ja"),
                List.of(
                        "ISO-2022-JP-2004", "iso2022-jp-3", "iso2022jp_2004", "iso_2022_jp_2004"
                )
        ),

        /// The extended ISO-2022-JP stateful encoding for Japanese text.
        ///
        /// OpenJDK 17's standard charset providers do not include an exact
        /// [Charset] mapping for this encoding.
        ISO_2022_JP_EXT(
                "iso2022_jp_ext", Era.MODERN_WEB, true,
                List.of("ja"),
                List.of(
                        "ISO-2022-JP-EXT", "iso2022jp_ext", "iso_2022_jp_ext"
                )
        ),

        /// The ISO-2022-KR stateful encoding for Korean text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        ISO_2022_KR(
                "iso2022_kr", "ISO-2022-KR", Era.LEGACY_REGIONAL, true,
                List.of("ko"),
                List.of(
                        "ISO-2022-KR", "csiso2022kr", "iso2022kr", "iso_2022_kr"
                )
        ),

        /// The Shift_JIS-2004 multibyte encoding for Japanese text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping under the name `x-SJIS_0213`.
        SHIFT_JIS_2004(
                "shift_jis_2004", "SHIFT_JIS", Era.MODERN_WEB, true,
                List.of("ja"),
                List.of(
                        "Shift-JIS-2004", "Shift_JIS_2004", "shift_jis", "sjis", "shiftjis", "s_jis", "shift-jisx0213",
                        "x-sjis", "csshiftjis", "ms_kanji", "s_jis_2004", "shiftjis2004", "sjis_2004", "x_mac_japanese"
                )
        ),

        /// Microsoft's single-byte Windows encoding for Thai text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP874(
                "cp874", Era.MODERN_WEB, false,
                List.of("th"),
                List.of(
                        "CP874", "windows-874", "dos-874", "874", "ms874", "windows_874"
                )
        ),

        /// Microsoft's single-byte Windows encoding for Central European text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP1250(
                "cp1250", Era.MODERN_WEB, false,
                List.of("pl", "cs", "hu", "hr", "ro", "sk", "sl", "sr"),
                List.of(
                        "Windows-1250", "cp1250", "x-cp1250", "cswindows1250", "1250", "windows_1250"
                )
        ),

        /// Microsoft's single-byte Windows encoding for Cyrillic text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP1251(
                "cp1251", "Windows-1251", Era.MODERN_WEB, false,
                List.of("ru", "bg", "uk", "sr", "mk", "be"),
                List.of(
                        "Windows-1251", "cp1251", "x-cp1251", "cswindows1251", "1251", "windows_1251"
                )
        ),

        /// Microsoft's single-byte Windows encoding for Western European text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP1252(
                "cp1252", "Windows-1252", Era.MODERN_WEB, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                List.of(
                        "Windows-1252", "cp1252", "x-cp1252", "cswindows1252", "1252", "windows_1252"
                )
        ),

        /// Microsoft's single-byte Windows encoding for Greek text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP1253(
                "cp1253", "Windows-1253", Era.MODERN_WEB, false,
                List.of("el"),
                List.of(
                        "Windows-1253", "cp1253", "x-cp1253", "cswindows1253", "1253", "windows_1253"
                )
        ),

        /// Microsoft's single-byte Windows encoding for Turkish text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP1254(
                "cp1254", "Windows-1254", Era.MODERN_WEB, false,
                List.of("tr"),
                List.of(
                        "Windows-1254", "cp1254", "x-cp1254", "cswindows1254", "1254", "windows_1254"
                )
        ),

        /// Microsoft's single-byte Windows encoding for Hebrew text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP1255(
                "cp1255", "Windows-1255", Era.MODERN_WEB, false,
                List.of("he"),
                List.of(
                        "Windows-1255", "cp1255", "x-cp1255", "cswindows1255", "1255", "windows_1255"
                )
        ),

        /// Microsoft's single-byte Windows encoding for Arabic and Persian text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP1256(
                "cp1256", Era.MODERN_WEB, false,
                List.of("ar", "fa"),
                List.of(
                        "Windows-1256", "cp1256", "x-cp1256", "cswindows1256", "1256", "windows_1256"
                )
        ),

        /// Microsoft's single-byte Windows encoding for Baltic text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP1257(
                "cp1257", Era.MODERN_WEB, false,
                List.of("et", "lt", "lv"),
                List.of(
                        "Windows-1257", "cp1257", "x-cp1257", "cswindows1257", "1257", "windows_1257"
                )
        ),

        /// Microsoft's single-byte Windows encoding for Vietnamese text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP1258(
                "cp1258", Era.MODERN_WEB, false,
                List.of("vi"),
                List.of(
                        "Windows-1258", "cp1258", "x-cp1258", "cswindows1258", "1258", "windows_1258"
                )
        ),

        /// The single-byte KOI8-R encoding for Russian text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        KOI8_R(
                "koi8-r", "KOI8-R", Era.MODERN_WEB, false,
                List.of("ru"),
                List.of(
                        "KOI8-R", "koi8r", "koi", "koi8", "cskoi8r", "koi8_r"
                )
        ),

        /// The single-byte KOI8-U encoding for Ukrainian text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        KOI8_U(
                "koi8-u", Era.MODERN_WEB, false,
                List.of("uk"),
                List.of(
                        "KOI8-U", "koi8u", "koi8-ru", "cskoi8u"
                )
        ),

        /// The single-byte TIS-620 encoding for Thai text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        TIS_620(
                "tis-620", "TIS-620", Era.MODERN_WEB, false,
                List.of("th"),
                List.of(
                        "TIS-620", "tis620", "iso-8859-11", "iso8859-11", "iso885911", "cstis620", "iso8859_11",
                        "iso_8859_11", "iso_8859_11_2001", "iso_ir_166", "thai", "tis_620", "tis_620_0",
                        "tis_620_2529_0", "tis_620_2529_1"
                )
        ),

        /// ISO 8859-1, the single-byte Latin-1 encoding for Western European text.
        ///
        /// Every Java SE implementation must provide an exact [Charset] mapping
        /// for this encoding.
        ISO_8859_1(
                "iso8859-1", "ISO-8859-1", Era.LEGACY_ISO, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                List.of(
                        "ISO-8859-1", "latin-1", "latin1", "iso8859-1", "iso88591", "8859", "cp819", "csisolatin1",
                        "ibm819", "iso8859", "iso8859_1", "iso_8859_1", "iso_8859_1_1987", "iso_ir_100", "l1", "latin",
                        "latin_1"
                )
        ),

        /// ISO 8859-2, the single-byte Latin-2 encoding for Central European text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        ISO_8859_2(
                "iso8859-2", Era.LEGACY_ISO, false,
                List.of("pl", "cs", "hu", "hr", "ro", "sk", "sl"),
                List.of(
                        "ISO-8859-2", "latin-2", "latin2", "iso8859-2", "iso88592", "csisolatin2", "iso8859_2",
                        "iso_8859_2", "iso_8859_2_1987", "iso_ir_101", "l2"
                )
        ),

        /// ISO 8859-3, the single-byte Latin-3 encoding for South European text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        ISO_8859_3(
                "iso8859-3", Era.LEGACY_ISO, false,
                List.of("eo", "mt", "tr"),
                List.of(
                        "ISO-8859-3", "latin-3", "latin3", "iso8859-3", "iso88593", "csisolatin3", "iso8859_3",
                        "iso_8859_3", "iso_8859_3_1988", "iso_ir_109", "l3"
                )
        ),

        /// ISO 8859-4, the single-byte Latin-4 encoding for North European text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        ISO_8859_4(
                "iso8859-4", Era.LEGACY_ISO, false,
                List.of("et", "lt", "lv"),
                List.of(
                        "ISO-8859-4", "latin-4", "latin4", "iso8859-4", "iso88594", "csisolatin4", "iso8859_4",
                        "iso_8859_4", "iso_8859_4_1988", "iso_ir_110", "l4"
                )
        ),

        /// ISO 8859-5, the single-byte encoding for Cyrillic text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        ISO_8859_5(
                "iso8859-5", "ISO-8859-5", Era.LEGACY_ISO, false,
                List.of("ru", "bg", "uk", "sr", "mk", "be"),
                List.of(
                        "ISO-8859-5", "iso8859-5", "cyrillic", "iso88595", "csisolatincyrillic", "iso8859_5",
                        "iso_8859_5", "iso_8859_5_1988", "iso_ir_144"
                )
        ),

        /// ISO 8859-6, the single-byte encoding for Arabic text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        ISO_8859_6(
                "iso8859-6", Era.LEGACY_ISO, false,
                List.of("ar", "fa"),
                List.of(
                        "ISO-8859-6", "iso8859-6", "arabic", "iso88596", "iso-8859-6-e", "iso-8859-6-i", "csiso88596e",
                        "csiso88596i", "asmo_708", "csisolatinarabic", "ecma_114", "iso8859_6", "iso_8859_6",
                        "iso_8859_6_1987", "iso_ir_127"
                )
        ),

        /// ISO 8859-7, the single-byte encoding for Greek text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        ISO_8859_7(
                "iso8859-7", "ISO-8859-7", Era.LEGACY_ISO, false,
                List.of("el"),
                List.of(
                        "ISO-8859-7", "iso8859-7", "greek", "iso88597", "sun_eu_greek", "csisolatingreek", "ecma_118",
                        "elot_928", "greek8", "iso8859_7", "iso_8859_7", "iso_8859_7_1987", "iso_ir_126"
                )
        ),

        /// ISO 8859-8, the single-byte encoding for Hebrew text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        ISO_8859_8(
                "iso8859-8", "ISO-8859-8", Era.LEGACY_ISO, false,
                List.of("he"),
                List.of(
                        "ISO-8859-8", "iso8859-8", "hebrew", "iso88598", "iso-8859-8-e", "iso-8859-8-i", "csiso88598e",
                        "csiso88598i", "visual", "logical", "csisolatinhebrew", "iso8859_8", "iso_8859_8",
                        "iso_8859_8_1988", "iso_8859_8_e", "iso_8859_8_i", "iso_ir_138"
                )
        ),

        /// ISO 8859-9, the single-byte Latin-5 encoding for Turkish text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        ISO_8859_9(
                "iso8859-9", "ISO-8859-9", Era.LEGACY_ISO, false,
                List.of("tr"),
                List.of(
                        "ISO-8859-9", "latin-5", "latin5", "iso8859-9", "iso88599", "csisolatin5", "iso8859_9",
                        "iso_8859_9", "iso_8859_9_1989", "iso_ir_148", "l5"
                )
        ),

        /// ISO 8859-10, the single-byte Latin-6 encoding for Nordic text.
        ///
        /// OpenJDK 17's standard charset providers do not include an exact
        /// [Charset] mapping for this encoding.
        ISO_8859_10(
                "iso8859-10", Era.LEGACY_ISO, false,
                List.of("is", "fi"),
                List.of(
                        "ISO-8859-10", "latin-6", "latin6", "iso8859-10", "iso885910", "csisolatin6", "iso8859_10",
                        "iso_8859_10", "iso_8859_10_1992", "iso_ir_157", "l6"
                )
        ),

        /// ISO 8859-13, the single-byte Latin-7 encoding for Baltic text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        ISO_8859_13(
                "iso8859-13", Era.LEGACY_ISO, false,
                List.of("et", "lt", "lv"),
                List.of(
                        "ISO-8859-13", "latin-7", "latin7", "iso8859-13", "iso885913", "csiso885913", "iso8859_13",
                        "iso_8859_13", "l7"
                )
        ),

        /// ISO 8859-14, the single-byte Latin-8 encoding for Celtic text.
        ///
        /// OpenJDK 17's standard charset providers do not include an exact
        /// [Charset] mapping for this encoding.
        ISO_8859_14(
                "iso8859-14", Era.LEGACY_ISO, false,
                List.of("cy", "ga", "br", "gd"),
                List.of(
                        "ISO-8859-14", "latin-8", "latin8", "iso8859-14", "iso885914", "csiso885914", "iso-ir-199",
                        "iso-celtic", "l8", "iso8859_14", "iso_8859_14", "iso_8859_14_1998", "iso_celtic", "iso_ir_199"
                )
        ),

        /// ISO 8859-15, the single-byte Latin-9 encoding with the euro sign.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        ISO_8859_15(
                "iso8859-15", Era.LEGACY_ISO, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                List.of(
                        "ISO-8859-15", "latin-9", "latin9", "iso8859-15", "iso885915", "csisolatin9", "csiso885915",
                        "l9", "iso8859_15", "iso_8859_15"
                )
        ),

        /// ISO 8859-16, the single-byte Latin-10 encoding for South-Eastern European text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        ISO_8859_16(
                "iso8859-16", Era.LEGACY_ISO, false,
                List.of("ro", "pl", "hr", "hu", "sk", "sl"),
                List.of(
                        "ISO-8859-16", "latin-10", "latin10", "iso8859-16", "iso885916", "csiso885916", "iso-ir-226",
                        "l10", "iso8859_16", "iso_8859_16", "iso_8859_16_2001", "iso_ir_226"
                )
        ),

        /// The Johab multibyte encoding for Korean text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        JOHAB(
                "johab", "Johab", Era.LEGACY_ISO, true,
                List.of("ko"),
                List.of(
                        "Johab", "cp1361", "ms1361"
                )
        ),

        /// The classic Mac OS single-byte encoding for Cyrillic text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        MAC_CYRILLIC(
                "mac-cyrillic", "MacCyrillic", Era.LEGACY_MAC, false,
                List.of("ru", "bg", "uk", "sr", "mk", "be"),
                List.of(
                        "Mac-Cyrillic", "MacCyrillic", "maccyrillic", "x-mac-cyrillic", "x-mac-ukrainian",
                        "mac_cyrillic"
                )
        ),

        /// The classic Mac OS single-byte encoding for Greek text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        MAC_GREEK(
                "mac-greek", "MacGreek", Era.LEGACY_MAC, false,
                List.of("el"),
                List.of(
                        "Mac-Greek", "MacGreek", "macgreek", "mac_greek"
                )
        ),

        /// The classic Mac OS single-byte encoding for Icelandic text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        MAC_ICELAND(
                "mac-iceland", "MacIceland", Era.LEGACY_MAC, false,
                List.of("is"),
                List.of(
                        "Mac-Iceland", "MacIceland", "maciceland", "mac_iceland"
                )
        ),

        /// The classic Mac OS single-byte encoding for Central European text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        MAC_LATIN2(
                "mac-latin2", "MacLatin2", Era.LEGACY_MAC, false,
                List.of("pl", "cs", "hu", "hr", "sk", "sl"),
                List.of(
                        "Mac-Latin2", "MacLatin2", "maclatin2", "maccentraleurope", "mac_centeuro", "mac_latin2"
                )
        ),

        /// The classic Mac OS single-byte Roman encoding for Western text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        MAC_ROMAN(
                "mac-roman", "MacRoman", Era.LEGACY_MAC, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                List.of(
                        "Mac-Roman", "MacRoman", "macroman", "macintosh", "csmacintosh", "mac", "x-mac-roman",
                        "mac_roman"
                )
        ),

        /// The classic Mac OS single-byte encoding for Turkish text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        MAC_TURKISH(
                "mac-turkish", "MacTurkish", Era.LEGACY_MAC, false,
                List.of("tr"),
                List.of(
                        "Mac-Turkish", "MacTurkish", "macturkish", "mac_turkish"
                )
        ),

        /// The DOS CP720 single-byte encoding for Arabic text.
        ///
        /// OpenJDK 17's standard charset providers do not include an exact
        /// [Charset] mapping for this encoding.
        CP720(
                "cp720", Era.LEGACY_REGIONAL, false,
                List.of("ar", "fa"),
                List.of(
                        "CP720"
                )
        ),

        /// The single-byte CP1006 encoding for Urdu text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP1006(
                "cp1006", Era.LEGACY_REGIONAL, false,
                List.of("ur"),
                List.of(
                        "CP1006"
                )
        ),

        /// The DOS CP1125 single-byte encoding for Ukrainian text.
        ///
        /// OpenJDK 17's standard charset providers do not include an exact
        /// [Charset] mapping for this encoding.
        CP1125(
                "cp1125", Era.LEGACY_REGIONAL, false,
                List.of("uk"),
                List.of(
                        "CP1125", "1125", "cp866u", "ibm1125", "ruscii"
                )
        ),

        /// The single-byte KOI8-T encoding for Tajik text.
        ///
        /// OpenJDK 17's standard charset providers do not include an exact
        /// [Charset] mapping for this encoding.
        KOI8_T(
                "koi8-t", Era.LEGACY_REGIONAL, false,
                List.of("tg"),
                List.of(
                        "KOI8-T"
                )
        ),

        /// The single-byte KZ-1048 encoding for Kazakh text.
        ///
        /// OpenJDK 17's standard charset providers do not include an exact
        /// [Charset] mapping for this encoding.
        KZ1048(
                "kz1048", "KZ1048", Era.LEGACY_REGIONAL, false,
                List.of("kk"),
                List.of(
                        "KZ-1048", "kz1048", "strk1048-2002", "rk1048", "kz_1048", "strk1048_2002"
                )
        ),

        /// The single-byte PTCP154 encoding for Kazakh text.
        ///
        /// OpenJDK 17's standard charset providers do not include an exact
        /// [Charset] mapping for this encoding.
        PTCP154(
                "ptcp154", Era.LEGACY_REGIONAL, false,
                List.of("kk"),
                List.of(
                        "PTCP154", "pt154", "cp154", "csptcp154", "cyrillic_asian"
                )
        ),

        /// Hewlett-Packard's single-byte Roman8 encoding for Western text.
        ///
        /// OpenJDK 17's standard charset providers do not include an exact
        /// [Charset] mapping for this encoding.
        HP_ROMAN8(
                "hp-roman8", Era.LEGACY_REGIONAL, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                List.of(
                        "HP-Roman8", "roman8", "r8", "csHPRoman8", "cp1051", "hp_roman8", "ibm1051"
                )
        ),

        /// The original IBM PC OEM single-byte code page for United States text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP437(
                "cp437", Era.DOS, false,
                List.of("en", "fr", "de", "es", "pt", "it", "nl", "da", "sv", "fi", "ga"),
                List.of(
                        "CP437", "437", "cspc8codepage437", "ibm437"
                )
        ),

        /// The DOS CP737 single-byte encoding for Greek text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP737(
                "cp737", Era.DOS, false,
                List.of("el"),
                List.of(
                        "CP737"
                )
        ),

        /// The DOS CP775 single-byte encoding for Baltic text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP775(
                "cp775", Era.DOS, false,
                List.of("et", "lt", "lv"),
                List.of(
                        "CP775", "775", "cspc775baltic", "ibm775"
                )
        ),

        /// The DOS CP850 single-byte encoding for Western European text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP850(
                "cp850", Era.DOS, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                List.of(
                        "CP850", "850", "cspc850multilingual", "ibm850"
                )
        ),

        /// The DOS CP852 single-byte encoding for Central European text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP852(
                "cp852", Era.DOS, false,
                List.of("pl", "cs", "hu", "hr", "ro", "sk", "sl"),
                List.of(
                        "CP852", "852", "cspcp852", "ibm852"
                )
        ),

        /// The DOS CP855 single-byte encoding for Cyrillic text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP855(
                "cp855", "IBM855", Era.DOS, false,
                List.of("ru", "bg", "uk", "sr", "mk", "be"),
                List.of(
                        "CP855", "855", "csibm855", "ibm855"
                )
        ),

        /// The DOS CP856 single-byte encoding for Hebrew text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP856(
                "cp856", Era.DOS, false,
                List.of("he"),
                List.of(
                        "CP856"
                )
        ),

        /// The DOS CP857 single-byte encoding for Turkish text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP857(
                "cp857", Era.DOS, false,
                List.of("tr"),
                List.of(
                        "CP857", "857", "csibm857", "ibm857"
                )
        ),

        /// The euro-enabled DOS CP858 variant of CP850.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP858(
                "cp858", Era.DOS, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                List.of(
                        "CP858", "858", "cp00858", "csibm00858", "csibm858", "ibm00858", "ibm858",
                        "pc_multilingual_850_euro"
                )
        ),

        /// The DOS CP860 single-byte encoding for Portuguese text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP860(
                "cp860", Era.DOS, false,
                List.of("pt"),
                List.of(
                        "CP860", "860", "csibm860", "ibm860"
                )
        ),

        /// The DOS CP861 single-byte encoding for Icelandic text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP861(
                "cp861", Era.DOS, false,
                List.of("is"),
                List.of(
                        "CP861", "861", "cp_is", "csibm861", "ibm861"
                )
        ),

        /// The DOS CP862 single-byte encoding for Hebrew text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP862(
                "cp862", Era.DOS, false,
                List.of("he"),
                List.of(
                        "CP862", "862", "cspc862latinhebrew", "ibm862"
                )
        ),

        /// The DOS CP863 single-byte encoding for Canadian French text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP863(
                "cp863", Era.DOS, false,
                List.of("fr"),
                List.of(
                        "CP863", "863", "csibm863", "ibm863"
                )
        ),

        /// The DOS CP864 single-byte encoding for Arabic text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP864(
                "cp864", Era.DOS, false,
                List.of("ar"),
                List.of(
                        "CP864", "864", "csibm864", "ibm864"
                )
        ),

        /// The DOS CP865 single-byte encoding for Nordic text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP865(
                "cp865", Era.DOS, false,
                List.of("da", "no"),
                List.of(
                        "CP865", "865", "csibm865", "ibm865"
                )
        ),

        /// The DOS CP866 single-byte encoding for Cyrillic text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP866(
                "cp866", "IBM866", Era.DOS, false,
                List.of("ru", "bg", "uk", "sr", "mk", "be"),
                List.of(
                        "CP866", "866", "csibm866", "ibm866"
                )
        ),

        /// The DOS CP869 single-byte encoding for Greek text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP869(
                "cp869", Era.DOS, false,
                List.of("el"),
                List.of(
                        "CP869", "869", "cp_gr", "csibm869", "ibm869"
                )
        ),

        /// The euro-enabled EBCDIC CP1140 encoding derived from code page 37.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP1140(
                "cp1140", Era.MAINFRAME, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv", "tr"),
                List.of(
                        "CP1140", "cp037", "cp01140", "ibm01140", "ibm1140", "csibm01140", "037", "1140", "csibm037",
                        "ebcdic_cp_ca", "ebcdic_cp_nl", "ebcdic_cp_us", "ebcdic_cp_wt", "ebcdic_us_37_euro", "ibm037",
                        "ibm039"
                )
        ),

        /// The EBCDIC CP424 single-byte encoding for Hebrew text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP424(
                "cp424", Era.MAINFRAME, false,
                List.of("he"),
                List.of(
                        "CP424", "424", "csibm424", "ebcdic_cp_he", "ibm424"
                )
        ),

        /// The EBCDIC CP500 single-byte encoding for international Latin text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP500(
                "cp500", Era.MAINFRAME, false,
                List.of("br", "cy", "da", "de", "en", "es", "fi", "fr", "ga", "id", "is", "it", "ms", "nl", "no", "pt", "sv"),
                List.of(
                        "CP500", "500", "csibm500", "ebcdic_cp_be", "ebcdic_cp_ch", "ibm500"
                )
        ),

        /// The EBCDIC CP875 single-byte encoding for Greek text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP875(
                "cp875", Era.MAINFRAME, false,
                List.of("el"),
                List.of(
                        "CP875"
                )
        ),

        /// The EBCDIC CP1026 single-byte encoding for Turkish text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP1026(
                "cp1026", Era.MAINFRAME, false,
                List.of("tr"),
                List.of(
                        "CP1026", "1026", "csibm1026", "ibm1026"
                )
        ),

        /// The EBCDIC CP273 single-byte encoding for German text.
        ///
        /// OpenJDK 17's standard charset providers include an exact [Charset]
        /// mapping for this encoding.
        CP273(
                "cp273", Era.MAINFRAME, false,
                List.of("de"),
                List.of(
                        "CP273", "273", "csibm273", "ibm273"
                )
        );

        /// Immutable set containing every encoding target.
        private static final @Unmodifiable Set<Encoding> ALL =
                Collections.unmodifiableSet(EnumSet.allOf(Encoding.class));

        /// Canonical registry name.
        private final String canonicalName;

        /// Display name used by the command-line interface.
        private final String displayName;

        /// Historical or operational encoding group.
        private final Era era;

        /// Whether multibyte detection stages apply.
        private final boolean multibyte;

        /// Possible ISO 639 language codes.
        private final @Unmodifiable List<String> languages;

        /// Canonical, standards, and codec aliases in lookup order.
        private final @Unmodifiable List<String> aliases;

        /// Successfully resolved exact runtime charset.
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private @Nullable Optional<Charset> charsetCache;

        /// Successfully resolved approximate runtime charset.
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private @Nullable Optional<Charset> approximateCharsetCache;

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
                @Unmodifiable List<String> aliases
        ) {
            this(canonicalName, canonicalName, era, multibyte, languages, aliases);
        }

        /// Creates an encoding with distinct canonical and display names.
        ///
        /// @param canonicalName canonical registry name
        /// @param displayName   command-line display name
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
                @Unmodifiable List<String> aliases
        ) {
            this.canonicalName = canonicalName;
            this.displayName = displayName;
            this.era = era;
            this.multibyte = multibyte;
            this.languages = List.copyOf(languages);
            this.aliases = List.copyOf(aliases);
        }

        /// Returns the stable canonical name of this detection target.
        ///
        /// @return canonical registry name used for interchange and alias lookup
        public String canonicalName() {
            return canonicalName;
        }

        /// Returns the display name used by the command-line interface.
        ///
        /// @return name used by the command-line interface
        public String displayName() {
            return displayName;
        }

        /// Returns a Java charset for the encoded text payload.
        ///
        /// [#UTF_8_SIG] returns [StandardCharsets#UTF_8]; the caller must handle
        /// its signature framing separately. For every other encoding,
        /// availability depends on installed charset providers, and this method
        /// returns `null` instead of substituting a charset with different
        /// character mappings. For example, [#EUC_JIS_2004] is not mapped to
        /// EUC-JP. This method is safe for concurrent invocation.
        ///
        /// @return payload charset, or `null` when no suitable mapping is available
        public @Nullable Charset charset() {
            @Nullable Optional<Charset> charsetCache = this.charsetCache;
            //noinspection OptionalAssignedToNull
            if (charsetCache != null) {
                return charsetCache.orElse(null);
            }

            String preferredName = switch (this) {
                case UTF_8_SIG -> "UTF-8";
                case UTF_16_BE -> "UTF-16BE";
                case UTF_16_LE -> "UTF-16LE";
                case UTF_32_BE -> "UTF-32BE";
                case UTF_32_LE -> "UTF-32LE";
                case CP932 -> "windows-31j";
                case CP949 -> "x-windows-949";
                case HZ -> "HZ-GB-2312";
                case ISO_2022_JP_2 -> "ISO-2022-JP-2";
                case ISO_2022_JP_2004 -> "ISO-2022-JP-2004";
                case ISO_2022_JP_EXT -> "ISO-2022-JP-EXT";
                case ISO_2022_KR -> "ISO-2022-KR";
                case SHIFT_JIS_2004 -> "x-SJIS_0213";
                case CP874 -> "x-windows-874";
                case ISO_8859_10 -> "ISO-8859-10";
                case ISO_8859_14 -> "ISO-8859-14";
                case ISO_8859_16 -> "ISO-8859-16";
                case MAC_CYRILLIC -> "x-MacCyrillic";
                case MAC_GREEK -> "x-MacGreek";
                case MAC_ICELAND -> "x-MacIceland";
                case MAC_LATIN2 -> "x-MacCentralEurope";
                case MAC_ROMAN -> "x-MacRoman";
                case MAC_TURKISH -> "x-MacTurkish";
                case CP720 -> "IBM720";
                case CP1125 -> "IBM1125";
                case KOI8_T -> "KOI8-T";
                case KZ1048 -> "KZ-1048";
                default -> canonicalName;
            };
            @Nullable Charset charset = findCharset(preferredName);
            if (charset != null) {
                this.charsetCache = Optional.of(charset);
                return charset;
            }
            // OpenJDK assigns these ambiguous numeric names to IBM variants.
            if (this == CP932 || this == CP949 || this == CP874 || preferredName.equals(canonicalName)) {
                return null;
            }
            charset = findCharset(canonicalName);
            this.charsetCache = Optional.ofNullable(charset);
            return charset;
        }

        /// Returns an exact or approximate Java charset for this encoding.
        ///
        /// Returns [#charset()] when an exact mapping is available. Otherwise,
        /// it returns a configured related charset.
        ///
        /// A non-exact charset may reject or decode source bytes differently.
        ///
        /// @return available charset, or `null` when no mapping is available
        public @Nullable Charset approximateCharset() {
            @Nullable Charset charset = charset();
            if (charset != null) {
                return charset;
            }

            @Nullable Optional<Charset> approximateCharsetCache = this.approximateCharsetCache;
            //noinspection OptionalAssignedToNull
            if (approximateCharsetCache != null) {
                return approximateCharsetCache.orElse(null);
            }

            List<String> approximateNames = switch (this) {
                case BIG5_HKSCS -> List.of("x-Big5-HKSCS-2001", "Big5");
                case CP932 -> List.of("x-MS932_0213", "Shift_JIS");
                case SHIFT_JIS_2004 -> List.of("Shift_JIS");
                case CP949 -> List.of("EUC-KR");
                case EUC_JIS_2004 -> List.of("EUC-JP");
                case GB18030 -> List.of("GBK", "GB2312");
                case ISO_2022_JP_2, ISO_2022_JP_2004, ISO_2022_JP_EXT -> List.of("ISO-2022-JP");
                case CP874 -> List.of("TIS-620");
                case KOI8_U, KOI8_T -> List.of("KOI8-R");
                case TIS_620 -> List.of("x-windows-874");
                case ISO_8859_15 -> List.of("ISO-8859-1");
                case CP1125 -> List.of("IBM866");
                case KZ1048, PTCP154 -> List.of("windows-1251");
                case CP858 -> List.of("IBM850");
                case CP1140 -> List.of("IBM037");
                default -> List.of();
            };

            for (String approximateName : approximateNames) {
                charset = findCharset(approximateName);
                if (charset != null) {
                    this.approximateCharsetCache = Optional.of(charset);
                    return charset;
                }
            }

            this.approximateCharsetCache = Optional.empty();
            return null;
        }

        /// Returns whether [#charset()] supplies a Java charset for this
        /// encoding.
        ///
        /// This method is equivalent to `charset() != null`. For [#UTF_8_SIG],
        /// support covers the UTF-8 payload; its signature framing must still be
        /// handled separately. The result for other encodings may depend on the
        /// charset providers installed in the current runtime. This method is
        /// safe for concurrent invocation.
        ///
        /// @return `true` if the runtime can process this encoding
        public boolean isCharsetSupported() {
            return charset() != null;
        }

        /// Returns the historical or operational group assigned to this target.
        ///
        /// @return era used by era-based encoding selection
        public Era era() {
            return era;
        }

        /// Reports whether multibyte detection stages apply.
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
        /// this list when it is explicitly registered as an alias.
        ///
        /// @return immutable aliases in lookup order
        public @Unmodifiable List<String> aliases() {
            return aliases;
        }

        /// Returns all registered encoding targets in enum declaration order.
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

        /// Resolves one fixed valid charset name without exposing lookup failure.
        ///
        /// @param name charset name to resolve
        /// @return registered charset, or `null` when unsupported
        private static @Nullable Charset findCharset(String name) {
            try {
                return Charset.forName(name);
            } catch (UnsupportedCharsetException ignored) {
                return null;
            }
        }

        /// Holds alias indexes derived from the enum constants.
        @NotNullByDefault
        private static final class AliasIndex {
            /// Required number of canonical targets.
            private static final int EXPECTED_ENCODING_COUNT = 86;

            /// Exact case-folded canonical names and aliases.
            private static final @Unmodifiable Map<String, Encoding> EXACT_ALIASES;

            /// Punctuation-normalized canonical names and aliases.
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
            /// preserve the first mapping when normalized forms collide.
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

            /// Normalizes a name for punctuation-insensitive alias lookup.
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
    /// Instances are created by [#detect(byte[])] and [#detect(ByteBuffer)];
    /// applications cannot construct arbitrary candidates.
    ///
    /// @apiNote [Encoding#charset()] returns a Java charset for the encoded
    /// payload when one is available in the current runtime. External framing
    /// associated with an encoding is not represented by that charset.
    @NotNullByDefault
    public static final class Candidate {
        /// Detected encoding, or `null` for a non-text classification.
        private final @Nullable Encoding encoding;

        /// Detection confidence in the range `[0.0, 1.0]`.
        private final double confidence;

        /// ISO 639 language code, or `null` when undetermined.
        private final @Nullable String language;

        /// Detected or inferred MIME type.
        private final String mimeType;

        /// Creates a complete candidate.
        ///
        /// @param encoding   detected encoding, or `null` for a non-text result
        /// @param confidence confidence in the range `[0.0, 1.0]`
        /// @param language   ISO 639 language code, or `null` when undetermined
        /// @param mimeType   detected or inferred MIME type
        private Candidate(
                @Nullable Encoding encoding,
                double confidence,
                @Nullable String language,
                String mimeType
        ) {
            this.encoding = encoding;
            this.confidence = confidence;
            this.language = language;
            this.mimeType = mimeType;
        }

        /// Returns the detected encoding.
        ///
        /// @return detected encoding, or `null` for a non-text classification
        public @Nullable Encoding encoding() {
            return encoding;
        }

        /// Returns the detection confidence.
        ///
        /// @return confidence in the range `[0.0, 1.0]`
        public double confidence() {
            return confidence;
        }

        /// Returns the detected language.
        ///
        /// @return ISO 639 language code, or `null` when undetermined
        public @Nullable String language() {
            return language;
        }

        /// Returns the detected or inferred MIME type.
        ///
        /// @return MIME type
        public String mimeType() {
            return mimeType;
        }

        /// Compares this candidate with another object by value.
        ///
        /// @param object object to compare
        /// @return whether both objects contain equal candidate properties
        @Override
        public boolean equals(@Nullable Object object) {
            if (this == object) {
                return true;
            }
            return object instanceof Candidate other
                    && encoding == other.encoding
                    && Double.compare(confidence, other.confidence) == 0
                    && Objects.equals(language, other.language)
                    && mimeType.equals(other.mimeType);
        }

        /// Returns a value-based hash code for this candidate.
        ///
        /// @return hash code derived from all candidate properties
        @Override
        public int hashCode() {
            int result = Objects.hashCode(encoding);
            result = 31 * result + Double.hashCode(confidence);
            result = 31 * result + Objects.hashCode(language);
            return 31 * result + mimeType.hashCode();
        }

        /// Returns a string representation of this candidate.
        ///
        /// @return string containing all candidate properties
        @Override
        public String toString() {
            return "Candidate[encoding=" + encoding
                    + ", confidence=" + confidence
                    + ", language=" + language
                    + ", mimeType=" + mimeType + ']';
        }
    }

    /// Contains the candidates and recommended encoding for one detection outcome.
    ///
    /// The candidate list is an immutable snapshot in descending-confidence
    /// order. Results returned by [#detect(byte[])] and [#detect(ByteBuffer)]
    /// contain only candidates whose confidence meets the detector's inclusive
    /// [#minimumConfidence()] value, with stable ordering for equal confidences.
    ///
    /// Instances are created by [#detect(byte[])] and [#detect(ByteBuffer)];
    /// applications cannot construct arbitrary results.
    ///
    /// A recommendation can exist without a candidate. This occurs when the
    /// detector applies its empty-input recommendation or configured fallback
    /// encoding. Such a recommendation carries no confidence, language, or
    /// MIME type. When a candidate is present, the recommendation is exactly
    /// the first candidate's encoding and may therefore be `null` for a
    /// non-text classification.
    @NotNullByDefault
    public static final class Result {
        /// Qualifying candidates in descending-confidence order.
        private final @Unmodifiable List<Candidate> candidates;

        /// Recommended encoding, or `null` when no encoding is recommended.
        private final @Nullable Encoding bestEncoding;

        /// Creates a result after copying and validating its candidates.
        ///
        /// @param candidates   qualifying candidates in descending-confidence order;
        ///                     may be empty
        /// @param bestEncoding recommended encoding, or `null` when no encoding is
        ///                     recommended
        /// @throws NullPointerException     if `candidates` or an element is `null`
        /// @throws IllegalArgumentException if candidates are not in
        /// descending-confidence order, or a nonempty list's first candidate
        /// does not have `bestEncoding`
        private Result(
                List<Candidate> candidates,
                @Nullable Encoding bestEncoding
        ) {
            List<Candidate> copy = List.copyOf(candidates);
            for (int index = 1; index < copy.size(); index++) {
                if (copy.get(index - 1).confidence()
                        < copy.get(index).confidence()) {
                    throw new IllegalArgumentException(
                            "candidates must be in descending-confidence order"
                    );
                }
            }
            if (!copy.isEmpty() && copy.get(0).encoding() != bestEncoding) {
                throw new IllegalArgumentException(
                        "bestEncoding must match the highest-ranked candidate"
                );
            }
            this.candidates = copy;
            this.bestEncoding = bestEncoding;
        }

        /// Returns the qualifying candidates.
        ///
        /// @return immutable candidates in descending-confidence order
        public @Unmodifiable List<Candidate> candidates() {
            return candidates;
        }

        /// Returns the recommended encoding.
        ///
        /// @return recommended encoding, or `null` when none is available
        public @Nullable Encoding bestEncoding() {
            return bestEncoding;
        }

        /// Returns the highest-ranked candidate, if present.
        ///
        /// @return first candidate, or `null` when `candidates` is empty
        public @Nullable Candidate bestCandidate() {
            return candidates.isEmpty() ? null : candidates.get(0);
        }

        /// Compares this result with another object by value.
        ///
        /// @param object object to compare
        /// @return whether both objects contain equal candidates and the same
        /// recommended encoding
        @Override
        public boolean equals(@Nullable Object object) {
            if (this == object) {
                return true;
            }
            return object instanceof Result other
                    && candidates.equals(other.candidates)
                    && bestEncoding == other.bestEncoding;
        }

        /// Returns a value-based hash code for this result.
        ///
        /// @return hash code derived from the candidates and recommended encoding
        @Override
        public int hashCode() {
            return 31 * candidates.hashCode()
                    + (bestEncoding == null ? 0 : bestEncoding.hashCode());
        }

        /// Returns a string representation of this result.
        ///
        /// @return string containing the candidates and recommended encoding
        @Override
        public String toString() {
            return "Result[candidates=" + candidates
                    + ", bestEncoding=" + bestEncoding + ']';
        }
    }

    /// Default maximum number of leading input bytes examined.
    ///
    /// This limit is used by [#DEFAULT] and the bundled command-line application.
    public static final long DEFAULT_MAX_BYTES = 256 * 1024L;

    /// Default inclusive confidence threshold used by [Result#candidates()].
    public static final double DEFAULT_MINIMUM_CONFIDENCE = 0.20;

    /// Default detector with every encoding target enabled.
    ///
    /// It examines at most 256 KiB, retains candidates with confidence of
    /// at least `0.20`, disables preferred-superset remapping, permits
    /// charset approximation for text decoding, allows every encoding target,
    /// makes no recommendation when nonempty input has no qualifying text
    /// candidate, and recommends [Encoding#UTF_8] for empty input.
    public static final EncodingDetector DEFAULT = new EncodingDetector(
            DEFAULT_MAX_BYTES,
            DEFAULT_MINIMUM_CONFIDENCE,
            false,
            true,
            EnumSet.allOf(Encoding.class),
            null,
            Encoding.UTF_8
    );

    /// Preset detector limited to encodings classified in [Era#MODERN_WEB].
    ///
    /// Its maximum input length, confidence threshold, preferred-superset
    /// setting, charset policy, and recommendation encodings are
    /// identical to those of [#DEFAULT].
    public static final EncodingDetector MODERN_WEB =
            DEFAULT.withEncodingEra(Era.MODERN_WEB);

    /// Maximum number of leading input bytes examined.
    private final long maxBytes;

    /// Inclusive lower confidence bound applied to result candidate lists.
    private final double minimumConfidence;

    /// Whether subset encodings are remapped to preferred supersets.
    private final boolean preferSuperset;

    /// Whether text-decoding operations may use charset approximation.
    private final boolean allowCharsetApproximation;

    /// Encoding targets permitted to participate in detection.
    private final @Unmodifiable EnumSet<Encoding> encodings;

    /// Optional encoding recommended when nonempty input has no qualifying text candidate.
    private final @Nullable Encoding fallbackEncoding;

    /// Encoding recommended for empty input.
    private final Encoding emptyInputEncoding;

    /// Creates a detector from validated immutable configuration state.
    ///
    /// @param maxBytes                  maximum leading input bytes examined
    /// @param minimumConfidence         inclusive candidate confidence bound
    /// @param preferSuperset            whether to remap subset encodings
    /// @param allowCharsetApproximation whether decoding may use charset approximation
    /// @param encodings                 immutable permitted encoding targets
    /// @param fallbackEncoding          nonempty-input fallback, or `null` for none
    /// @param emptyInputEncoding        empty-input recommendation
    private EncodingDetector(
            long maxBytes,
            double minimumConfidence,
            boolean preferSuperset,
            boolean allowCharsetApproximation,
            @Unmodifiable EnumSet<Encoding> encodings,
            @Nullable Encoding fallbackEncoding,
            Encoding emptyInputEncoding
    ) {
        this.maxBytes = maxBytes;
        this.minimumConfidence = minimumConfidence;
        this.preferSuperset = preferSuperset;
        this.allowCharsetApproximation = allowCharsetApproximation;
        this.encodings = encodings;
        this.fallbackEncoding = fallbackEncoding;
        this.emptyInputEncoding = emptyInputEncoding;
    }

    /// Returns the maximum number of leading bytes examined.
    ///
    /// @return a positive byte count
    public long maxBytes() {
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

    /// Reports whether text-decoding operations may use charset approximation.
    ///
    /// When enabled, methods that decode detected text use
    /// [Encoding#approximateCharset()]. Otherwise they require
    /// [Encoding#charset()]. Detection is unaffected.
    ///
    /// @return whether charset approximation is permitted for decoding
    public boolean allowsCharsetApproximation() {
        return allowCharsetApproximation;
    }

    /// Returns the encoding targets permitted to participate in detection.
    ///
    /// This is the complete effective selection. Any encoding- or era-selection
    /// method replaces it rather than adding another filter. The returned view
    /// cannot be modified.
    ///
    /// An empty set permits no text encoding or configured recommendation, but
    /// does not suppress binary classifications. Preferred-superset remapping
    /// may produce a result absent from this set.
    ///
    /// @return an immutable view of the permitted encodings in enum declaration
    /// order
    public @UnmodifiableView Set<Encoding> encodings() {
        //noinspection RedundantUnmodifiable
        return Collections.unmodifiableSet(encodings);
    }

    /// Returns the encoding recommended for unmatched nonempty input.
    ///
    /// @return configured fallback encoding, or `null` when none is configured
    public @Nullable Encoding fallbackEncoding() {
        return fallbackEncoding;
    }

    /// Returns the empty-input recommendation.
    ///
    /// @return recommended encoding
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
    public EncodingDetector withMaxBytes(long value) {
        if (value < 1L) {
            throw new IllegalArgumentException("value must be a positive integer");
        }
        if (maxBytes == value) {
            return this;
        }
        return new EncodingDetector(
                value,
                minimumConfidence,
                preferSuperset,
                allowCharsetApproximation,
                encodings,
                fallbackEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector with a new candidate confidence threshold.
    ///
    /// [Result#candidates()] contains only candidates whose confidence is
    /// greater than or equal to this value. A value of `0.0` retains every
    /// candidate produced by the detection pipeline.
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
                allowCharsetApproximation,
                encodings,
                fallbackEncoding,
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
                allowCharsetApproximation,
                encodings,
                fallbackEncoding,
                emptyInputEncoding
        );
    }

    /// Returns a detector that permits or rejects charset approximation.
    ///
    /// This option affects methods that decode detected text; it does not
    /// affect detection.
    ///
    /// @param enabled whether decoding may use [Encoding#approximateCharset()]
    /// @return this detector if unchanged; otherwise a new detector
    public EncodingDetector withCharsetApproximation(boolean enabled) {
        if (allowCharsetApproximation == enabled) {
            return this;
        }
        return new EncodingDetector(
                maxBytes,
                minimumConfidence,
                preferSuperset,
                enabled,
                encodings,
                fallbackEncoding,
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
    /// configured recommendation, but does not suppress binary classifications.
    /// The collection is copied and is not retained.
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

    /// Returns a detector using the supplied optional fallback encoding.
    ///
    /// When nonempty input has no qualifying text candidate and this encoding
    /// is permitted by [#encodings()], it is returned by
    /// [Result#bestEncoding()] without adding a [Candidate]. A `null` value
    /// disables that recommendation. This policy does not replace a detected
    /// non-text classification.
    ///
    /// @param value fallback encoding, or `null` to disable the recommendation
    /// @return this detector if unchanged; otherwise a new detector
    public EncodingDetector withFallbackEncoding(@Nullable Encoding value) {
        if (fallbackEncoding == value) {
            return this;
        }
        return new EncodingDetector(
                maxBytes,
                minimumConfidence,
                preferSuperset,
                allowCharsetApproximation,
                encodings,
                value,
                emptyInputEncoding
        );
    }

    /// Returns a detector using the supplied empty-input recommendation.
    ///
    /// Empty input produces no [Candidate]. If this encoding is permitted by
    /// [#encodings()], it is returned by [Result#bestEncoding()].
    ///
    /// @param value recommended encoding
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
                allowCharsetApproximation,
                encodings,
                fallbackEncoding,
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
    /// @return immutable result containing qualifying candidates and the
    /// recommended encoding
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
    /// @return immutable result containing qualifying candidates and the
    /// recommended encoding
    /// @throws NullPointerException if `input` is `null`
    public Result detect(ByteBuffer input) {
        return detectNormalized(ByteBufferSupport.view(input));
    }

    /// Detects and decodes an input array.
    ///
    /// Only the first [#maxBytes()] bytes participate in detection; the complete
    /// array is decoded with the selected charset. The configured charset
    /// approximation policy is applied, malformed and unmappable input is
    /// replaced, and a leading UTF-8 signature is omitted when
    /// [Encoding#UTF_8_SIG] is selected.
    ///
    /// The array is read directly without copying, is not retained, and is
    /// never modified. Its contents must not change while this method runs.
    ///
    /// @param input bytes to detect and decode
    /// @return decoded text
    /// @throws IllegalStateException       if no encoding can be selected
    /// @throws UnsupportedCharsetException if the selected encoding has no
    ///                                     permitted charset mapping
    /// @throws NullPointerException        if `input` is `null`
    public String toString(byte[] input) {
        return decodeNormalized(ByteBufferSupport.wrap(input));
    }

    /// Detects and decodes the remaining bytes of a buffer.
    ///
    /// This method has the detection and decoding semantics of
    /// [#toString(byte[])]. The buffer's content, position, limit, and mark are
    /// unchanged. Its remaining bytes are accessed directly without copying
    /// and must not change while this method runs.
    ///
    /// @param input buffer whose remaining bytes are decoded
    /// @return decoded text
    /// @throws IllegalStateException       if no encoding can be selected
    /// @throws UnsupportedCharsetException if the selected encoding has no
    ///                                     permitted charset mapping
    /// @throws NullPointerException        if `input` is `null`
    public String toString(ByteBuffer input) {
        return decodeNormalized(ByteBufferSupport.view(input));
    }

    /// Reads and decodes a file as a string.
    ///
    /// The file is decoded with the semantics of [#newReader(Path)]. The opened
    /// channel is closed before this method returns, including when reading or
    /// decoding fails.
    ///
    /// @param path file to read
    /// @return decoded file contents
    /// @throws IOException          if the file cannot be opened, read,
    ///                              decoded, or closed
    /// @throws NullPointerException if `path` is `null`
    public String readString(Path path) throws IOException {
        return readAndClose(newReader(path));
    }

    /// Reads and decodes an input stream as a string.
    ///
    /// This method takes ownership of `input`, reads it to end of stream, and
    /// closes it before returning, including when reading or decoding fails.
    /// Detection and decoding otherwise follow [#newReader(InputStream)].
    ///
    /// @param input byte stream to read
    /// @return decoded stream contents
    /// @throws IOException          if the stream cannot be read, decoded, or
    ///                              closed
    /// @throws NullPointerException if `input` is `null`
    public String readString(InputStream input) throws IOException {
        return readAndClose(newReader(input));
    }

    /// Reads and decodes a byte channel as a string.
    ///
    /// This method takes ownership of `channel`, reads it to end of input, and
    /// closes it before returning, including when reading or decoding fails.
    /// Detection and decoding otherwise follow
    /// [#newReader(ReadableByteChannel)].
    ///
    /// @param channel byte channel to read
    /// @return decoded channel contents
    /// @throws IOException                  if the channel cannot be read,
    ///                                      decoded, or closed
    /// @throws IllegalBlockingModeException if `channel` is a selectable
    ///                                      channel configured in non-blocking mode
    /// @throws NullPointerException         if `channel` is `null`
    public String readString(ReadableByteChannel channel) throws IOException {
        return readAndClose(newReader(channel));
    }

    /// Creates a reader that detects and decodes an input stream.
    ///
    /// The reader uses at most [#maxBytes()] leading bytes to select
    /// [Result#bestEncoding()], then decodes the stream with the charset mapping
    /// permitted by [#allowsCharsetApproximation()]. Malformed and unmappable
    /// input is replaced. When [Encoding#UTF_8_SIG] is selected, its leading
    /// signature is consumed.
    ///
    /// Detection and I/O failures are reported by read operations. If the
    /// selected encoding has no permitted charset mapping, reads throw
    /// [java.io.UnsupportedEncodingException]. The reader owns and closes
    /// `input` and is not safe for concurrent use.
    ///
    /// @param input byte stream to detect and decode
    /// @return reader positioned before the first decoded character
    /// @throws NullPointerException if `input` is `null`
    public Reader newReader(InputStream input) {
        return newReader(Channels.newChannel(input));
    }

    /// Creates a reader that detects and decodes a byte channel.
    ///
    /// This method has the detection, decoding, blocking, and failure semantics
    /// of [#newReader(InputStream)]. It does not read from `channel`. The returned
    /// reader owns the channel immediately and closes it when the reader is
    /// closed, including when no read has occurred.
    ///
    /// @param channel byte channel to detect and decode
    /// @return reader positioned before the first decoded character
    /// @throws IllegalBlockingModeException if `channel` is a selectable channel
    ///                                      configured in non-blocking mode
    /// @throws NullPointerException         if `channel` is `null`
    public Reader newReader(ReadableByteChannel channel) {
        return new EncodingReader(this, channel);
    }

    /// Creates a reader that detects and decodes a file.
    ///
    /// The returned reader owns the opened file channel and otherwise has the
    /// semantics of [#newReader(ReadableByteChannel)].
    ///
    /// @param path file to open and decode
    /// @return reader positioned before the first decoded character
    /// @throws IOException if the file cannot be opened for reading
    /// @throws NullPointerException if `path` is `null`
    public Reader newReader(Path path) throws IOException {
        return newReader(Files.newByteChannel(path));
    }

    /// Creates a buffered reader that detects and decodes an input stream.
    ///
    /// The returned reader has the detection, decoding, and ownership semantics
    /// of [#newReader(InputStream)].
    ///
    /// @param input byte stream to detect and decode
    /// @return buffered reader positioned before the first decoded character
    /// @throws NullPointerException if `input` is `null`
    public BufferedReader newBufferedReader(InputStream input) {
        return new BufferedReader(newReader(input));
    }

    /// Creates a buffered reader that detects and decodes a byte channel.
    ///
    /// The returned reader has the detection, decoding, and ownership semantics
    /// of [#newReader(ReadableByteChannel)].
    ///
    /// @param channel byte channel to detect and decode
    /// @return buffered reader positioned before the first decoded character
    /// @throws IllegalBlockingModeException if `channel` is a selectable channel
    ///                                      configured in non-blocking mode
    /// @throws NullPointerException         if `channel` is `null`
    public BufferedReader newBufferedReader(ReadableByteChannel channel) {
        return new BufferedReader(newReader(channel));
    }

    /// Creates a buffered reader that detects and decodes a file.
    ///
    /// The returned reader owns the opened file channel and otherwise has the
    /// semantics of [#newBufferedReader(ReadableByteChannel)].
    ///
    /// @param path file to open and decode
    /// @return buffered reader positioned before the first decoded character
    /// @throws IOException if the file cannot be opened for reading
    /// @throws NullPointerException if `path` is `null`
    public BufferedReader newBufferedReader(Path path) throws IOException {
        return new BufferedReader(newReader(path));
    }

    /// Reads every character from a reader and then closes it.
    ///
    /// @param reader owned reader to consume
    /// @return all decoded characters
    /// @throws IOException if reading or closure fails
    private static String readAndClose(Reader reader) throws IOException {
        try (reader) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[8192];
            int count;
            while ((count = reader.read(buffer)) >= 0) {
                result.append(buffer, 0, count);
            }
            return result.toString();
        }
    }

    /// Decodes a normalized byte-buffer view with its detected encoding.
    ///
    /// @param input normalized bytes to detect and decode
    /// @return decoded text
    /// @throws IllegalStateException       if no encoding can be selected
    /// @throws UnsupportedCharsetException if the selected encoding has no
    ///                                     permitted charset mapping
    private String decodeNormalized(@UnmodifiableView ByteBuffer input) {
        @Nullable Encoding encoding = detectNormalized(input).bestEncoding();
        if (encoding == null) {
            throw new IllegalStateException("No character encoding could be selected");
        }

        @Nullable Charset charset = allowCharsetApproximation
                ? encoding.approximateCharset()
                : encoding.charset();
        if (charset == null) {
            throw new UnsupportedCharsetException(encoding.canonicalName());
        }

        int start = input.position();
        if (encoding == Encoding.UTF_8_SIG
                && input.remaining() >= 3
                && input.get(start) == (byte) 0xef
                && input.get(start + 1) == (byte) 0xbb
                && input.get(start + 2) == (byte) 0xbf) {
            input.position(start + 3);
        }

        if (input.hasArray()) {
            return new String(
                    input.array(),
                    input.arrayOffset() + input.position(),
                    input.remaining(),
                    charset
            );
        }
        return charset.decode(input).toString();
    }

    /// Detects candidates for a normalized buffer view.
    ///
    /// @param input bytes to examine
    /// @return immutable aggregate result
    private Result detectNormalized(@UnmodifiableView ByteBuffer input) {
        List<Candidate> detectedCandidates = DetectionEngine.detect(input, this)
                .map(result -> {
                    @Nullable Encoding encoding = transformEncoding(result.encoding());
                    double confidence = Math.max(
                            0.0,
                            Math.min(result.confidence(), 1.0)
                    );
                    @Nullable String detectedMimeType = result.mimeType();
                    String mimeType;
                    if (detectedMimeType != null) {
                        mimeType = detectedMimeType;
                    } else {
                        mimeType = result.encoding() == null
                                ? "application/octet-stream"
                                : "text/plain";
                    }
                    return new Candidate(
                            encoding,
                            confidence,
                            result.language(),
                            mimeType
                    );
                })
                .sorted(Comparator.comparingDouble(Candidate::confidence).reversed())
                .toList();
        int candidateCount = 0;
        while (candidateCount < detectedCandidates.size()
                && detectedCandidates.get(candidateCount).confidence() >= minimumConfidence) {
            candidateCount++;
        }
        List<Candidate> candidates = candidateCount == detectedCandidates.size()
                ? detectedCandidates
                : detectedCandidates.subList(0, candidateCount);

        @Nullable Encoding bestEncoding;
        if (!candidates.isEmpty()) {
            bestEncoding = candidates.get(0).encoding();
        } else if (input.remaining() == 0) {
            bestEncoding = encodings.contains(emptyInputEncoding)
                    ? transformEncoding(emptyInputEncoding)
                    : null;
        } else if (!detectedCandidates.isEmpty()
                && detectedCandidates.get(0).encoding() == null) {
            bestEncoding = null;
        } else {
            @Nullable Encoding fallback = fallbackEncoding;
            bestEncoding = fallback != null && encodings.contains(fallback)
                    ? transformEncoding(fallback)
                    : null;
        }
        return new Result(candidates, bestEncoding);
    }

    /// Applies preferred-superset transformation to an encoding.
    ///
    /// @param encoding encoding, or `null`
    /// @return transformed encoding, or `null`
    private @Nullable Encoding transformEncoding(@Nullable Encoding encoding) {
        if (encoding == null) {
            return null;
        }
        return preferSuperset
                ? PREFERRED_SUPERSETS.getOrDefault(encoding, encoding)
                : encoding;
    }

    /// Returns a detector using an already validated independent encoding set.
    ///
    /// @param value independently owned permitted encodings
    /// @return this detector if unchanged; otherwise a new detector
    private EncodingDetector withEncodingSet(@Unmodifiable EnumSet<Encoding> value) {
        if (encodings.equals(value)) {
            return this;
        }
        return new EncodingDetector(
                maxBytes,
                minimumConfidence,
                preferSuperset,
                allowCharsetApproximation,
                value,
                fallbackEncoding,
                emptyInputEncoding
        );
    }
}
