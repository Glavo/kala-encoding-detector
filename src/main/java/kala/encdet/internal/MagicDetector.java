// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/// Detects binary MIME types from fixed and container-specific signatures.
@NotNullByDefault
final class MagicDetector {
    /// Fixed offset-zero signatures in reference matching order.
    private static final @Unmodifiable List<Signature> SIGNATURES = List.of(
            signature("89504e470d0a1a0a", "image/png"),
            asciiSignature("GIF87a", "image/gif"),
            asciiSignature("GIF89a", "image/gif"),
            signature("4d4d002a", "image/tiff"),
            signature("49492a00", "image/tiff"),
            asciiSignature("8BPS", "image/vnd.adobe.photoshop"),
            asciiSignature("qoif", "image/qoi"),
            asciiSignature("BM", "image/bmp"),
            signature("ffd8ff", "image/jpeg"),
            signature("0000000c4a584c200d0a870a", "image/jxl"),
            signature("ff0a", "image/jxl"),
            signature("00000100", "image/vnd.microsoft.icon"),
            asciiSignature("ID3", "audio/mpeg"),
            asciiSignature("MThd", "audio/midi"),
            asciiSignature("OggS", "audio/ogg"),
            asciiSignature("fLaC", "audio/flac"),
            signature("1a45dfa3", "video/webm"),
            signature("1f8b", "application/gzip"),
            asciiSignature("BZh", "application/x-bzip2"),
            signature("fd377a585a00", "application/x-xz"),
            signature("377abcaf271c", "application/x-7z-compressed"),
            signature("526172211a070100", "application/vnd.rar"),
            signature("526172211a0700", "application/vnd.rar"),
            signature("28b52ffd", "application/zstd"),
            asciiSignature("%PDF-", "application/pdf"),
            signature("53514c69746520666f726d6174203300", "application/x-sqlite3"),
            asciiSignature("ARROW1", "application/vnd.apache.arrow.file"),
            asciiSignature("PAR1", "application/vnd.apache.parquet"),
            signature("0061736d", "application/wasm"),
            asciiSignature("dex\n", "application/vnd.android.dex"),
            signature("7f454c46", "application/x-elf"),
            signature("feedface", "application/x-mach-binary"),
            signature("feedfacf", "application/x-mach-binary"),
            signature("cefaedfe", "application/x-mach-binary"),
            signature("cffaedfe", "application/x-mach-binary"),
            asciiSignature("MZ", "application/vnd.microsoft.portable-executable"),
            asciiSignature("wOFF", "font/woff"),
            asciiSignature("wOF2", "font/woff2"),
            asciiSignature("OTTO", "font/otf"),
            signature("00010000", "font/ttf")
    );

    /// ZIP local-file-header signature.
    private static final byte @Unmodifiable [] ZIP_SIGNATURE = hex("504b0304");

    /// Maximum ZIP prefix scanned for subtype evidence.
    private static final int ZIP_SCAN_LIMIT = 4096;

    /// OpenDocument MIME strings accepted from an uncompressed `mimetype` entry.
    private static final @Unmodifiable Set<String> OPENDOCUMENT_MIMES = Set.of(
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.presentation",
            "application/vnd.oasis.opendocument.graphics"
    );

    /// TAR signature offset.
    private static final int TAR_OFFSET = 257;

    /// Prevents instantiation of this static detector.
    private MagicDetector() {
    }

    /// Returns a binary result when a known signature is present.
    ///
    /// @param data bytes to inspect
    /// @return definitive binary result, or `null`
    static @Nullable PipelineResult detect(byte @Unmodifiable [] data) {
        if (data.length == 0) {
            return null;
        }

        if (data.length >= 12 && matchesAscii(data, 4, "ftyp")) {
            long boxSize = readUnsignedIntBigEndian(data, 0);
            if (boxSize >= 8 && boxSize <= data.length) {
                String brand = new String(data, 8, 4, StandardCharsets.ISO_8859_1);
                if (brand.equals("avif") || brand.equals("avis")) {
                    return result("image/avif");
                }
                if (brand.equals("heic") || brand.equals("heix")) {
                    return result("image/heic");
                }
                if (brand.equals("mif1") || brand.equals("msf1")) {
                    return result("image/heif");
                }
                if (brand.equals("M4A ") || brand.equals("M4B ") || brand.equals("F4A ")) {
                    return result("audio/mp4");
                }
                if (brand.equals("qt  ")) {
                    return result("video/quicktime");
                }
                return result("video/mp4");
            }
        }

        if (data.length >= 12 && matchesAscii(data, 0, "RIFF")) {
            if (matchesAscii(data, 8, "WEBP")) {
                return result("image/webp");
            }
            if (matchesAscii(data, 8, "WAVE")) {
                return result("audio/wav");
            }
            if (matchesAscii(data, 8, "AVI ")) {
                return result("video/x-msvideo");
            }
        }

        if (data.length >= 12 && matchesAscii(data, 0, "FORM")) {
            if (matchesAscii(data, 8, "AIFF") || matchesAscii(data, 8, "AIFC")) {
                return result("audio/aiff");
            }
        }

        if (startsWith(data, ZIP_SIGNATURE)) {
            return result(classifyZip(data));
        }

        if (data.length >= 8 && startsWith(data, hex("cafebabe"))) {
            long architectureCount = readUnsignedIntBigEndian(data, 4);
            return result(architectureCount <= 20
                    ? "application/x-mach-binary"
                    : "application/java-vm");
        }

        for (Signature signature : SIGNATURES) {
            if (startsWith(data, signature.prefix())) {
                return result(signature.mimeType());
            }
        }

        if (data.length >= TAR_OFFSET + 6
                && (matchesAscii(data, TAR_OFFSET, "ustar\u0000")
                || matchesAscii(data, TAR_OFFSET, "ustar "))) {
            return result("application/x-tar");
        }
        return null;
    }

    /// Classifies a ZIP container from local entry headers in its first 4 KiB.
    ///
    /// @param data complete input prefix
    /// @return subtype MIME type or `application/zip`
    private static String classifyZip(byte @Unmodifiable [] data) {
        int scanLength = Math.min(data.length, ZIP_SCAN_LIMIT);
        int offset = 0;
        while (true) {
            int header = indexOf(data, ZIP_SIGNATURE, offset, scanLength);
            if (header < 0 || scanLength < header + 30) {
                break;
            }
            int nameLength = readUnsignedShortLittleEndian(data, header + 26);
            int extraLength = readUnsignedShortLittleEndian(data, header + 28);
            int nameStart = header + 30;
            if (scanLength < nameStart + nameLength) {
                break;
            }

            if (startsWithAscii(data, nameStart, nameLength, "xl/")) {
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            }
            if (startsWithAscii(data, nameStart, nameLength, "word/")) {
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }
            if (startsWithAscii(data, nameStart, nameLength, "ppt/")) {
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            }
            if (equalsAscii(data, nameStart, nameLength, "META-INF/MANIFEST.MF")) {
                return "application/java-archive";
            }
            if (equalsAscii(data, nameStart, nameLength, "AndroidManifest.xml")) {
                return "application/vnd.android.package-archive";
            }
            if (equalsAscii(data, nameStart, nameLength, "META-INF/container.xml")) {
                return "application/epub+zip";
            }
            if (containsAscii(data, nameStart, nameLength, ".dist-info/")) {
                return "application/x-wheel+zip";
            }

            if (equalsAscii(data, nameStart, nameLength, "mimetype")
                    && readUnsignedShortLittleEndian(data, header + 8) == 0) {
                long contentLength = readUnsignedIntLittleEndian(data, header + 22);
                long contentStart = (long) nameStart + nameLength + extraLength;
                if (contentStart + contentLength <= scanLength) {
                    String mime = new String(
                            data,
                            (int) contentStart,
                            (int) contentLength,
                            StandardCharsets.US_ASCII
                    );
                    if (OPENDOCUMENT_MIMES.contains(mime)) {
                        return mime;
                    }
                }
            }

            int flags = readUnsignedShortLittleEndian(data, header + 6);
            long contentSize = (flags & 0x0008) != 0
                    ? 0L
                    : readUnsignedIntLittleEndian(data, header + 18);
            long next = (long) nameStart + nameLength + extraLength + contentSize;
            if (next > Integer.MAX_VALUE) {
                break;
            }
            offset = (int) next;
        }
        return "application/zip";
    }

    /// Creates a definitive binary result.
    ///
    /// @param mimeType detected MIME type
    /// @return result with no encoding and confidence one
    private static PipelineResult result(String mimeType) {
        return new PipelineResult(null, 1.0, null, mimeType);
    }

    /// Creates a fixed signature from hexadecimal bytes.
    ///
    /// @param value    hexadecimal prefix
    /// @param mimeType MIME type
    /// @return signature
    private static Signature signature(String value, String mimeType) {
        return new Signature(hex(value), mimeType);
    }

    /// Creates a fixed ASCII signature.
    ///
    /// @param value    ASCII prefix
    /// @param mimeType MIME type
    /// @return signature
    private static Signature asciiSignature(String value, String mimeType) {
        return new Signature(asciiBytes(value), mimeType);
    }

    /// Parses hexadecimal bytes.
    ///
    /// @param value hexadecimal text
    /// @return parsed bytes
    private static byte @Unmodifiable [] hex(String value) {
        return java.util.HexFormat.of().parseHex(value);
    }

    /// Encodes a byte-preserving ASCII constant.
    ///
    /// @param value constant text
    /// @return encoded bytes
    private static byte @Unmodifiable [] asciiBytes(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }

    /// Tests an offset-zero byte prefix.
    ///
    /// @param data   source bytes
    /// @param prefix required prefix
    /// @return whether it matches
    private static boolean startsWith(
            byte @Unmodifiable [] data,
            byte @Unmodifiable [] prefix
    ) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (data[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    /// Tests ASCII text at an exact offset.
    ///
    /// @param data     source bytes
    /// @param offset   first byte
    /// @param expected expected byte-preserving text
    /// @return whether it matches
    private static boolean matchesAscii(
            byte @Unmodifiable [] data,
            int offset,
            String expected
    ) {
        if (offset < 0 || data.length - offset < expected.length()) {
            return false;
        }
        for (int index = 0; index < expected.length(); index++) {
            if (Byte.toUnsignedInt(data[offset + index]) != expected.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    /// Tests whether a bounded filename starts with ASCII text.
    ///
    /// @param data     source bytes
    /// @param offset   filename offset
    /// @param length   filename length
    /// @param expected expected prefix
    /// @return whether it matches
    private static boolean startsWithAscii(
            byte @Unmodifiable [] data,
            int offset,
            int length,
            String expected
    ) {
        return length >= expected.length() && matchesAscii(data, offset, expected);
    }

    /// Tests whether a bounded filename equals ASCII text.
    ///
    /// @param data     source bytes
    /// @param offset   filename offset
    /// @param length   filename length
    /// @param expected expected name
    /// @return whether it matches
    private static boolean equalsAscii(
            byte @Unmodifiable [] data,
            int offset,
            int length,
            String expected
    ) {
        return length == expected.length() && matchesAscii(data, offset, expected);
    }

    /// Tests whether a bounded filename contains ASCII text.
    ///
    /// @param data     source bytes
    /// @param offset   filename offset
    /// @param length   filename length
    /// @param expected expected fragment
    /// @return whether it occurs
    private static boolean containsAscii(
            byte @Unmodifiable [] data,
            int offset,
            int length,
            String expected
    ) {
        int limit = offset + length - expected.length();
        for (int index = offset; index <= limit; index++) {
            if (matchesAscii(data, index, expected)) {
                return true;
            }
        }
        return false;
    }

    /// Finds a byte pattern within a bounded source prefix.
    ///
    /// @param data    source bytes
    /// @param pattern pattern bytes
    /// @param start   first candidate index
    /// @param limit   exclusive source limit
    /// @return first index, or `-1`
    private static int indexOf(
            byte @Unmodifiable [] data,
            byte @Unmodifiable [] pattern,
            int start,
            int limit
    ) {
        int last = limit - pattern.length;
        for (int index = Math.max(0, start); index <= last; index++) {
            boolean match = true;
            for (int patternIndex = 0; patternIndex < pattern.length; patternIndex++) {
                if (data[index + patternIndex] != pattern[patternIndex]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return index;
            }
        }
        return -1;
    }

    /// Reads an unsigned little-endian 16-bit integer.
    ///
    /// @param data   source bytes
    /// @param offset first byte
    /// @return unsigned value
    private static int readUnsignedShortLittleEndian(byte @Unmodifiable [] data, int offset) {
        return Byte.toUnsignedInt(data[offset]) | (Byte.toUnsignedInt(data[offset + 1]) << 8);
    }

    /// Reads an unsigned little-endian 32-bit integer.
    ///
    /// @param data   source bytes
    /// @param offset first byte
    /// @return unsigned value
    private static long readUnsignedIntLittleEndian(byte @Unmodifiable [] data, int offset) {
        int value = Byte.toUnsignedInt(data[offset])
                | (Byte.toUnsignedInt(data[offset + 1]) << 8)
                | (Byte.toUnsignedInt(data[offset + 2]) << 16)
                | (Byte.toUnsignedInt(data[offset + 3]) << 24);
        return Integer.toUnsignedLong(value);
    }

    /// Reads an unsigned big-endian 32-bit integer.
    ///
    /// @param data   source bytes
    /// @param offset first byte
    /// @return unsigned value
    private static long readUnsignedIntBigEndian(byte @Unmodifiable [] data, int offset) {
        int value = (Byte.toUnsignedInt(data[offset]) << 24)
                | (Byte.toUnsignedInt(data[offset + 1]) << 16)
                | (Byte.toUnsignedInt(data[offset + 2]) << 8)
                | Byte.toUnsignedInt(data[offset + 3]);
        return Integer.toUnsignedLong(value);
    }

    /// Holds one fixed magic signature.
    ///
    /// @param prefix   immutable offset-zero byte prefix
    /// @param mimeType MIME type returned on a match
    @NotNullByDefault
    private record Signature(byte @Unmodifiable [] prefix, String mimeType) {
        /// Creates a fixed signature.
        private Signature {
        }
    }
}
