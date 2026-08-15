/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

/** Pie and donut renderer that totals all series by category. */
public final class PieChartRenderer implements ChartRenderer {
    private final ChartType type;
    public PieChartRenderer(ChartType type) { this.type = type; }
    @Override public ChartType type() { return type; }

    @Override public void render(GC gc, Rectangle bounds, ChartDataset data) {
        Map<String, Double> values = new LinkedHashMap<>();
        data.points().forEach(p -> values.merge(p.label(), Math.max(0, p.y()), Double::sum));
        double total = values.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) { ChartDrawing.drawMessage(gc, bounds, "Pie charts require positive values."); return; }
        int diameter = Math.max(40, Math.min(bounds.width - 210, bounds.height - 60));
        int x = bounds.x + 24, y = bounds.y + (bounds.height - diameter) / 2;
        int start = 0, index = 0;
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            int arc = index == values.size() - 1 ? 360 - start
                    : (int) Math.round(entry.getValue() / total * 360);
            gc.setBackground(ChartDrawing.seriesColor(gc, index));
            gc.fillArc(x, y, diameter, diameter, start, arc);
            start += arc;
            int legendY = bounds.y + 24 + index * 22;
            gc.fillRectangle(x + diameter + 26, legendY + 3, 12, 12);
            gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
            gc.drawText(entry.getKey() + "  "
                    + new java.text.DecimalFormat("0.##%").format(entry.getValue() / total),
                    x + diameter + 44, legendY, true);
            index++;
        }
        if (type == ChartType.DONUT) {
            int hole = diameter / 2;
            gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
            gc.fillOval(x + (diameter - hole) / 2, y + (diameter - hole) / 2, hole, hole);
            String totalText = ChartDrawing.formatNumber(total);
            gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
            gc.drawText(totalText, x + (diameter - gc.textExtent(totalText).x) / 2,
                    y + (diameter - gc.textExtent(totalText).y) / 2, true);
        }
    }
}
