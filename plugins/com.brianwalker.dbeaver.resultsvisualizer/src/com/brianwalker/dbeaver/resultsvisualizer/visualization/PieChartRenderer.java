/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.eclipse.swt.graphics.Rectangle;

/** Pie and donut renderer that totals all series by category. */
public final class PieChartRenderer implements ChartRenderer {
    private final ChartType type;
    public PieChartRenderer(ChartType type) { this.type = type; }
    @Override public ChartType type() { return type; }

    @Override public void render(ChartGraphics gc, Rectangle bounds, ChartDataset data) {
        Map<String, Double> values = new LinkedHashMap<>();
        data.points().forEach(p -> values.merge(p.label(), Math.max(0, p.y()), Double::sum));
        if (data.displayOptions().topN() > 0 && values.size() > data.displayOptions().topN()) {
            List<Map.Entry<String, Double>> ranked = new ArrayList<>(values.entrySet());
            ranked.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
            Map<String, Double> trimmed = new LinkedHashMap<>(); double other = 0;
            for (int i = 0; i < ranked.size(); i++) {
                if (i < data.displayOptions().topN()) trimmed.put(ranked.get(i).getKey(), ranked.get(i).getValue());
                else other += ranked.get(i).getValue();
            }
            if (other > 0) trimmed.put("Other", other);
            values.clear();
            values.putAll(trimmed);
        }
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
            gc.setForeground(gc.theme().foreground());
            gc.drawText(label(entry.getKey(), entry.getValue(), total, data.displayOptions().pieLabelMode()),
                    x + diameter + 44, legendY);
            index++;
        }
        if (type == ChartType.DONUT) {
            int hole = diameter / 2;
            gc.setBackground(gc.theme().background());
            gc.fillOval(x + (diameter - hole) / 2, y + (diameter - hole) / 2, hole, hole);
            String totalText = ChartDrawing.formatNumber(total);
            gc.setForeground(gc.theme().foreground());
            gc.drawText(totalText, x + (diameter - gc.textExtent(totalText).width()) / 2,
                    y + (diameter - gc.textExtent(totalText).height()) / 2);
        }
    }

    private static String label(String category, double value, double total,
            ChartDisplayOptions.PieLabelMode mode) {
        String percent = new java.text.DecimalFormat("0.##%").format(value / total);
        return switch (mode) {
            case CATEGORY -> category;
            case VALUE -> ChartDrawing.formatNumber(value);
            case PERCENT -> percent;
            case CATEGORY_PERCENT -> category + "  " + percent;
        };
    }
}
