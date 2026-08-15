/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import org.eclipse.swt.graphics.Rectangle;

/** Ordered categorical line chart renderer. */
public final class LineChartRenderer implements ChartRenderer {
    @Override
    public ChartType type() {
        return ChartType.LINE;
    }

    @Override
    public void render(ChartGraphics graphics, Rectangle bounds, ChartDataset dataset) {
        if (dataset.points().isEmpty()) {
            ChartDrawing.drawMessage(graphics, bounds, "No numeric Y-axis values to chart.");
            return;
        }
        Rectangle plot = ChartDrawing.plotBounds(bounds);
        ChartDrawing.Range yRange = ChartDrawing.yRange(
                dataset.points(), false, dataset.yAxisMaximum());
        ChartDrawing.drawAxes(graphics, plot, dataset, yRange,
                new ChartDrawing.Range(0, Math.max(1, dataset.points().size() - 1)), false);

        graphics.setLineWidth(2);
        java.util.List<String> categories = dataset.categories();
        java.util.List<String> seriesNames = dataset.seriesNames();
        for (int seriesIndex = 0; seriesIndex < seriesNames.size(); seriesIndex++) {
            ChartColor color = ChartDrawing.seriesColor(graphics, seriesIndex);
            graphics.setForeground(color);
            graphics.setBackground(color);
            int previousX = 0;
            int previousY = 0;
            boolean hasPrevious = false;
            for (ChartPoint point : dataset.pointsForSeries(seriesNames.get(seriesIndex))) {
                int index = categories.indexOf(point.label());
                int x = ChartDrawing.categoryX(plot, index, categories.size());
                int y = ChartDrawing.y(plot, point.y(), yRange);
                if (hasPrevious) graphics.drawLine(previousX, previousY, x, y);
                graphics.fillOval(x - 4, y - 4, 9, 9);
                graphics.setBackground(graphics.theme().background());
                graphics.fillOval(x - 2, y - 2, 5, 5);
                graphics.setBackground(color);
                if (ChartDrawing.shouldDrawValueLabel(index, categories.size())) {
                    ChartDrawing.drawValueLabel(graphics, plot, x, y, point.y());
                    graphics.setForeground(color);
                }
                previousX = x;
                previousY = y;
                hasPrevious = true;
            }
        }
        graphics.setLineWidth(1);
    }
}
