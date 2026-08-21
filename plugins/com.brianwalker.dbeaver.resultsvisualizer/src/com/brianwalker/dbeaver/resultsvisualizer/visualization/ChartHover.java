/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.swt.graphics.Rectangle;

/** Maps a pointer position to a compact, renderer-independent chart value tooltip. */
final class ChartHover {
    private ChartHover() { }

    static String textAt(ChartType type, ChartDataset data, Rectangle bounds, int x, int y) {
        if (type == ChartType.MATRIX || type == ChartType.HEATMAP || data.points().isEmpty()) return null;
        return switch (type) {
            case PIE, DONUT -> pieText(type, data, bounds, x, y);
            case BAR -> barText(data, bounds, x, y);
            case STACKED_BAR, STACKED_100_BAR -> stackedBarText(type, data, bounds, x, y);
            case HORIZONTAL_BAR -> horizontalText(data, bounds, x, y);
            case COMBO -> comboText(data, bounds, x, y);
            case SCATTER, BUBBLE -> pointText(data, bounds, x, y);
            default -> categoricalPointText(data, bounds, x, y);
        };
    }

    private static String categoricalPointText(ChartDataset data, Rectangle bounds, int x, int y) {
        Rectangle plot = ChartDrawing.plotBounds(bounds);
        if (!plot.contains(x, y) || data.categories().isEmpty()) return null;
        ChartDrawing.Range range = ChartDrawing.yRange(data.points(), true, data.yAxisMaximum());
        return nearestCategoricalPoint(data, plot, range, x, y, 14);
    }

    private static String barText(ChartDataset data, Rectangle bounds, int x, int y) {
        Rectangle plot = ChartDrawing.plotBounds(bounds);
        if (!plot.contains(x, y) || data.categories().isEmpty()) return null;
        List<String> categories = data.categories(), series = data.seriesNames();
        ChartDrawing.Range range = ChartDrawing.yRange(data.points(), true, data.yAxisMaximum());
        int slotWidth = Math.max(1, plot.width / categories.size());
        int barWidth = Math.max(2, (int) (slotWidth * .72 / series.size()));
        int baseline = ChartDrawing.y(plot, 0, range);
        for (int s = 0; s < series.size(); s++) for (ChartPoint point : data.pointsForSeries(series.get(s))) {
            int category = categories.indexOf(point.label());
            int center = ChartDrawing.categoryX(plot, category, categories.size());
            int left = center - barWidth * series.size() / 2 + s * barWidth;
            int valueY = ChartDrawing.y(plot, point.y(), range);
            Rectangle bar = new Rectangle(left, Math.min(valueY, baseline), Math.max(1, barWidth - 3),
                    Math.max(1, Math.abs(baseline - valueY)));
            if (bar.contains(x, y)) return pointText(point);
        }
        return null;
    }

    private static String stackedBarText(ChartType type, ChartDataset data, Rectangle bounds, int x, int y) {
        Rectangle plot = ChartDrawing.plotBounds(bounds);
        if (!plot.contains(x, y) || data.categories().isEmpty()) return null;
        List<String> categories = data.categories(), series = data.seriesNames();
        boolean hundredPercent = type == ChartType.STACKED_100_BAR;
        double maximum = hundredPercent ? 100 : categories.stream().mapToDouble(category -> data.points().stream()
                .filter(point -> point.label().equals(category)).mapToDouble(point -> Math.max(0, point.y())).sum()).max().orElse(1);
        ChartDrawing.Range range = new ChartDrawing.Range(0, ChartDrawing.niceCeiling(maximum));
        int width = Math.max(3, (int) ((double) plot.width / categories.size() * .62));
        for (int c = 0; c < categories.size(); c++) {
            String category = categories.get(c);
            double total = data.points().stream().filter(point -> point.label().equals(category))
                    .mapToDouble(point -> Math.max(0, point.y())).sum();
            double running = 0;
            for (String seriesName : series) {
                ChartPoint point = data.pointsForSeries(seriesName).stream().filter(candidate -> candidate.label().equals(category)).findFirst().orElse(null);
                if (point == null) continue;
                double value = hundredPercent ? (total == 0 ? 0 : Math.max(0, point.y()) / total * 100) : point.y();
                int top = ChartDrawing.y(plot, running + Math.max(0, value), range);
                int bottom = ChartDrawing.y(plot, running, range);
                Rectangle segment = new Rectangle(ChartDrawing.categoryX(plot, c, categories.size()) - width / 2, top, width, Math.max(1, bottom - top));
                if (segment.contains(x, y)) return pointText(point);
                running += Math.max(0, value);
            }
        }
        return null;
    }

    private static String horizontalText(ChartDataset data, Rectangle bounds, int x, int y) {
        int left = 130, right = 28, top = 34, bottom = 38;
        Rectangle plot = new Rectangle(bounds.x + left, bounds.y + top,
                Math.max(1, bounds.width - left - right), Math.max(1, bounds.height - top - bottom));
        if (!plot.contains(x, y) || data.categories().isEmpty()) return null;
        List<String> categories = data.categories(), series = data.seriesNames();
        double maximum = ChartDrawing.niceCeiling(data.points().stream().mapToDouble(ChartPoint::y).max().orElse(1));
        int slot = Math.max(1, plot.height / categories.size());
        int barHeight = Math.max(2, (int) (slot * .72 / series.size()));
        for (int c = 0; c < categories.size(); c++) for (int s = 0; s < series.size(); s++) {
            String category = categories.get(c);
            ChartPoint point = data.pointsForSeries(series.get(s)).stream().filter(candidate -> candidate.label().equals(category)).findFirst().orElse(null);
            if (point == null) continue;
            int width = (int) Math.round(Math.max(0, point.y()) / maximum * plot.width);
            Rectangle bar = new Rectangle(plot.x, plot.y + c * slot + s * barHeight, Math.max(1, width), Math.max(1, barHeight - 2));
            if (bar.contains(x, y)) return pointText(point);
        }
        return null;
    }

    private static String comboText(ChartDataset data, Rectangle bounds, int x, int y) {
        Rectangle plot = ChartDrawing.plotBounds(bounds);
        List<String> categories = data.categories(), series = data.seriesNames();
        if (!plot.contains(x, y) || categories.isEmpty() || series.isEmpty()) return null;
        boolean secondary = data.displayOptions().secondaryAxis() && series.size() > 1;
        if (secondary) plot = new Rectangle(plot.x, plot.y, Math.max(1, plot.width - 48), plot.height);
        List<ChartPoint> columns = data.pointsForSeries(series.get(0));
        List<ChartPoint> lines = series.stream().skip(1).flatMap(name -> data.pointsForSeries(name).stream()).toList();
        ChartDrawing.Range primary = ChartDrawing.yRange(secondary ? columns : data.points(), true, data.yAxisMaximum());
        ChartDrawing.Range line = secondary ? ChartDrawing.yRange(lines, true) : primary;
        int width = Math.max(3, (int) ((double) plot.width / categories.size() * .45));
        int baseline = ChartDrawing.y(plot, 0, primary);
        for (ChartPoint point : columns) {
            int center = ChartDrawing.categoryX(plot, categories.indexOf(point.label()), categories.size());
            int valueY = ChartDrawing.y(plot, point.y(), primary);
            if (new Rectangle(center - width / 2, Math.min(valueY, baseline), width, Math.max(1, baseline - valueY)).contains(x, y)) return pointText(point);
        }
        return nearestCategoricalPoint(lines, categories, plot, line, x, y, 14);
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
        return pointText(closest);
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

    private static String nearestCategoricalPoint(ChartDataset data, Rectangle plot, ChartDrawing.Range range,
            int x, int y, int tolerance) {
        return nearestCategoricalPoint(data.points(), data.categories(), plot, range, x, y, tolerance);
    }

    private static String nearestCategoricalPoint(List<ChartPoint> points, List<String> categories, Rectangle plot,
            ChartDrawing.Range range, int x, int y, int tolerance) {
        ChartPoint closest = points.stream().min(Comparator.comparingDouble(point -> {
            int pointX = ChartDrawing.categoryX(plot, categories.indexOf(point.label()), categories.size());
            int pointY = ChartDrawing.y(plot, point.y(), range);
            return Math.hypot(pointX - x, pointY - y);
        })).orElse(null);
        if (closest == null) return null;
        int pointX = ChartDrawing.categoryX(plot, categories.indexOf(closest.label()), categories.size());
        int pointY = ChartDrawing.y(plot, closest.y(), range);
        return Math.hypot(pointX - x, pointY - y) <= tolerance ? pointText(closest) : null;
    }

    private static String pointText(ChartPoint point) {
        StringBuilder text = new StringBuilder(point.label());
        if (!point.series().isBlank()) text.append('\n').append(point.series());
        text.append("\nValue: ").append(ChartDrawing.formatNumber(point.y()));
        if (point.size() != null) text.append("\nSize: ").append(ChartDrawing.formatNumber(point.size()));
        return text.toString();
    }
}
