// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet;

import kala.encdet.EncodingDetector.Encoding;
import kala.encdet.EncodingDetector.Candidate;
import kala.encdet.EncodingDetector.Result;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies pipeline ordering and regressions not represented by ordinary corpus files.
@NotNullByDefault
final class PipelineEdgeCaseTest {
    /// Verifies overlapping UTF-32 and UTF-16 BOMs use the reference alignment rule.
    @Test
    void handlesOverlappingBomPrefixes() {
        assertEquals(
                Encoding.UTF_32,
                EncodingDetector.DEFAULT.detect(
                        new byte[]{(byte) 0xff, (byte) 0xfe, 0, 0}
                ).bestEncoding()
        );
        assertEquals(
                Encoding.UTF_16,
                EncodingDetector.DEFAULT.detect(
                        new byte[]{(byte) 0xff, (byte) 0xfe, 0, 0, 'x'}
                ).bestEncoding()
        );
        assertNull(
                EncodingDetector.DEFAULT.detect(
                        new byte[]{0, 0, (byte) 0xfe, (byte) 0xff, 'x'}
                ).bestEncoding()
        );
    }

    /// Verifies BOM-less endian-specific UTF-16 and UTF-32 pattern detection.
    @Test
    void detectsBomlessUnicodePatterns() {
        byte[] utf16le = "This is a sufficiently long UTF-16 sample.".getBytes(StandardCharsets.UTF_16LE);
        byte[] utf16be = "This is a sufficiently long UTF-16 sample.".getBytes(StandardCharsets.UTF_16BE);
        byte[] utf32le = encodeUtf32("UTF-32 little endian sample", ByteOrder.LITTLE_ENDIAN);
        byte[] utf32be = encodeUtf32("UTF-32 big endian sample", ByteOrder.BIG_ENDIAN);
        assertEquals(
                Encoding.UTF_16_LE,
                EncodingDetector.DEFAULT.detect(utf16le).bestEncoding()
        );
        assertEquals(
                Encoding.UTF_16_BE,
                EncodingDetector.DEFAULT.detect(utf16be).bestEncoding()
        );
        assertEquals(
                EncodingDetector.DEFAULT.detect(utf16le),
                EncodingDetector.DEFAULT.detect(directView(utf16le))
        );
        assertEquals(
                EncodingDetector.DEFAULT.detect(utf16be),
                EncodingDetector.DEFAULT.detect(readOnlyView(utf16be))
        );
        assertEquals(
                Encoding.UTF_32_LE,
                EncodingDetector.DEFAULT.detect(utf32le).bestEncoding()
        );
        assertEquals(
                Encoding.UTF_32_BE,
                EncodingDetector.DEFAULT.detect(utf32be).bestEncoding()
        );
        assertEquals(
                EncodingDetector.DEFAULT.detect(utf32le),
                EncodingDetector.DEFAULT.detect(directView(utf32le))
        );
        assertEquals(
                EncodingDetector.DEFAULT.detect(utf32be),
                EncodingDetector.DEFAULT.detect(readOnlyView(utf32be))
        );
    }

    /// Verifies sparse null separators remain ASCII while dense null data is binary.
    @Test
    void distinguishesSparseNullTextFromBinaryData() {
        byte[] sparse = "alpha beta gamma delta".getBytes(StandardCharsets.US_ASCII);
        sparse[10] = 0;
        Candidate ascii = requireBestCandidate(EncodingDetector.DEFAULT.detect(sparse));
        assertEquals(Encoding.ASCII, ascii.encoding());
        assertEquals(0.99, ascii.confidence());

        Result binaryResult = EncodingDetector.DEFAULT.detect(new byte[100]);
        Candidate binary = requireBestCandidate(binaryResult);
        assertNull(binary.encoding());
        assertEquals("application/octet-stream", binary.mimeType());
        assertEquals(0.95, binary.confidence());
        assertNull(binaryResult.bestEncoding());

        Result filteredBinary = EncodingDetector.DEFAULT
                .withMinimumConfidence(1.0)
                .withNoMatchEncoding(Encoding.ASCII)
                .detect(new byte[100]);
        assertTrue(filteredBinary.candidates().isEmpty());
        assertNull(filteredBinary.bestCandidate());
        assertNull(filteredBinary.bestEncoding());
    }

    /// Verifies truncated UTF-8 is accepted only after a complete multibyte sequence.
    @Test
    void toleratesTrailingTruncatedUtf8() {
        byte[] complete = "éx€".getBytes(StandardCharsets.UTF_8);
        byte[] truncated = java.util.Arrays.copyOf(complete, complete.length - 1);
        assertEquals(
                Encoding.UTF_8,
                EncodingDetector.DEFAULT.detect(truncated).bestEncoding()
        );
        assertNotEquals(
                Encoding.UTF_8,
                EncodingDetector.DEFAULT.detect(
                        new byte[]{(byte) 0xe2}
                ).bestEncoding()
        );
    }

    /// Verifies valid escape encodings and common false UTF-7 patterns.
    @Test
    void detectsEscapeEncodingsWithoutUtf7FalsePositives() {
        assertEquals(
                Encoding.HZ,
                EncodingDetector.DEFAULT.detect(
                        "Hello ~{CEDE~} World".getBytes(StandardCharsets.US_ASCII)
                ).bestEncoding()
        );
        assertEquals(
                Encoding.ISO_2022_KR,
                EncodingDetector.DEFAULT.detect(
                        new byte[]{0x1b, '$', ')', 'C', 0x0e, '!', '!', 0x0f}
                ).bestEncoding()
        );
        assertEquals(
                Encoding.UTF_7,
                EncodingDetector.DEFAULT.detect(
                        "Hello +ThZ1TA-".getBytes(StandardCharsets.US_ASCII)
                ).bestEncoding()
        );
        assertEquals(
                Encoding.ASCII,
                EncodingDetector.DEFAULT.detect(
                        "C++20 and +100 are ASCII".getBytes(StandardCharsets.US_ASCII)
                ).bestEncoding()
        );
        assertEquals(
                Encoding.ASCII,
                EncodingDetector.DEFAULT.detect(
                        "+4bafdea31b1a83b6eff5dac6cedcff073cb984f6"
                                .getBytes(StandardCharsets.US_ASCII)
                ).bestEncoding()
        );
    }

    /// Verifies XML, HTML, and PEP 263 declarations precede text prechecks.
    @Test
    void honorsValidDeclarationsAndIgnoresInvalidOnes() {
        EncodingDetector detector = EncodingDetector.DEFAULT;
        byte[] xmlData = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?><root/>"
                .getBytes(StandardCharsets.US_ASCII);
        Result xmlResult = detector.detect(directView(xmlData));
        Candidate xml = requireBestCandidate(xmlResult);
        assertEquals(detector.detect(xmlData), xmlResult);
        assertEquals(Encoding.ISO_8859_1, xml.encoding());
        assertEquals("text/xml", xml.mimeType());
        assertEquals(0.95, xml.confidence());

        byte[] htmlData = "<meta charset=\"utf-8\"><p>Hello</p>"
                .getBytes(StandardCharsets.US_ASCII);
        Result htmlResult = detector.detect(readOnlyView(htmlData));
        Candidate html = requireBestCandidate(htmlResult);
        assertEquals(detector.detect(htmlData), htmlResult);
        assertEquals(Encoding.UTF_8, html.encoding());
        assertEquals("text/html", html.mimeType());

        byte[] pepData = "#!/usr/bin/env python\n# -*- coding: iso-8859-1 -*-\nx='é'\n"
                .getBytes(StandardCharsets.ISO_8859_1);
        Result pepResult = detector.detect(directView(pepData));
        Candidate pep = requireBestCandidate(pepResult);
        assertEquals(detector.detect(pepData), pepResult);
        assertEquals(Encoding.ISO_8859_1, pep.encoding());
        assertEquals("text/x-python", pep.mimeType());

        assertEquals(
                Encoding.ASCII,
                EncodingDetector.DEFAULT.detect(
                        "<meta charset=\"\0utf-8\">".getBytes(StandardCharsets.ISO_8859_1)
                ).bestEncoding()
        );
        assertEquals(
                Encoding.ASCII,
                EncodingDetector.DEFAULT.detect(
                        "# first\n# second\n# coding: iso-8859-1\n"
                                .getBytes(StandardCharsets.US_ASCII)
                ).bestEncoding()
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
    /// @return encoded bytes
    private static byte[] encodeUtf32(String value, ByteOrder order) {
        int[] codePoints = value.codePoints().toArray();
        ByteBuffer buffer = ByteBuffer.allocate(codePoints.length * Integer.BYTES).order(order);
        for (int codePoint : codePoints) {
            buffer.putInt(codePoint);
        }
        return buffer.array();
    }

    /// Creates a direct buffer whose remaining region contains the supplied bytes.
    ///
    /// @param data bytes to expose
    /// @return mutable direct buffer with a nonzero position
    private static ByteBuffer directView(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(data.length + 2);
        buffer.put((byte) 0x55).put(data).put((byte) 0x66);
        buffer.position(1).limit(data.length + 1);
        return buffer;
    }

    /// Creates a read-only buffer whose remaining region contains the supplied bytes.
    ///
    /// @param data bytes to expose
    /// @return read-only buffer with a nonzero position
    private static @UnmodifiableView ByteBuffer readOnlyView(byte[] data) {
        byte[] framed = new byte[data.length + 2];
        framed[0] = 0x55;
        System.arraycopy(data, 0, framed, 1, data.length);
        framed[framed.length - 1] = 0x66;
        ByteBuffer buffer = ByteBuffer.wrap(framed);
        buffer.position(1).limit(data.length + 1);
        return buffer.asReadOnlyBuffer();
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
        Result result = EncodingDetector.DEFAULT.detect(data);
        Candidate candidate = requireBestCandidate(result);
        assertNull(candidate.encoding());
        assertEquals(1.0, candidate.confidence());
        assertEquals(mimeType, candidate.mimeType());
        assertEquals(result, EncodingDetector.DEFAULT.detect(directView(data)));
        assertEquals(result, EncodingDetector.DEFAULT.detect(readOnlyView(data)));
    }

    /// Returns the highest-ranked candidate or fails the current test.
    ///
    /// @param result detection result expected to contain a candidate
    /// @return highest-ranked candidate
    private static Candidate requireBestCandidate(Result result) {
        @Nullable Candidate candidate = result.bestCandidate();
        if (candidate == null) {
            throw new AssertionError("Expected a detection candidate");
        }
        return candidate;
    }
}
