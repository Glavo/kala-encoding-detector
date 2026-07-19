// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Verifies pipeline ordering and regressions not represented by ordinary corpus files.
@NotNullByDefault
final class PipelineEdgeCaseTest {
    /// Verifies overlapping UTF-32 and UTF-16 BOMs use the reference alignment rule.
    @Test
    void handlesOverlappingBomPrefixes() {
        assertEquals(
                "UTF-32",
                EncodingDetector.DEFAULT.detect(new byte[]{(byte) 0xff, (byte) 0xfe, 0, 0}).encoding()
        );
        assertEquals(
                "UTF-16",
                EncodingDetector.DEFAULT.detect(new byte[]{(byte) 0xff, (byte) 0xfe, 0, 0, 'x'}).encoding()
        );
        assertNull(
                EncodingDetector.DEFAULT.detect(new byte[]{0, 0, (byte) 0xfe, (byte) 0xff, 'x'}).encoding()
        );
    }

    /// Verifies BOM-less endian-specific UTF-16 and UTF-32 pattern detection.
    @Test
    void detectsBomlessUnicodePatterns() {
        byte[] utf16le = "This is a sufficiently long UTF-16 sample.".getBytes(StandardCharsets.UTF_16LE);
        byte[] utf16be = "This is a sufficiently long UTF-16 sample.".getBytes(StandardCharsets.UTF_16BE);
        ByteBuffer utf32le = encodeUtf32("UTF-32 little endian sample", ByteOrder.LITTLE_ENDIAN);
        ByteBuffer utf32be = encodeUtf32("UTF-32 big endian sample", ByteOrder.BIG_ENDIAN);
        assertEquals("utf-16-le", EncodingDetector.DEFAULT.detect(utf16le).encoding());
        assertEquals("utf-16-be", EncodingDetector.DEFAULT.detect(utf16be).encoding());
        assertEquals("utf-32-le", EncodingDetector.DEFAULT.detect(utf32le.array()).encoding());
        assertEquals("utf-32-be", EncodingDetector.DEFAULT.detect(utf32be.array()).encoding());
    }

    /// Verifies sparse null separators remain ASCII while dense null data is binary.
    @Test
    void distinguishesSparseNullTextFromBinaryData() {
        byte[] sparse = "alpha beta gamma delta".getBytes(StandardCharsets.US_ASCII);
        sparse[10] = 0;
        DetectionResult ascii = EncodingDetector.DEFAULT.detect(sparse);
        assertEquals("ascii", ascii.encoding());
        assertEquals(0.99, ascii.confidence());

        DetectionResult binary = EncodingDetector.DEFAULT.detect(new byte[100]);
        assertNull(binary.encoding());
        assertEquals("application/octet-stream", binary.mimeType());
        assertEquals(0.95, binary.confidence());
    }

    /// Verifies truncated UTF-8 is accepted only after a complete multibyte sequence.
    @Test
    void toleratesTrailingTruncatedUtf8() {
        byte[] complete = "éx€".getBytes(StandardCharsets.UTF_8);
        byte[] truncated = java.util.Arrays.copyOf(complete, complete.length - 1);
        assertEquals("utf-8", EncodingDetector.DEFAULT.detect(truncated).encoding());
        assertNotEquals(
                "utf-8",
                EncodingDetector.DEFAULT.detect(new byte[]{(byte) 0xe2}).encoding()
        );
    }

    /// Verifies valid escape encodings and common false UTF-7 patterns.
    @Test
    void detectsEscapeEncodingsWithoutUtf7FalsePositives() {
        assertEquals(
                "HZ-GB-2312",
                EncodingDetector.DEFAULT.detect("Hello ~{CEDE~} World".getBytes(StandardCharsets.US_ASCII))
                        .encoding()
        );
        assertEquals(
                "ISO-2022-KR",
                EncodingDetector.DEFAULT.detect(new byte[]{0x1b, '$', ')', 'C', 0x0e, '!', '!', 0x0f})
                        .encoding()
        );
        assertEquals(
                "utf-7",
                EncodingDetector.DEFAULT.detect("Hello +ThZ1TA-".getBytes(StandardCharsets.US_ASCII)).encoding()
        );
        assertEquals(
                "ascii",
                EncodingDetector.DEFAULT.detect("C++20 and +100 are ASCII".getBytes(StandardCharsets.US_ASCII))
                        .encoding()
        );
        assertEquals(
                "ascii",
                EncodingDetector.DEFAULT.detect(
                        "+4bafdea31b1a83b6eff5dac6cedcff073cb984f6"
                                .getBytes(StandardCharsets.US_ASCII)
                ).encoding()
        );
    }

    /// Verifies XML, HTML, and PEP 263 declarations precede text prechecks.
    @Test
    void honorsValidDeclarationsAndIgnoresInvalidOnes() {
        EncodingDetector canonical = EncodingDetector.DEFAULT
                .withNameStyle(EncodingNameStyle.CANONICAL);
        DetectionResult xml = canonical.detect(
                "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?><root/>"
                        .getBytes(StandardCharsets.US_ASCII)
        );
        assertEquals("iso8859-1", xml.encoding());
        assertEquals("text/xml", xml.mimeType());
        assertEquals(0.95, xml.confidence());

        DetectionResult html = canonical.detect(
                "<meta charset=\"utf-8\"><p>Hello</p>".getBytes(StandardCharsets.US_ASCII)
        );
        assertEquals("utf-8", html.encoding());
        assertEquals("text/html", html.mimeType());

        DetectionResult pep = canonical.detect(
                "#!/usr/bin/env python\n# -*- coding: iso-8859-1 -*-\nx='é'\n"
                        .getBytes(StandardCharsets.ISO_8859_1)
        );
        assertEquals("iso8859-1", pep.encoding());
        assertEquals("text/x-python", pep.mimeType());

        assertEquals(
                "ascii",
                EncodingDetector.DEFAULT.detect(
                        "<meta charset=\"\0utf-8\">".getBytes(StandardCharsets.ISO_8859_1)
                ).encoding()
        );
        assertEquals(
                "ascii",
                EncodingDetector.DEFAULT.detect(
                        "# first\n# second\n# coding: iso-8859-1\n"
                                .getBytes(StandardCharsets.US_ASCII)
                ).encoding()
        );
    }

    /// Verifies binary magic and ZIP subtype MIME classification.
    @Test
    void classifiesBinaryMagicAndZipSubtypes() {
        assertMagic(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a}, "image/png");
        assertMagic("%PDF-1.7".getBytes(StandardCharsets.US_ASCII), "application/pdf");
        assertMagic(zipEntry("xl/workbook.xml"),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertMagic(zipEntry("META-INF/MANIFEST.MF"), "application/java-archive");
        assertMagic(zipEntry("package-1.0.dist-info/WHEEL"), "application/x-wheel+zip");
        assertMagic(new byte[]{'P', 'K', 3, 4, 0, 0, 0, 0}, "application/zip");
    }

    /// Encodes a string into BOM-less UTF-32.
    ///
    /// @param value text to encode
    /// @param order requested byte order
    /// @return filled byte buffer
    private static ByteBuffer encodeUtf32(String value, ByteOrder order) {
        int[] codePoints = value.codePoints().toArray();
        ByteBuffer buffer = ByteBuffer.allocate(codePoints.length * Integer.BYTES).order(order);
        for (int codePoint : codePoints) {
            buffer.putInt(codePoint);
        }
        return buffer;
    }

    /// Creates one stored empty ZIP local entry.
    ///
    /// @param name entry name
    /// @return local header bytes
    private static byte[] zipEntry(String name) {
        byte[] encodedName = name.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(30 + encodedName.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(new byte[]{'P', 'K', 3, 4});
        buffer.putShort((short) 20);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putShort((short) encodedName.length);
        buffer.putShort((short) 0);
        buffer.put(encodedName);
        return buffer.array();
    }

    /// Asserts a definitive no-encoding MIME result.
    ///
    /// @param data     signature bytes
    /// @param mimeType expected MIME type
    private static void assertMagic(byte[] data, String mimeType) {
        DetectionResult result = EncodingDetector.DEFAULT.detect(data);
        assertNull(result.encoding());
        assertEquals(1.0, result.confidence());
        assertEquals(mimeType, result.mimeType());
    }
}
