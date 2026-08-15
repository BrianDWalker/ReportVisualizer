/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

/** Report-style pivot matrix with ordered hierarchies, optional subtotals, and totals. */
public final class MatrixChartRenderer implements ChartRenderer {
    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.####");
    private static final int MAX_VISIBLE_CELLS = 2_500;
    private static final int CELL_HEIGHT = 28;
    private static final int ROW_WIDTH = 122;
    private final ChartType type;

    public MatrixChartRenderer(ChartType type) { this.type = type; }
    @Override public ChartType type() { return type; }

    @Override public void render(GC gc, Rectangle bounds, ChartDataset data) {
        List<List<String>> rows = data.rowTuples();
        List<List<String>> columns = data.columnTuples();
        if (data.points().isEmpty()) {
            ChartDrawing.drawMessage(gc, bounds, "No matrix values to display."); return;
        }
        if (columns.isEmpty() || columns.stream().allMatch(List::isEmpty)) {
            ChartDrawing.drawMessage(gc, bounds, "Add a Columns field to build the matrix."); return;
        }
        if ((long) rows.size() * columns.size() > MAX_VISIBLE_CELLS) {
            ChartDrawing.drawMessage(gc, bounds, "Matrix is too large; use slicers to reduce it below 2,500 cells.");
            return;
        }

        MatrixDisplayOptions options = data.matrixOptions();
        List<DisplayRow> displayRows = displayRows(rows, options.subtotals());
        int rowLevels = Math.max(1, data.rowLevelCount());
        int columnLevels = Math.max(1, data.columnLevelCount());
        int rowHeaderWidth = rowLevels * ROW_WIDTH;
        int valueColumns = columns.size() + (options.rowTotals() ? 1 : 0);
        int cellWidth = Math.max(76, Math.min(136,
                (bounds.width - rowHeaderWidth - 18) / Math.max(1, valueColumns)));
        int x0 = bounds.x + 8, y0 = bounds.y + 8;
        double maximum = data.points().stream().mapToDouble(ChartPoint::y).max().orElse(1);

        for (int level = 0; level < rowLevels; level++) {
            String name = level < data.rowLevelNames().size()
                    ? data.rowLevelNames().get(level) : "Row " + (level + 1);
            drawCell(gc, x0 + level * ROW_WIDTH, y0 + (columnLevels - 1) * CELL_HEIGHT,
                    ROW_WIDTH, CELL_HEIGHT, name, CellStyle.HEADER, false, 0, maximum);
        }
        for (int level = 0; level < columnLevels; level++) {
            int start = 0;
            while (start < columns.size()) {
                int end = start + 1;
                while (end < columns.size() && samePrefix(columns.get(start), columns.get(end), level)) end++;
                drawCell(gc, x0 + rowHeaderWidth + start * cellWidth, y0 + level * CELL_HEIGHT,
                        (end - start) * cellWidth, CELL_HEIGHT, valueAt(columns.get(start), level),
                        CellStyle.HEADER, false, 0, maximum);
                start = end;
            }
        }
        if (options.rowTotals()) {
            drawCell(gc, x0 + rowHeaderWidth + columns.size() * cellWidth, y0,
                    cellWidth, columnLevels * CELL_HEIGHT, "Total", CellStyle.TOTAL, false, 0, maximum);
        }

        for (int displayIndex = 0; displayIndex < displayRows.size(); displayIndex++) {
            DisplayRow displayRow = displayRows.get(displayIndex);
            int y = y0 + (columnLevels + displayIndex) * CELL_HEIGHT;
            if (displayRow.subtotalLevel() >= 0) {
                drawSubtotal(gc, data, columns, displayRow, x0, y, rowLevels,
                        rowHeaderWidth, cellWidth, maximum, options.rowTotals());
                continue;
            }
            List<String> row = displayRow.values();
            int sourceIndex = rows.indexOf(row);
            for (int level = 0; level < rowLevels; level++) {
                boolean repeated = sourceIndex > 0 && samePrefix(rows.get(sourceIndex - 1), row, level);
                drawCell(gc, x0 + level * ROW_WIDTH, y, ROW_WIDTH, CELL_HEIGHT,
                        repeated ? "" : valueAt(row, level),
                        displayIndex % 2 == 0 ? CellStyle.BODY : CellStyle.ALTERNATE,
                        false, 0, maximum);
            }
            double rowTotal = 0;
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                double value = value(data, row, columns.get(columnIndex));
                rowTotal += value;
                drawCell(gc, x0 + rowHeaderWidth + columnIndex * cellWidth, y,
                        cellWidth, CELL_HEIGHT, FORMAT.format(value),
                        type == ChartType.HEATMAP ? CellStyle.HEAT
                                : displayIndex % 2 == 0 ? CellStyle.BODY : CellStyle.ALTERNATE,
                        true, value, maximum);
            }
            if (options.rowTotals()) {
                drawCell(gc, x0 + rowHeaderWidth + columns.size() * cellWidth, y,
                        cellWidth, CELL_HEIGHT, FORMAT.format(rowTotal), CellStyle.TOTAL, true, 0, maximum);
            }
        }

        if (options.columnTotals()) {
            int y = y0 + (columnLevels + displayRows.size()) * CELL_HEIGHT;
            drawCell(gc, x0, y, rowHeaderWidth, CELL_HEIGHT,
                    "Grand Total", CellStyle.TOTAL, false, 0, maximum);
            double grand = 0;
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                List<String> column = columns.get(columnIndex);
                double total = data.points().stream().filter(point -> point.columnLevels().equals(column))
                        .mapToDouble(ChartPoint::y).sum();
                grand += total;
                drawCell(gc, x0 + rowHeaderWidth + columnIndex * cellWidth, y,
                        cellWidth, CELL_HEIGHT, FORMAT.format(total), CellStyle.TOTAL, true, 0, maximum);
            }
            if (options.rowTotals()) {
                drawCell(gc, x0 + rowHeaderWidth + columns.size() * cellWidth, y,
                        cellWidth, CELL_HEIGHT, FORMAT.format(grand), CellStyle.TOTAL, true, 0, maximum);
            }
        }
    }

    static int visualRowCount(ChartDataset data) {
        return displayRows(data.rowTuples(), data.matrixOptions().subtotals()).size()
                + (data.matrixOptions().columnTotals() ? 1 : 0);
    }

    private static List<DisplayRow> displayRows(List<List<String>> rows, boolean subtotals) {
        List<DisplayRow> result = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            result.add(new DisplayRow(row, -1));
            if (!subtotals || row.size() < 2) continue;
            List<String> next = index + 1 < rows.size() ? rows.get(index + 1) : List.of();
            for (int level = row.size() - 2; level >= 0; level--) {
                if (next.isEmpty() || !samePrefix(row, next, level)) {
                    result.add(new DisplayRow(row.subList(0, level + 1), level));
                }
            }
        }
        return result;
    }

    private static void drawSubtotal(GC gc, ChartDataset data, List<List<String>> columns,
            DisplayRow row, int x0, int y, int rowLevels, int rowHeaderWidth,
            int cellWidth, double maximum, boolean rowTotals) {
        String label = valueAt(row.values(), row.subtotalLevel()) + " subtotal";
        drawCell(gc, x0, y, rowHeaderWidth, CELL_HEIGHT, label,
                CellStyle.SUBTOTAL, false, 0, maximum);
        double rowTotal = 0;
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            List<String> column = columns.get(columnIndex);
            double total = data.points().stream().filter(point -> point.columnLevels().equals(column)
                            && hasPrefix(point.rowLevels(), row.values()))
                    .mapToDouble(ChartPoint::y).sum();
            rowTotal += total;
            drawCell(gc, x0 + rowHeaderWidth + columnIndex * cellWidth, y,
                    cellWidth, CELL_HEIGHT, FORMAT.format(total), CellStyle.SUBTOTAL, true, 0, maximum);
        }
        if (rowTotals) {
            drawCell(gc, x0 + rowHeaderWidth + columns.size() * cellWidth, y,
                    cellWidth, CELL_HEIGHT, FORMAT.format(rowTotal), CellStyle.SUBTOTAL, true, 0, maximum);
        }
    }

    private static double value(ChartDataset data, List<String> row, List<String> column) {
        return data.points().stream().filter(point -> point.rowLevels().equals(row)
                        && point.columnLevels().equals(column))
                .mapToDouble(ChartPoint::y).findFirst().orElse(0);
    }

    private static boolean hasPrefix(List<String> values, List<String> prefix) {
        if (values.size() < prefix.size()) return false;
        for (int index = 0; index < prefix.size(); index++)
            if (!values.get(index).equals(prefix.get(index))) return false;
        return true;
    }

    private static boolean samePrefix(List<String> left, List<String> right, int level) {
        for (int index = 0; index <= level; index++)
            if (!valueAt(left, index).equals(valueAt(right, index))) return false;
        return true;
    }

    private static String valueAt(List<String> values, int index) {
        return index < values.size() ? values.get(index) : "";
    }

    private static void drawCell(GC gc, int x, int y, int width, int height,
            String text, CellStyle style, boolean numeric, double value, double maximum) {
        var device = gc.getDevice();
        boolean heat = style == CellStyle.HEAT && maximum > 0;
        Color temporary = null;
        if (heat) {
            double ratio = Math.max(0, Math.min(1, value / maximum));
            temporary = new Color(device,
                    (int) Math.round(196 - ratio * 120),
                    (int) Math.round(216 - ratio * 96),
                    (int) Math.round(231 - ratio * 63));
            gc.setBackground(temporary);
        } else if (style == CellStyle.HEADER) {
            gc.setBackground(ChartDrawing.seriesColor(gc, 0));
        } else if (style == CellStyle.TOTAL || style == CellStyle.SUBTOTAL) {
            gc.setBackground(ChartDrawing.seriesColor(gc, style == CellStyle.TOTAL ? 7 : 9));
        } else {
            gc.setBackground(device.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        }
        gc.fillRectangle(x, y, width, height);
        if (style == CellStyle.ALTERNATE) {
            gc.setAlpha(18);
            gc.setBackground(device.getSystemColor(SWT.COLOR_LIST_SELECTION));
            gc.fillRectangle(x, y, width, height);
            gc.setAlpha(255);
        }
        gc.setForeground(device.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
        gc.drawLine(x, y + height - 1, x + width, y + height - 1);
        String shown = abbreviate(text, Math.max(4, width / 8));
        if (heat) {
            gc.setForeground(device.getSystemColor(SWT.COLOR_BLACK));
        } else if (style == CellStyle.HEADER || style == CellStyle.TOTAL || style == CellStyle.SUBTOTAL) {
            Color background = gc.getBackground();
            int luminance = background.getRed() * 299 + background.getGreen() * 587
                    + background.getBlue() * 114;
            gc.setForeground(device.getSystemColor(luminance < 125_000 ? SWT.COLOR_WHITE : SWT.COLOR_BLACK));
        } else {
            gc.setForeground(device.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
        }
        int textX = numeric ? Math.max(x + 6, x + width - gc.textExtent(shown).x - 7) : x + 7;
        gc.drawText(shown, textX, y + Math.max(1, (height - gc.textExtent(shown).y) / 2), true);
        if (temporary != null) temporary.dispose();
    }

    private static String abbreviate(String text, int maximum) {
        if (text == null) return "";
        return text.length() > maximum ? text.substring(0, Math.max(1, maximum - 1)) + "…" : text;
    }

    private enum CellStyle { HEADER, BODY, ALTERNATE, HEAT, SUBTOTAL, TOTAL }
    private record DisplayRow(List<String> values, int subtotalLevel) {}
}
