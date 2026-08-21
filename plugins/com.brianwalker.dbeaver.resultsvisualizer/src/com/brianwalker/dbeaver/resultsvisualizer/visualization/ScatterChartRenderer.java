/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import org.eclipse.swt.graphics.Rectangle;

/** Numeric X/Y scatter chart renderer. */
public final class ScatterChartRenderer implements ChartRenderer {
    private final ChartType type; private final boolean bubble;
    public ScatterChartRenderer() { this(ChartType.SCATTER, false); }
    public ScatterChartRenderer(ChartType type, boolean bubble) { this.type = type; this.bubble = bubble; }
    @Override
    public ChartType type() {
        return type;
    }

    @Override
    public void render(ChartGraphics graphics, Rectangle bounds, ChartDataset dataset) {
        if (dataset.points().isEmpty()) {
            ChartDrawing.drawMessage(graphics, bounds, "No numeric values to chart.");
            return;
        }
        if (!dataset.hasNumericX()) {
            ChartDrawing.drawMessage(graphics, bounds,
                    "Scatter charts require a numeric X-axis field.");
            return;
        }
        Rectangle plot = ChartDrawing.plotBounds(bounds);
        ChartDrawing.Range xRange = ChartDrawing.xRange(dataset.points());
        ChartDrawing.Range yRange = ChartDrawing.yRange(
                dataset.points(), false, dataset.yAxisMaximum());
        ChartDrawing.drawAxes(graphics, plot, dataset, yRange, xRange, true);
        double maximumBubbleSize = dataset.points().stream().map(ChartPoint::size)
                .filter(java.util.Objects::nonNull).mapToDouble(value -> Math.abs(value)).max().orElse(0);

        java.util.List<String> seriesNames = dataset.seriesNames();
        for (int seriesIndex = 0; seriesIndex < seriesNames.size(); seriesIndex++) {
            ChartColor color = ChartDrawing.seriesColor(graphics, seriesIndex);
            graphics.setBackground(color);
            var seriesPoints = dataset.pointsForSeries(seriesNames.get(seriesIndex));
            for (int pointIndex = 0; pointIndex < seriesPoints.size(); pointIndex++) {
                ChartPoint point = seriesPoints.get(pointIndex);
                int x = ChartDrawing.numericX(plot, point.numericX(), xRange);
                int y = ChartDrawing.y(plot, point.y(), yRange);
                double bubbleValue = point.size() == null ? Math.abs(point.y()) : Math.abs(point.size());
                double bubbleMaximum = maximumBubbleSize > 0 ? maximumBubbleSize
                        : Math.max(1, Math.max(Math.abs(yRange.minimum()), Math.abs(yRange.maximum())));
                int radius = bubble ? Math.max(5, Math.min(18,
                        5 + (int) Math.round(bubbleValue / bubbleMaximum * 13))) : 5;
                graphics.fillOval(x - radius, y - radius, radius * 2 + 1, radius * 2 + 1);
                graphics.setForeground(graphics.theme().background());
                graphics.drawOval(x - Math.max(2, radius - 2), y - Math.max(2, radius - 2), Math.max(5, radius * 2 - 3), Math.max(5, radius * 2 - 3));
                if (dataset.displayOptions().dataLabels() && ChartDrawing.shouldDrawValueLabel(pointIndex, seriesPoints.size())) {
                    ChartDrawing.drawValueLabel(graphics, plot, x, y, point.y());
                }
            }
        }
    }
}
