/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Centralizes visualization export so PNG, JPEG, SVG, and PDF share the same capture/rendering
 * pipeline instead of four separate copies of chart-export logic. On-screen rendering, PNG/JPEG
 * raster capture, and SVG vector export all draw the same {@link ChartDataset} through the same
 * {@link ChartRenderer}/{@link ChartDrawing} code, so exported output cannot visually drift from
 * what is shown in {@link ChartCanvas}. Matrix/Pivot and Heatmap exports always capture the
 * entire matrix content, not just the currently visible scrolled viewport.
 */
public final class VisualizationExportService {
    /**
     * Raster resolution multiplier used for the PDF export's embedded image, so the printed
     * output is noticeably sharper than a plain 1x screen capture (roughly 144 DPI against the
     * on-screen 72-DPI-equivalent page size below) without needing true vector PDF content.
     */
    private static final int PDF_RESOLUTION_SCALE = 2;

    private VisualizationExportService() {
    }

    /** PNG bytes of the current chart/matrix's full content (not just the visible viewport). */
    public static byte[] pngBytes(ChartCanvas canvas) {
        Image image = canvas.captureFullImage();
        try {
            return encode(image, SWT.IMAGE_PNG);
        } finally {
            image.dispose();
        }
    }

    /**
     * JPEG bytes of the current chart/matrix's full content. The rendered image already has an
     * opaque background (every renderer fills its background before drawing), so JPEG's lack of
     * transparency support has no visible effect: there is no alpha channel to flatten.
     */
    public static byte[] jpegBytes(ChartCanvas canvas) {
        Image image = canvas.captureFullImage();
        try {
            return encode(image, SWT.IMAGE_JPEG);
        } finally {
            image.dispose();
        }
    }

    /** Real vector SVG document (not a screenshot) of the current chart/matrix's full content. */
    public static byte[] svgBytes(ChartCanvas canvas) {
        return canvas.renderToSvg().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Single-page PDF document containing a high-resolution raster of the current chart/matrix's
     * full content, sized and scaled to preserve the visualization's aspect ratio with no
     * clipping. This is a documented raster-in-PDF fallback: true vector PDF content would
     * require reimplementing every {@link ChartRenderer} against a PDF content-stream graphics
     * abstraction, which is a disproportionate increase in packaging complexity for this
     * release.
     */
    public static byte[] pdfBytes(ChartCanvas canvas) {
        Rectangle content = canvas.contentSize();
        Image highResolution = canvas.renderToImage(
                content.width * PDF_RESOLUTION_SCALE, content.height * PDF_RESOLUTION_SCALE);
        try {
            byte[] jpeg = encode(highResolution, SWT.IMAGE_JPEG);
            return com.brianwalker.dbeaver.resultsvisualizer.services.PdfImageWriter.wrapJpeg(
                    jpeg, content.width * PDF_RESOLUTION_SCALE, content.height * PDF_RESOLUTION_SCALE,
                    content.width, content.height);
        } finally {
            highResolution.dispose();
        }
    }

    private static byte[] encode(Image image, int swtImageFormat) {
        ImageLoader loader = new ImageLoader();
        loader.data = new ImageData[] { image.getImageData() };
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        loader.save(out, swtImageFormat);
        return out.toByteArray();
    }

    /** Export formats offered by the Export dropdown, with their default file extension. */
    public enum ExportFormat {
        PNG("png", "PNG image", new String[] { "*.png" }),
        JPEG("jpg", "JPEG image", new String[] { "*.jpg", "*.jpeg" }),
        SVG("svg", "SVG image", new String[] { "*.svg" }),
        PDF("pdf", "PDF document", new String[] { "*.pdf" });

        private final String extension;
        private final String description;
        private final String[] filterExtensions;

        ExportFormat(String extension, String description, String[] filterExtensions) {
            this.extension = extension;
            this.description = description;
            this.filterExtensions = filterExtensions;
        }

        public String extension() {
            return extension;
        }

        public String description() {
            return description;
        }

        public String[] filterExtensions() {
            return filterExtensions.clone();
        }

        public String defaultFileName() {
            return "results-visualizer-chart." + extension;
        }
    }
}
