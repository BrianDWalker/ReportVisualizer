/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.List;
import org.eclipse.swt.graphics.Rectangle;

/** Categorical horizontal bar chart. */
public final class HorizontalBarChartRenderer implements ChartRenderer {
    @Override public ChartType type() { return ChartType.HORIZONTAL_BAR; }

    @Override public void render(ChartGraphics gc, Rectangle bounds, ChartDataset data) {
        if (data.points().isEmpty()) { ChartDrawing.drawMessage(gc, bounds, "No values to chart."); return; }
        List<String> categories = data.categories();
        List<String> series = data.seriesNames();
        int left = 130, right = 28, top = 34, bottom = 38;
        Rectangle plot = new Rectangle(bounds.x + left, bounds.y + top,
                Math.max(1, bounds.width - left - right), Math.max(1, bounds.height - top - bottom));
        double maximum = ChartDrawing.niceCeiling(data.points().stream().mapToDouble(ChartPoint::y).max().orElse(1));
        gc.setForeground(gc.theme().foreground());
        gc.drawLine(plot.x, plot.y, plot.x, plot.y + plot.height);
        int slot = Math.max(1, plot.height / categories.size());
        int barHeight = Math.max(2, (int) (slot * .72 / series.size()));
        for (int c = 0; c < categories.size(); c++) {
            String label = categories.get(c);
            gc.drawText(label, Math.max(bounds.x + 4, plot.x - gc.textExtent(label).width() - 8),
                    plot.y + c * slot + Math.max(0, (slot - gc.textExtent(label).height()) / 2));
            for (int s = 0; s < series.size(); s++) {
                ChartPoint point = data.pointsForSeries(series.get(s)).stream()
                        .filter(p -> p.label().equals(label)).findFirst().orElse(null);
                if (point == null) continue;
                gc.setBackground(ChartDrawing.seriesColor(gc, s));
                int width = (int) Math.round(Math.max(0, point.y()) / maximum * plot.width);
                int y = plot.y + c * slot + s * barHeight;
                gc.fillRoundRectangle(plot.x, y, Math.max(1, width),
                        Math.max(1, barHeight - 2), 7, 7);
                if (data.displayOptions().dataLabels() && ChartDrawing.shouldDrawValueLabel(c, categories.size())) {
                    String value = ChartDrawing.formatNumber(point.y());
                    gc.setForeground(gc.theme().foreground());
                    gc.drawText(value, Math.min(plot.x + plot.width - gc.textExtent(value).width(),
                            plot.x + width + 5), y);
                }
            }
        }
        gc.drawText(data.yAxisTitle(), plot.x, bounds.y + 6);
        ChartDrawing.drawLegend(gc, plot, series, data.displayOptions().legendPosition());
    }
}
