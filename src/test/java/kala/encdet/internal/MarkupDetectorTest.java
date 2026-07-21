// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package kala.encdet.internal;

import kala.encdet.EncodingDetector.Encoding;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Verifies direct byte-buffer scanning of XML, HTML, and PEP 263 declarations.
@NotNullByDefault
final class MarkupDetectorTest {
    /// Verifies XML declarations support case folding, quoting, and whitespace.
    @Test
    void detectsXmlDeclarations() {
        assertDeclaration(
                "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?><root/>",
                Encoding.ISO_8859_1,
                "text/xml"
        );
        assertDeclaration(
                "<?XML version='1.0' encoding \u000b=\r 'shift_jis'?><root/>",
                Encoding.SHIFT_JIS_2004,
                "text/xml"
        );
    }

    /// Verifies HTML5 and content-type declarations retain their accepted forms.
    @Test
    void detectsHtmlDeclarations() {
        assertDeclaration(
                "<META CHARSET = \"UTF-8\"><p>Hello</p>",
                Encoding.UTF_8,
                "text/html"
        );
        assertDeclaration(
                "<meta http-equiv='Content-Type' "
                        + "content='text/html; charset=windows-1252'>",
                Encoding.CP1252,
                "text/html"
        );
        assertDeclaration(
                "<meta x charset=\u000butf-8;>",
                Encoding.UTF_8,
                "text/html"
        );
    }

    /// Verifies greedy attributes choose the last syntactically valid candidate.
    @Test
    void preservesGreedyAttributeSelectionAndBacktracking() {
        assertDeclaration(
                "<?xml x encoding='utf-8' y encoding='windows-1252'?>",
                Encoding.CP1252,
                "text/xml"
        );
        assertDeclaration(
                "<meta x charset=utf-8 y charset=windows-1252>",
                Encoding.CP1252,
                "text/html"
        );
        assertDeclaration(
                "<meta x charset=utf-8 y charset=>",
                Encoding.UTF_8,
                "text/html"
        );
        assertDeclaration(
                "<meta x content='text/plain; charset=utf-8; charset=windows-1252'>",
                Encoding.CP1252,
                "text/html"
        );
    }

    /// Verifies one syntactic match is not silently replaced by a later match.
    @Test
    void retainsFirstMatchFailureSemantics() {
        assertNull(detect("<meta x charset=not-real><meta x charset=utf-8>"));
        assertNull(detect(
                "<?xml x encoding='not-real'?><?xml x encoding='utf-8'?>"
        ));
    }

    /// Verifies the 4 KiB markup scan limit.
    @Test
    void respectsMarkupScanLimit() {
        assertDeclaration(
                "x".repeat(4000) + "<meta charset='utf-8'>",
                Encoding.UTF_8,
                "text/html"
        );
        assertNull(detect("x".repeat(4096) + "<meta charset='utf-8'>"));
    }

    /// Verifies unknown, non-ASCII, null-containing, and lying names are rejected.
    @Test
    void rejectsInvalidDeclarations() {
        assertNull(detect("<meta charset='not-a-real-encoding'>"));
        assertNull(detect("<meta charset='\u00ff\u00fe'>"));
        assertNull(detect("<meta charset='\0utf-8'>"));
        assertNull(MarkupDetector.detect(ByteBufferSupport.wrap(
                "<meta charset='shift_jis'>日本語テスト".getBytes(StandardCharsets.UTF_8)
        )));
    }

    /// Verifies PEP 263 scanning is case-sensitive and limited to two LF lines.
    @Test
    void detectsPep263Declarations() {
        assertDeclaration("# coding: utf-8\n", Encoding.UTF_8, "text/x-python");
        assertDeclaration(
                "#!/usr/bin/env python\n# -*- coding=iso-8859-1 -*-\n",
                Encoding.ISO_8859_1,
                "text/x-python"
        );
        assertDeclaration(
                "# coding: utf-8! trailing text\n",
                Encoding.UTF_8,
                "text/x-python"
        );
        assertNull(detect("# first\n# second\n# coding: utf-8\n"));
        assertNull(detect("# Coding: utf-8\n"));
        assertNull(detect("x\r# coding: utf-8\n"));
        assertNull(detect("# coding: \u00ff\u00fe\n"));
        assertNull(detect(" ".repeat(201) + "# coding: utf-8\n"));
    }

    /// Verifies scanning a direct-buffer view preserves caller buffer state.
    @Test
    void scansDirectBufferViewsWithoutConsumingInput() {
        byte[] data = "<meta charset='utf-8'>".getBytes(StandardCharsets.US_ASCII);
        ByteBuffer source = ByteBuffer.allocateDirect(data.length + 4);
        source.put((byte) 1).put((byte) 2).put(data).put((byte) 3).put((byte) 4);
        source.position(2).limit(2 + data.length);
        source.mark();

        @Nullable PipelineResult result = MarkupDetector.detect(ByteBufferSupport.view(source));
        assertNotNull(result);
        assertEquals(Encoding.UTF_8, result.encoding());
        assertEquals(2, source.position());
        assertEquals(2 + data.length, source.limit());
        source.reset();
        assertEquals(2, source.position());
    }

    /// Detects one ISO-8859-1 fixture through a normalized buffer view.
    ///
    /// @param value byte-preserving fixture text
    /// @return detected declaration, or `null`
    private static @Nullable PipelineResult detect(String value) {
        return MarkupDetector.detect(ByteBufferSupport.wrap(
                value.getBytes(StandardCharsets.ISO_8859_1)
        ));
    }

    /// Verifies one declaration result.
    ///
    /// @param value        byte-preserving fixture text
    /// @param encoding     expected encoding
    /// @param expectedMime expected MIME type
    private static void assertDeclaration(
            String value,
            Encoding encoding,
            String expectedMime
    ) {
        @Nullable PipelineResult result = detect(value);
        assertNotNull(result);
        assertEquals(encoding, result.encoding());
        assertEquals(0.95, result.confidence());
        assertEquals(expectedMime, result.mimeType());
    }
}
