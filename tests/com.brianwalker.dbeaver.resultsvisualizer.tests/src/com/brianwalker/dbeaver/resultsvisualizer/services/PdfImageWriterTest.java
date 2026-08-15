/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.Test;

/**
 * Validates the hand-written minimal PDF wrapper used for chart PDF export: no PDF library is
 * used, so this test checks structural correctness of the raw bytes directly (header, embedded
 * JPEG stream via {@code DCTDecode}, xref table, trailer, and footer) rather than relying on a
 * PDF parser dependency.
 */
public class PdfImageWriterTest {

    @Test
    public void wrapsJpegIntoStructurallyValidSinglePagePdf() {
        byte[] fakeJpeg = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02, 0x03 };
        byte[] pdf = PdfImageWriter.wrapJpeg(fakeJpeg, 800, 600, 400, 300);
        String text = new String(pdf, StandardCharsets.ISO_8859_1);

        assertTrue("PDF must start with the %PDF header", text.startsWith("%PDF-1.4"));
        assertTrue("PDF must end with %%EOF", text.trim().endsWith("%%EOF"));
        assertTrue("PDF must declare a Catalog", text.contains("/Type /Catalog"));
        assertTrue("PDF must declare exactly one page", text.contains("/Type /Page ") || text.contains("/Type /Page\n") || text.contains("/Type /Page/"));
        assertTrue("PDF must embed the image via DCTDecode (raw JPEG passthrough)",
                text.contains("/Filter /DCTDecode"));
        assertTrue("PDF must contain a stream/endstream pair for the image", text.contains("stream") && text.contains("endstream"));
        assertTrue("PDF must contain an xref table", text.contains("xref"));
        assertTrue("PDF must contain a trailer with a Root reference", text.contains("trailer") && text.contains("/Root 1 0 R"));
        assertTrue("PDF must declare the requested page size in points",
                text.contains("[0 0 400.00 300.00]"));

        // The embedded JPEG bytes must appear verbatim (uncompressed passthrough, not re-encoded).
        int index = indexOf(pdf, fakeJpeg);
        assertTrue("Embedded JPEG bytes must be present verbatim in the PDF stream", index >= 0);
    }

    @Test
    public void rejectsEmptyImageData() {
        assertThrows(IllegalArgumentException.class,
                () -> PdfImageWriter.wrapJpeg(new byte[0], 100, 100, 100, 100));
    }

    @Test
    public void rejectsNonPositivePixelDimensions() {
        assertThrows(IllegalArgumentException.class,
                () -> PdfImageWriter.wrapJpeg(new byte[] { 1, 2, 3 }, 0, 100, 100, 100));
    }

    @Test
    public void fallsBackToPixelDimensionsWhenPageSizeNotPositive() {
        byte[] fakeJpeg = new byte[] { 1, 2, 3, 4 };
        byte[] pdf = PdfImageWriter.wrapJpeg(fakeJpeg, 200, 150, 0, 0);
        String text = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(text.contains("[0 0 200.00 150.00]"));
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
