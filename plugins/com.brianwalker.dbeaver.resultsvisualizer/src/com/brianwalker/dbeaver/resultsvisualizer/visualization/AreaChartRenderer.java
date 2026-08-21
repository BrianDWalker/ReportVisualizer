/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.List;
import org.eclipse.swt.graphics.Rectangle;

/** Filled categorical area chart, optionally stacking series. */
public final class AreaChartRenderer implements ChartRenderer {
    private final ChartType type;
    private final boolean stacked;
    private final boolean hundredPercent;
    public AreaChartRenderer(ChartType type, boolean stacked) { this(type, stacked, false); }
    public AreaChartRenderer(ChartType type, boolean stacked, boolean hundredPercent) {
        this.type = type; this.stacked = stacked; this.hundredPercent = hundredPercent;
    }
    @Override public ChartType type() { return type; }

    @Override public void render(ChartGraphics gc, Rectangle bounds, ChartDataset data) {
        if (data.points().isEmpty()) { ChartDrawing.drawMessage(gc, bounds, "No values to chart."); return; }
        Rectangle plot = ChartDrawing.plotBounds(bounds);
        List<String> categories = data.categories(), series = data.seriesNames();
        double[] running = new double[categories.size()];
        double max = hundredPercent ? 100 : stacked ? categories.stream().mapToDouble(c -> data.points().stream()
                .filter(p -> p.label().equals(c)).mapToDouble(p -> Math.max(0, p.y())).sum()).max().orElse(1)
                : data.points().stream().mapToDouble(ChartPoint::y).max().orElse(1);
        ChartDrawing.Range range = new ChartDrawing.Range(0, ChartDrawing.niceCeiling(max));
        ChartDrawing.drawAxes(gc, plot, data, range, new ChartDrawing.Range(0, categories.size()), false);
        for (int s = 0; s < series.size(); s++) {
            int[] polygon = new int[categories.size() * 4];
            double[] bases = running.clone();
            double[] tops = new double[categories.size()];
            int offset = 0;
            for (int c = 0; c < categories.size(); c++) {
                double value = value(data, categories.get(c), series.get(s));
                if (hundredPercent) {
                    double total = total(data, categories.get(c));
                    value = total == 0 ? 0 : Math.max(0, value) / total * 100;
                }
                double top = stacked ? running[c] + Math.max(0, value) : value;
                tops[c] = top;
                polygon[offset++] = ChartDrawing.categoryX(plot, c, categories.size());
                polygon[offset++] = ChartDrawing.y(plot, top, range);
                if (stacked) running[c] = top;
            }
            for (int c = categories.size() - 1; c >= 0; c--) {
                double base = stacked ? bases[c] : 0;
                polygon[offset++] = ChartDrawing.categoryX(plot, c, categories.size());
                polygon[offset++] = ChartDrawing.y(plot, base, range);
            }
            gc.setBackground(ChartDrawing.seriesColor(gc, s));
            gc.setAlpha(stacked ? 190 : 110);
            gc.fillPolygon(polygon);
            gc.setAlpha(255);
            gc.setForeground(ChartDrawing.seriesColor(gc, s));
            gc.setLineWidth(2);
            for (int c = 1; c < categories.size(); c++) {
                gc.drawLine(ChartDrawing.categoryX(plot, c - 1, categories.size()),
                        ChartDrawing.y(plot, tops[c - 1], range),
                        ChartDrawing.categoryX(plot, c, categories.size()),
                        ChartDrawing.y(plot, tops[c], range));
            }
            gc.setLineWidth(1);
        }
        for (int c = 0; c < categories.size(); c++) {
            if (data.displayOptions().dataLabels() && ChartDrawing.shouldDrawValueLabel(c, categories.size())) {
                double value = stacked ? running[c]
                        : value(data, categories.get(c), series.get(series.size() - 1));
                ChartDrawing.drawValueLabel(gc, plot,
                        ChartDrawing.categoryX(plot, c, categories.size()),
                        ChartDrawing.y(plot, value, range), value);
            }
        }
    }

    private static double value(ChartDataset data, String category, String series) {
        return data.points().stream().filter(p -> p.label().equals(category) && p.series().equals(series))
                .mapToDouble(ChartPoint::y).findFirst().orElse(0);
    }

    private static double total(ChartDataset data, String category) {
        return data.points().stream().filter(p -> p.label().equals(category))
                .mapToDouble(p -> Math.max(0, p.y())).sum();
    }
}
