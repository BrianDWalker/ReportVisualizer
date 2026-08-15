/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.text.DecimalFormat;
import java.util.List;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartGraphics.LineStyle;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartGraphics.TextSize;
import org.eclipse.swt.graphics.Rectangle;

/** Shared axes, scales, labels, and empty-state drawing. */
final class ChartDrawing {
    private static final int[][] LIGHT_PALETTE = {
            {76, 120, 168}, {89, 161, 79}, {225, 124, 66}, {178, 121, 162},
            {42, 157, 143}, {225, 87, 89}, {123, 111, 208}, {111, 143, 175},
            {156, 117, 95}, {92, 141, 137}
    };
    private static final int[][] DARK_PALETTE = {
            {98, 160, 234}, {118, 185, 71}, {242, 142, 91}, {200, 138, 194},
            {57, 183, 165}, {240, 107, 107}, {145, 135, 224}, {126, 166, 201},
            {185, 137, 115}, {112, 165, 159}
    };
    static final int LEFT_MARGIN = 66;
    static final int RIGHT_MARGIN = 22;
    static final int TOP_MARGIN = 28;
    static final int BOTTOM_MARGIN = 58;
    private static final int TICK_COUNT = 5;
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("0.####");

    private ChartDrawing() {
    }

    static Rectangle plotBounds(Rectangle bounds) {
        return new Rectangle(bounds.x + LEFT_MARGIN, bounds.y + TOP_MARGIN,
                Math.max(1, bounds.width - LEFT_MARGIN - RIGHT_MARGIN),
                Math.max(1, bounds.height - TOP_MARGIN - BOTTOM_MARGIN));
    }

    static Range yRange(List<ChartPoint> points, boolean includeZero, Double configuredMaximum) {
        double minimum = points.stream().mapToDouble(ChartPoint::y).min().orElse(0);
        double maximum = points.stream().mapToDouble(ChartPoint::y).max().orElse(1);
        if (includeZero) {
            minimum = Math.min(0, minimum);
            maximum = Math.max(0, maximum);
        }
        if (configuredMaximum != null && configuredMaximum > minimum) {
            maximum = configuredMaximum;
        } else {
            maximum = niceCeiling(maximum);
        }
        return expandedRange(minimum, maximum);
    }

    static Range yRange(List<ChartPoint> points, boolean includeZero) {
        return yRange(points, includeZero, null);
    }

    /** Returns an easy-to-read 1/2/2.5/5/10 multiple at or above the value. */
    static double niceCeiling(double value) {
        if (value == 0 || !Double.isFinite(value)) return value == 0 ? 1 : value;
        if (value < 0) return 0;
        double magnitude = Math.pow(10, Math.floor(Math.log10(value)));
        double normalized = value / magnitude;
        double nice = normalized <= 1 ? 1
                : normalized <= 2 ? 2
                : normalized <= 2.5 ? 2.5
                : normalized <= 5 ? 5 : 10;
        return nice * magnitude;
    }

    static Range xRange(List<ChartPoint> points) {
        double minimum = points.stream().map(ChartPoint::numericX)
                .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue)
                .min().orElse(0);
        double maximum = points.stream().map(ChartPoint::numericX)
                .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue)
                .max().orElse(1);
        return expandedRange(minimum, maximum);
    }

    private static Range expandedRange(double minimum, double maximum) {
        if (Double.compare(minimum, maximum) == 0) {
            double padding = minimum == 0 ? 1 : Math.abs(minimum) * 0.1;
            minimum -= padding;
            maximum += padding;
        }
        return new Range(minimum, maximum);
    }

    static void drawAxes(ChartGraphics graphics, Rectangle plot, ChartDataset dataset,
            Range yRange, Range xRange, boolean numericX) {
        graphics.setForeground(graphics.theme().gridLine());
        graphics.setLineStyle(LineStyle.DOT);
        graphics.setAlpha(90);
        for (int tick = 0; tick <= TICK_COUNT; tick++) {
            int y = plot.y + plot.height - (plot.height * tick / TICK_COUNT);
            graphics.drawLine(plot.x, y, plot.x + plot.width, y);
            double value = yRange.minimum()
                    + (yRange.span() * tick / TICK_COUNT);
            String label = NUMBER_FORMAT.format(value);
            TextSize size = graphics.textExtent(label);
            graphics.setForeground(graphics.theme().foreground());
            graphics.drawText(label, plot.x - size.width() - 7, y - size.height() / 2);
            graphics.setForeground(graphics.theme().gridLine());
        }
        graphics.setAlpha(255);
        graphics.setLineStyle(LineStyle.SOLID);
        graphics.setForeground(graphics.theme().foreground());
        graphics.drawLine(plot.x, plot.y, plot.x, plot.y + plot.height);
        graphics.drawLine(plot.x, plot.y + plot.height, plot.x + plot.width, plot.y + plot.height);
        graphics.drawText(dataset.yAxisTitle(), plot.x, Math.max(0, plot.y - 24));
        TextSize xTitleSize = graphics.textExtent(dataset.xAxisTitle());
        graphics.drawText(dataset.xAxisTitle(),
                plot.x + (plot.width - xTitleSize.width()) / 2,
                plot.y + plot.height + 37);

        if (numericX) {
            drawNumericXLabels(graphics, plot, xRange);
        } else {
            drawCategoryLabels(graphics, plot, dataset.categories());
        }
        drawLegend(graphics, plot, dataset.seriesNames());
    }

    private static void drawNumericXLabels(ChartGraphics graphics, Rectangle plot, Range range) {
        for (int tick = 0; tick <= TICK_COUNT; tick++) {
            int x = plot.x + plot.width * tick / TICK_COUNT;
            String label = NUMBER_FORMAT.format(
                    range.minimum() + range.span() * tick / TICK_COUNT);
            TextSize size = graphics.textExtent(label);
            graphics.drawText(label, x - size.width() / 2, plot.y + plot.height + 7);
        }
    }

    private static void drawCategoryLabels(
            ChartGraphics graphics, Rectangle plot, List<String> categories) {
        if (categories.isEmpty()) return;
        int step = Math.max(1, (int) Math.ceil(categories.size() / 10.0));
        for (int index = 0; index < categories.size(); index += step) {
            int x = categoryX(plot, index, categories.size());
            int available = Math.max(28, plot.width / Math.min(categories.size(), 10) - 6);
            String label = elide(graphics, categories.get(index), available);
            TextSize size = graphics.textExtent(label);
            graphics.drawText(label, x - size.width() / 2, plot.y + plot.height + 7);
        }
    }

    static int categoryX(Rectangle plot, int index, int count) {
        if (count <= 1) return plot.x + plot.width / 2;
        double slot = (double) plot.width / count;
        return plot.x + (int) Math.round(slot * (index + 0.5));
    }

    static int numericX(Rectangle plot, double value, Range range) {
        return plot.x + (int) Math.round((value - range.minimum()) / range.span() * plot.width);
    }

    static int y(Rectangle plot, double value, Range range) {
        return plot.y + plot.height
                - (int) Math.round((value - range.minimum()) / range.span() * plot.height);
    }

    static void drawMessage(ChartGraphics graphics, Rectangle bounds, String message) {
        graphics.setForeground(graphics.theme().foreground());
        TextSize extent = graphics.textExtent(message);
        graphics.drawText(message,
                bounds.x + Math.max(8, (bounds.width - extent.width()) / 2),
                bounds.y + Math.max(8, (bounds.height - extent.height()) / 2));
    }

    static ChartColor seriesColor(ChartGraphics graphics, int index) {
        int[][] palette = graphics.theme().isDark() ? DARK_PALETTE : LIGHT_PALETTE;
        int[] rgb = palette[Math.floorMod(index, palette.length)];
        return new ChartColor(rgb[0], rgb[1], rgb[2]);
    }

    static String formatNumber(double value) {
        return NUMBER_FORMAT.format(value);
    }

    static boolean shouldDrawValueLabel(int index, int count) {
        int step = Math.max(1, (int) Math.ceil(count / 24.0));
        return index % step == 0 || index == count - 1;
    }

    static void drawValueLabel(ChartGraphics graphics, Rectangle plot, int x, int y, double value) {
        String label = formatNumber(value);
        TextSize size = graphics.textExtent(label);
        int labelX = Math.max(plot.x, Math.min(plot.x + plot.width - size.width(), x - size.width() / 2));
        int labelY = y - size.height() - 5;
        if (labelY < plot.y) labelY = Math.min(plot.y + plot.height - size.height(), y + 5);
        graphics.setForeground(graphics.theme().foreground());
        graphics.drawText(label, labelX, labelY);
    }

    private static void drawLegend(ChartGraphics graphics, Rectangle plot, List<String> seriesNames) {
        if (seriesNames.size() <= 1 || seriesNames.stream().allMatch(String::isBlank)) return;
        int x = plot.x + plot.width;
        int y = Math.max(2, plot.y - 24);
        for (int index = seriesNames.size() - 1; index >= 0; index--) {
            String name = elide(graphics, seriesNames.get(index), 100);
            int width = graphics.textExtent(name).width() + 19;
            x -= width;
            graphics.setBackground(seriesColor(graphics, index));
            graphics.fillRectangle(x, y + 4, 10, 10);
            graphics.setForeground(graphics.theme().foreground());
            graphics.drawText(name, x + 14, y);
            x -= 10;
            if (x < plot.x) break;
        }
    }

    private static String elide(ChartGraphics graphics, String text, int maximumWidth) {
        if (graphics.textExtent(text).width() <= maximumWidth) return text;
        String ellipsis = "…";
        int end = text.length();
        while (end > 1 && graphics.textExtent(text.substring(0, end) + ellipsis).width() > maximumWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    record Range(double minimum, double maximum) {
        double span() {
            return maximum - minimum;
        }
    }
}
