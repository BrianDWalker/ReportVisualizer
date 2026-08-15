/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes a minimal, dependency-free single-page PDF that embeds a JPEG image as a raster
 * {@code XObject}. No PDF/graphics library is used: this hand-written writer has no network
 * access, no scripting engine, and no native/process dependency, keeping the export path fully
 * local and auditable.
 */
public final class PdfImageWriter {
    private PdfImageWriter() {
    }

    /**
     * Wraps {@code jpegBytes} ({@code pixelWidth}x{@code pixelHeight}) as the sole content of a
     * one-page PDF sized {@code pageWidthPoints}x{@code pageHeightPoints} (in PDF points, 1/72
     * inch). The image is scaled to exactly fill the page, so the page and image should already
     * share the same aspect ratio if the caller wants an undistorted result.
     */
    public static byte[] wrapJpeg(byte[] jpegBytes, int pixelWidth, int pixelHeight,
            double pageWidthPoints, double pageHeightPoints) {
        if (jpegBytes == null || jpegBytes.length == 0) {
            throw new IllegalArgumentException("JPEG image data must not be empty.");
        }
        if (pixelWidth <= 0 || pixelHeight <= 0) {
            throw new IllegalArgumentException("Image pixel dimensions must be positive.");
        }
        double width = pageWidthPoints > 0 ? pageWidthPoints : pixelWidth;
        double height = pageHeightPoints > 0 ? pageHeightPoints : pixelHeight;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<Integer> offsets = new ArrayList<>();

            write(out, "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");

            offsets.add(out.size());
            write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            offsets.add(out.size());
            write(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

            offsets.add(out.size());
            write(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                    + format(width) + " " + format(height) + "] "
                    + "/Resources << /XObject << /Im0 4 0 R >> >> /Contents 5 0 R >>\nendobj\n");

            offsets.add(out.size());
            write(out, "4 0 obj\n<< /Type /XObject /Subtype /Image /Width " + pixelWidth
                    + " /Height " + pixelHeight + " /ColorSpace /DeviceRGB /BitsPerComponent 8 "
                    + "/Filter /DCTDecode /Length " + jpegBytes.length + " >>\nstream\n");
            out.write(jpegBytes);
            write(out, "\nendstream\nendobj\n");

            offsets.add(out.size());
            String content = "q " + format(width) + " 0 0 " + format(height) + " 0 0 cm /Im0 Do Q";
            byte[] contentBytes = content.getBytes(StandardCharsets.ISO_8859_1);
            write(out, "5 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n");
            out.write(contentBytes);
            write(out, "\nendstream\nendobj\n");

            int xrefOffset = out.size();
            write(out, "xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
            for (int offset : offsets) {
                write(out, String.format(Locale.ROOT, "%010d 00000 n \n", offset));
            }
            write(out, "trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\n"
                    + "startxref\n" + xrefOffset + "\n%%EOF");
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build PDF output.", e);
        }
    }

    private static void write(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
