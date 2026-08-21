/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.eclipse.swt.graphics.Rectangle;

/** Maps a pointer position to a compact, renderer-independent chart value tooltip. */
final class ChartHover {
    private ChartHover() { }

    static String textAt(ChartType type, ChartDataset data, Rectangle bounds, int x, int y) {
        if (type == ChartType.MATRIX || type == ChartType.HEATMAP || data.points().isEmpty()) return null;
        return switch (type) {
            case PIE, DONUT -> pieText(type, data, bounds, x, y);
            case HORIZONTAL_BAR -> horizontalText(data, bounds, x, y);
            case SCATTER, BUBBLE -> pointText(data, bounds, x, y);
            default -> categoryText(data, bounds, x, y);
        };
    }

    private static String categoryText(ChartDataset data, Rectangle bounds, int x, int y) {
        Rectangle plot = ChartDrawing.plotBounds(bounds);
        if (!plot.contains(x, y) || data.categories().isEmpty()) return null;
        int count = data.categories().size();
        int index = Math.max(0, Math.min(count - 1,
                (int) Math.floor((double) (x - plot.x) / plot.width * count)));
        String category = data.categories().get(index);
        List<ChartPoint> points = data.points().stream().filter(point -> point.label().equals(category)).toList();
        return points.isEmpty() ? null : categoryText(category, points, data.yAxisTitle());
    }

    private static String horizontalText(ChartDataset data, Rectangle bounds, int x, int y) {
        int left = 130, right = 28, top = 34, bottom = 38;
        Rectangle plot = new Rectangle(bounds.x + left, bounds.y + top,
                Math.max(1, bounds.width - left - right), Math.max(1, bounds.height - top - bottom));
        if (!plot.contains(x, y) || data.categories().isEmpty()) return null;
        int index = Math.max(0, Math.min(data.categories().size() - 1,
                (int) Math.floor((double) (y - plot.y) / plot.height * data.categories().size())));
        String category = data.categories().get(index);
        List<ChartPoint> points = data.points().stream().filter(point -> point.label().equals(category)).toList();
        return points.isEmpty() ? null : categoryText(category, points, data.yAxisTitle());
    }

    private static String pointText(ChartDataset data, Rectangle bounds, int x, int y) {
        Rectangle plot = ChartDrawing.plotBounds(bounds);
        if (!plot.contains(x, y) || !data.hasNumericX()) return null;
        ChartDrawing.Range xRange = ChartDrawing.xRange(data.points());
        ChartDrawing.Range yRange = ChartDrawing.yRange(data.points(), false, data.yAxisMaximum());
        ChartPoint closest = data.points().stream().min(Comparator.comparingDouble(point -> {
            int pointX = ChartDrawing.numericX(plot, point.numericX(), xRange);
            int pointY = ChartDrawing.y(plot, point.y(), yRange);
            int dx = pointX - x, dy = pointY - y;
            return dx * dx + dy * dy;
        })).orElse(null);
        if (closest == null) return null;
        int pointX = ChartDrawing.numericX(plot, closest.numericX(), xRange);
        int pointY = ChartDrawing.y(plot, closest.y(), yRange);
        int dx = pointX - x, dy = pointY - y;
        if (dx * dx + dy * dy > 22 * 22) return null;
        StringJoiner text = new StringJoiner("\n");
        text.add(closest.label());
        if (!closest.series().isBlank()) text.add(closest.series());
        text.add("Value: " + ChartDrawing.formatNumber(closest.y()));
        if (closest.size() != null) text.add("Size: " + ChartDrawing.formatNumber(closest.size()));
        return text.toString();
    }

    private static String pieText(ChartType type, ChartDataset data, Rectangle bounds, int x, int y) {
        Map<String, Double> values = pieValues(data);
        double total = values.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) return null;
        int diameter = Math.max(40, Math.min(bounds.width - 210, bounds.height - 60));
        int originX = bounds.x + 24, originY = bounds.y + (bounds.height - diameter) / 2;
        double centerX = originX + diameter / 2.0, centerY = originY + diameter / 2.0;
        double distance = Math.hypot(x - centerX, y - centerY);
        if (distance > diameter / 2.0 || (type == ChartType.DONUT && distance < diameter / 4.0)) return null;
        double angle = Math.toDegrees(Math.atan2(centerY - y, x - centerX));
        if (angle < 0) angle += 360;
        double start = 0;
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            double arc = entry.getValue() / total * 360;
            if (angle >= start && angle < start + arc) {
                return entry.getKey() + "\nValue: " + ChartDrawing.formatNumber(entry.getValue())
                        + "\nPercent: " + new java.text.DecimalFormat("0.##%").format(entry.getValue() / total);
            }
            start += arc;
        }
        return null;
    }

    private static Map<String, Double> pieValues(ChartDataset data) {
        Map<String, Double> values = new LinkedHashMap<>();
        data.points().forEach(point -> values.merge(point.label(), Math.max(0, point.y()), Double::sum));
        int topN = data.displayOptions().topN();
        if (topN <= 0 || values.size() <= topN) return values;
        List<Map.Entry<String, Double>> ranked = new ArrayList<>(values.entrySet());
        ranked.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        Map<String, Double> trimmed = new LinkedHashMap<>();
        double other = 0;
        for (int index = 0; index < ranked.size(); index++) {
            if (index < topN) trimmed.put(ranked.get(index).getKey(), ranked.get(index).getValue());
            else other += ranked.get(index).getValue();
        }
        if (other > 0) trimmed.put("Other", other);
        return trimmed;
    }

    private static String categoryText(String category, List<ChartPoint> points, String yTitle) {
        StringJoiner text = new StringJoiner("\n");
        text.add(category);
        for (ChartPoint point : points) {
            String name = point.series().isBlank() ? yTitle : point.series();
            text.add(name + ": " + ChartDrawing.formatNumber(point.y()));
        }
        return text.toString();
    }
}
