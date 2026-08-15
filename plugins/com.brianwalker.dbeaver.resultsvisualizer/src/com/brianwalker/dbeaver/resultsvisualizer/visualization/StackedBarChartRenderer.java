/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.List;
import org.eclipse.swt.graphics.Rectangle;

/** Series values stacked into one bar per category. */
public final class StackedBarChartRenderer implements ChartRenderer {
    @Override public ChartType type() { return ChartType.STACKED_BAR; }

    @Override public void render(ChartGraphics gc, Rectangle bounds, ChartDataset data) {
        if (data.points().isEmpty()) { ChartDrawing.drawMessage(gc, bounds, "No values to chart."); return; }
        List<String> categories = data.categories();
        List<String> series = data.seriesNames();
        double maximum = categories.stream().mapToDouble(category -> data.points().stream()
                .filter(p -> p.label().equals(category)).mapToDouble(p -> Math.max(0, p.y())).sum()).max().orElse(1);
        ChartDrawing.Range range = new ChartDrawing.Range(0, ChartDrawing.niceCeiling(maximum));
        Rectangle plot = ChartDrawing.plotBounds(bounds);
        ChartDrawing.drawAxes(gc, plot, data, range, new ChartDrawing.Range(0, categories.size()), false);
        int width = Math.max(3, (int) ((double) plot.width / categories.size() * .62));
        for (int c = 0; c < categories.size(); c++) {
            double running = 0;
            for (int s = 0; s < series.size(); s++) {
                String category = categories.get(c), seriesName = series.get(s);
                double value = data.points().stream().filter(p -> p.label().equals(category)
                        && p.series().equals(seriesName)).mapToDouble(ChartPoint::y).findFirst().orElse(0);
                int top = ChartDrawing.y(plot, running + Math.max(0, value), range);
                int bottom = ChartDrawing.y(plot, running, range);
                gc.setBackground(ChartDrawing.seriesColor(gc, s));
                gc.fillRectangle(ChartDrawing.categoryX(plot, c, categories.size()) - width / 2,
                        top, width, Math.max(1, bottom - top));
                running += Math.max(0, value);
            }
            if (ChartDrawing.shouldDrawValueLabel(c, categories.size())) {
                ChartDrawing.drawValueLabel(gc, plot,
                        ChartDrawing.categoryX(plot, c, categories.size()),
                        ChartDrawing.y(plot, running, range), running);
            }
        }
    }
}
