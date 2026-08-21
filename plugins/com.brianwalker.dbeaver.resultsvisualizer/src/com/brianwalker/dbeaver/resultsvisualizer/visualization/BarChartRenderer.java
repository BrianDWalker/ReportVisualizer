/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import org.eclipse.swt.graphics.Rectangle;

/** Categorical vertical bar chart renderer. */
public final class BarChartRenderer implements ChartRenderer {
    @Override
    public ChartType type() {
        return ChartType.BAR;
    }

    @Override
    public void render(ChartGraphics graphics, Rectangle bounds, ChartDataset dataset) {
        if (dataset.points().isEmpty()) {
            ChartDrawing.drawMessage(graphics, bounds, "No numeric Y-axis values to chart.");
            return;
        }
        Rectangle plot = ChartDrawing.plotBounds(bounds);
        ChartDrawing.Range yRange = ChartDrawing.yRange(
                dataset.points(), true, dataset.yAxisMaximum());
        ChartDrawing.drawAxes(graphics, plot, dataset, yRange,
                new ChartDrawing.Range(0, Math.max(1, dataset.points().size() - 1)), false);

        java.util.List<String> categories = dataset.categories();
        java.util.List<String> seriesNames = dataset.seriesNames();
        int slotWidth = Math.max(1, plot.width / categories.size());
        int barWidth = Math.max(2, (int) (slotWidth * 0.72 / seriesNames.size()));
        int baseline = ChartDrawing.y(plot, 0, yRange);
        for (int seriesIndex = 0; seriesIndex < seriesNames.size(); seriesIndex++) {
            String series = seriesNames.get(seriesIndex);
            graphics.setBackground(ChartDrawing.seriesColor(graphics, seriesIndex));
            for (ChartPoint point : dataset.pointsForSeries(series)) {
                int categoryIndex = categories.indexOf(point.label());
                int center = ChartDrawing.categoryX(plot, categoryIndex, categories.size());
                int groupWidth = barWidth * seriesNames.size();
                int x = center - groupWidth / 2 + seriesIndex * barWidth;
                int valueY = ChartDrawing.y(plot, point.y(), yRange);
                int top = Math.min(valueY, baseline);
                int height = Math.max(1, Math.abs(baseline - valueY));
                int width = Math.max(1, barWidth - 3);
                graphics.fillRoundRectangle(x, top, width, height, 7, 7);
                if (dataset.displayOptions().dataLabels() && ChartDrawing.shouldDrawValueLabel(categoryIndex, categories.size())) {
                    ChartDrawing.drawValueLabel(graphics, plot, x + width / 2, top, point.y());
                }
            }
        }
    }
}
