/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.List;
import org.eclipse.swt.graphics.Rectangle;

/** Compact column-plus-line chart: first series is columns; remaining series are lines. */
public final class ComboChartRenderer implements ChartRenderer {
    @Override public ChartType type() { return ChartType.COMBO; }
    @Override public void render(ChartGraphics gc, Rectangle bounds, ChartDataset data) {
        if (data.points().isEmpty()) { ChartDrawing.drawMessage(gc, bounds, "No values to chart."); return; }
        Rectangle plot = ChartDrawing.plotBounds(bounds); List<String> categories = data.categories(), series = data.seriesNames();
        boolean secondary = data.displayOptions().secondaryAxis() && series.size() > 1;
        if (secondary) plot = new Rectangle(plot.x, plot.y, Math.max(1, plot.width - 48), plot.height);
        List<ChartPoint> columnPoints = data.pointsForSeries(series.get(0));
        List<ChartPoint> linePoints = series.stream().skip(1).flatMap(name -> data.pointsForSeries(name).stream()).toList();
        ChartDrawing.Range primaryRange = ChartDrawing.yRange(secondary ? columnPoints : data.points(), true, data.yAxisMaximum());
        ChartDrawing.Range lineRange = secondary ? ChartDrawing.yRange(linePoints, true) : primaryRange;
        ChartDrawing.drawAxes(gc, plot, data, primaryRange, new ChartDrawing.Range(0, Math.max(1, categories.size() - 1)), false);
        if (secondary) ChartDrawing.drawSecondaryYAxis(gc, plot, lineRange, "Line series");
        String columns = series.get(0); int width = Math.max(3, (int) ((double) plot.width / categories.size() * .45));
        for (ChartPoint point : data.pointsForSeries(columns)) { int x = ChartDrawing.categoryX(plot, categories.indexOf(point.label()), categories.size()); int y = ChartDrawing.y(plot, point.y(), primaryRange); gc.setBackground(ChartDrawing.seriesColor(gc, 0)); gc.fillRoundRectangle(x - width / 2, y, width, Math.max(1, ChartDrawing.y(plot, 0, primaryRange) - y), 5, 5); if (data.displayOptions().dataLabels() && ChartDrawing.shouldDrawValueLabel(categories.indexOf(point.label()), categories.size())) ChartDrawing.drawValueLabel(gc, plot, x, y, point.y()); }
        for (int s = 1; s < series.size(); s++) { gc.setForeground(ChartDrawing.seriesColor(gc, s)); ChartPoint previous = null; for (ChartPoint point : data.pointsForSeries(series.get(s))) { int x = ChartDrawing.categoryX(plot, categories.indexOf(point.label()), categories.size()), y = ChartDrawing.y(plot, point.y(), lineRange); if (previous != null) gc.drawLine(ChartDrawing.categoryX(plot, categories.indexOf(previous.label()), categories.size()), ChartDrawing.y(plot, previous.y(), lineRange), x, y); if (data.displayOptions().markers()) { gc.setBackground(ChartDrawing.seriesColor(gc, s)); gc.fillOval(x - 3, y - 3, 7, 7); } if (data.displayOptions().dataLabels() && ChartDrawing.shouldDrawValueLabel(categories.indexOf(point.label()), categories.size())) ChartDrawing.drawValueLabel(gc, plot, x, y, point.y()); previous = point; } }
    }
}
