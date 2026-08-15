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

    private void paintChart(GC graphics) {
        paintChart(graphics, getClientArea());
    }

    private void paintChart(GC graphics, Rectangle bounds) {
        graphics.setAntialias(SWT.ON);
        graphics.setTextAntialias(SWT.ON);
        graphics.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        graphics.fillRectangle(bounds);
        if (prompt != null) {
            ChartDrawing.drawMessage(graphics, bounds, prompt);
            return;
        }
        if (chartType == ChartType.MATRIX || chartType == ChartType.HEATMAP) {
            int width = Math.max(bounds.width,
                    dataset.rowLevelCount() * 122 + (dataset.columnTuples().size()
                            + (dataset.matrixOptions().rowTotals() ? 1 : 0)) * 110 + 18);
            int height = Math.max(bounds.height,
                    18 + (MatrixChartRenderer.visualRowCount(dataset)
                            + dataset.columnLevelCount()) * 28);
            Transform transform = new Transform(getDisplay());
            try {
                transform.translate(-getHorizontalBar().getSelection(), -getVerticalBar().getSelection());
                graphics.setTransform(transform);
                registry.renderer(chartType).render(graphics, new Rectangle(0, 0, width, height), dataset);
                graphics.setTransform(null);
            } finally {
                transform.dispose();
            }
        } else {
            registry.renderer(chartType).render(graphics, bounds, dataset);
        }
    }

    private void configureScrolling() {
        boolean matrix = chartType == ChartType.MATRIX || chartType == ChartType.HEATMAP;
        getHorizontalBar().setVisible(matrix);
        getVerticalBar().setVisible(matrix);
        if (!matrix) return;
        int width = dataset.rowLevelCount() * 122 + (dataset.columnTuples().size()
                + (dataset.matrixOptions().rowTotals() ? 1 : 0)) * 110 + 18;
        int height = 18 + (MatrixChartRenderer.visualRowCount(dataset)
                + dataset.columnLevelCount()) * 28;
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
