/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/** Double-buffered SWT surface for local chart rendering. */
public final class ChartCanvas extends Canvas {
    private final ChartRendererRegistry registry;
    private ChartType chartType = ChartType.BAR;
    private ChartDataset dataset = new ChartDataset("", "", List.of());
    private String prompt;

    public ChartCanvas(Composite parent, int style, ChartRendererRegistry registry) {
        super(parent, style | SWT.DOUBLE_BUFFERED | SWT.H_SCROLL | SWT.V_SCROLL);
        this.registry = registry;
        addPaintListener(event -> paintChart(event.gc));
        addListener(SWT.Resize, event -> configureScrolling());
        getHorizontalBar().addListener(SWT.Selection, event -> redraw());
        getVerticalBar().addListener(SWT.Selection, event -> redraw());
    }

    public void setChart(ChartType chartType, ChartDataset dataset) {
        this.chartType = chartType;
        this.dataset = dataset;
        this.prompt = null;
        configureScrolling();
        redraw();
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
        getHorizontalBar().setVisible(false);
        getVerticalBar().setVisible(false);
        redraw();
    }

    /** Captures exactly what is currently visible on screen (the scrolled viewport). */
    public Image captureImage() {
        Rectangle bounds = getClientArea();
        int width = Math.max(1, bounds.width);
        int height = Math.max(1, bounds.height);
        Image image = new Image(getDisplay(), width, height);
        GC graphics = new GC(image);
        try {
            paintChart(graphics, bounds);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    /**
     * Captures the entire Matrix/Heatmap content at its full, unscrolled size rather than only
     * the currently visible scrolled viewport, so exporting a matrix larger than the on-screen
     * area does not silently produce a cropped image. For non-matrix chart types (which always
     * render their full content within the viewport already) this is equivalent to
     * {@link #captureImage()}.
     */
    public Image captureFullImage() {
        Rectangle content = contentSize();
        return renderToImage(content.width, content.height);
    }

    /**
     * Renders the current chart/matrix into a new {@code width}x{@code height} image using
     * absolute coordinates (no scrollbar offset), independent of the control's on-screen size.
     * Used for higher-resolution export (for example PDF) without duplicating layout logic.
     */
    public Image renderToImage(int width, int height) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        Image image = new Image(getDisplay(), safeWidth, safeHeight);
        GC graphics = new GC(image);
        try {
            Rectangle bounds = new Rectangle(0, 0, safeWidth, safeHeight);
            graphics.setAntialias(SWT.ON);
            graphics.setTextAntialias(SWT.ON);
            graphics.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
            graphics.fillRectangle(bounds);
            ChartGraphics chartGraphics = new SwtChartGraphics(graphics, ChartTheme.captureFrom(getDisplay()));
            if (prompt != null) {
                ChartDrawing.drawMessage(chartGraphics, bounds, prompt);
            } else {
                registry.renderer(chartType).render(chartGraphics, bounds, dataset);
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    /** The full, unscrolled content size that {@link #captureFullImage()} would render at. */
    public Rectangle contentSize() {
        Rectangle bounds = getClientArea();
        if (prompt != null || dataset == null || !MatrixCanvasMetrics.isMatrixLike(chartType)) {
            return new Rectangle(0, 0, Math.max(1, bounds.width), Math.max(1, bounds.height));
        }
        return new Rectangle(0, 0, Math.max(1, MatrixCanvasMetrics.width(dataset)),
                Math.max(1, MatrixCanvasMetrics.height(dataset)));
    }

    /**
     * Renders the full, unscrolled current chart/matrix as a real vector SVG document, using the
     * same {@link ChartRenderer}/{@link ChartDrawing} layout code as on-screen rendering.
     */
    public String renderToSvg() {
        Rectangle content = contentSize();
        ChartTheme theme = ChartTheme.captureFrom(getDisplay());
        SvgChartGraphics svg = new SvgChartGraphics(content.width, content.height, theme);
        if (prompt != null) {
            ChartDrawing.drawMessage(svg, content, prompt);
        } else {
            registry.renderer(chartType).render(svg, content, dataset);
        }
        return svg.toSvg();
    }

    private void paintChart(GC graphics) {
        paintChart(graphics, getClientArea());
    }

    private void paintChart(GC graphics, Rectangle bounds) {
        graphics.setAntialias(SWT.ON);
        graphics.setTextAntialias(SWT.ON);
        graphics.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        graphics.fillRectangle(bounds);
        ChartGraphics chartGraphics = new SwtChartGraphics(graphics, ChartTheme.captureFrom(getDisplay()));
        if (prompt != null) {
            ChartDrawing.drawMessage(chartGraphics, bounds, prompt);
            return;
        }
        if (MatrixCanvasMetrics.isMatrixLike(chartType)) {
            int width = Math.max(bounds.width, MatrixCanvasMetrics.width(dataset));
            int height = Math.max(bounds.height, MatrixCanvasMetrics.height(dataset));
            Transform transform = new Transform(getDisplay());
            try {
                transform.translate(-getHorizontalBar().getSelection(), -getVerticalBar().getSelection());
                graphics.setTransform(transform);
                registry.renderer(chartType).render(chartGraphics, new Rectangle(0, 0, width, height), dataset);
                graphics.setTransform(null);
            } finally {
                transform.dispose();
            }
        } else {
            registry.renderer(chartType).render(chartGraphics, bounds, dataset);
        }
    }

    private void configureScrolling() {
        boolean matrix = MatrixCanvasMetrics.isMatrixLike(chartType);
        getHorizontalBar().setVisible(matrix);
        getVerticalBar().setVisible(matrix);
        if (!matrix) return;
        int width = MatrixCanvasMetrics.width(dataset);
        int height = MatrixCanvasMetrics.height(dataset);
        getHorizontalBar().setMaximum(width);
        getHorizontalBar().setThumb(Math.min(width, Math.max(1, getClientArea().width)));
        getVerticalBar().setMaximum(height);
        getVerticalBar().setThumb(Math.min(height, Math.max(1, getClientArea().height)));
    }

    @Override
    protected void checkSubclass() {
        // SWT subclassing is intentional; this control owns no native resources.
    }
}
