/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import org.eclipse.swt.graphics.Rectangle;

/** Numeric X/Y scatter chart renderer. */
public final class ScatterChartRenderer implements ChartRenderer {
    @Override
    public ChartType type() {
        return ChartType.SCATTER;
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

        java.util.List<String> seriesNames = dataset.seriesNames();
        for (int seriesIndex = 0; seriesIndex < seriesNames.size(); seriesIndex++) {
            ChartColor color = ChartDrawing.seriesColor(graphics, seriesIndex);
            graphics.setBackground(color);
            var seriesPoints = dataset.pointsForSeries(seriesNames.get(seriesIndex));
            for (int pointIndex = 0; pointIndex < seriesPoints.size(); pointIndex++) {
                ChartPoint point = seriesPoints.get(pointIndex);
                int x = ChartDrawing.numericX(plot, point.numericX(), xRange);
                int y = ChartDrawing.y(plot, point.y(), yRange);
                graphics.fillOval(x - 5, y - 5, 11, 11);
                graphics.setForeground(graphics.theme().background());
                graphics.drawOval(x - 3, y - 3, 7, 7);
                if (ChartDrawing.shouldDrawValueLabel(pointIndex, seriesPoints.size())) {
                    ChartDrawing.drawValueLabel(graphics, plot, x, y, point.y());
                }
            }
        }
    }
}
