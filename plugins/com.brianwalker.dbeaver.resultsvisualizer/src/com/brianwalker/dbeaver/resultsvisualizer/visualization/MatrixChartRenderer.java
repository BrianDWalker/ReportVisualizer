/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.swt.graphics.Rectangle;

/** Report-style pivot matrix with ordered hierarchies, optional subtotals, and totals. */
public final class MatrixChartRenderer implements ChartRenderer {
    private static final int MAX_VISIBLE_CELLS = 2_500;
    static final int CELL_HEIGHT = 28;
    static final int ROW_WIDTH = 122;
    private final ChartType type;

    public MatrixChartRenderer(ChartType type) { this.type = type; }
    @Override public ChartType type() { return type; }

    @Override public void render(ChartGraphics gc, Rectangle bounds, ChartDataset data) {
        MatrixDisplayOptions options = data.matrixOptions();
        List<List<String>> rows = topRows(data, data.rowTuples(), options.topN());
        List<List<String>> columns = data.columnTuples();
        if (data.points().isEmpty()) {
            ChartDrawing.drawMessage(gc, bounds, "No matrix values to display."); return;
        }
        if (columns.isEmpty() || columns.stream().allMatch(List::isEmpty)) {
            ChartDrawing.drawMessage(gc, bounds, "Add a Columns field to build the matrix."); return;
        }
        List<DisplayRow> displayRows = displayRows(rows, options);
        long logicalCells = (long) displayRows.size() * columns.size();
        if (logicalCells > MAX_VISIBLE_CELLS) {
            ChartDrawing.drawMessage(gc, bounds, "Matrix: " + data.points().size()
                    + " source points, " + logicalCells + " logical cells; rendering is capped at 2,500. Use slicers or Top N.");
            return;
        }
        int rowLevels = Math.max(1, data.rowLevelCount());
        int columnLevels = Math.max(1, data.columnLevelCount());
        int rowHeaderWidth = options.layout() == MatrixDisplayOptions.Layout.STEPPED
                ? ROW_WIDTH * 2 : rowLevels * ROW_WIDTH;
        int valueColumns = columns.size() + (options.rowTotals() ? 1 : 0);
        int cellWidth = options.columnWidth();
        int x0 = bounds.x + 8, y0 = bounds.y + 8;
        double maximum = data.points().stream().mapToDouble(ChartPoint::y).max().orElse(1);

        for (int level = 0; level < (options.layout() == MatrixDisplayOptions.Layout.STEPPED ? 1 : rowLevels); level++) {
            String name = level < data.rowLevelNames().size()
                    ? data.rowLevelNames().get(level) : "Row " + (level + 1);
            drawCell(gc, x0 + level * ROW_WIDTH, y0 + (columnLevels - 1) * CELL_HEIGHT,
                    options.layout() == MatrixDisplayOptions.Layout.STEPPED ? rowHeaderWidth : ROW_WIDTH,
                    CELL_HEIGHT, options.layout() == MatrixDisplayOptions.Layout.STEPPED ? String.join(" › ", data.rowLevelNames()) : name, CellStyle.HEADER, false, 0, maximum);
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
            if (options.layout() == MatrixDisplayOptions.Layout.STEPPED) {
                int depth = Math.max(0, row.size() - 1);
                drawCell(gc, x0, y, rowHeaderWidth, CELL_HEIGHT,
                        "  ".repeat(depth) + (depth < rowLevels - 1 ? "▾ " : "") + valueAt(row, depth),
                        displayIndex % 2 == 0 ? CellStyle.BODY : CellStyle.ALTERNATE, false, 0, maximum);
            } else for (int level = 0; level < rowLevels; level++) {
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
                        cellWidth, CELL_HEIGHT, format(value, options),
                        type == ChartType.HEATMAP || options.conditionalFormat() == MatrixDisplayOptions.ConditionalFormat.COLOR_SCALE ? CellStyle.HEAT
                                : options.dataBars() || options.conditionalFormat() == MatrixDisplayOptions.ConditionalFormat.DATA_BARS ? CellStyle.DATA_BAR
                                : displayIndex % 2 == 0 ? CellStyle.BODY : CellStyle.ALTERNATE,
                        true, value, maximum);
            }
            if (options.rowTotals()) {
                drawCell(gc, x0 + rowHeaderWidth + columns.size() * cellWidth, y,
                        cellWidth, CELL_HEIGHT, format(rowTotal, options), CellStyle.TOTAL, true, 0, maximum);
            }
        }

        if (options.columnTotals() && options.grandTotals()) {
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
                        cellWidth, CELL_HEIGHT, format(total, options), CellStyle.TOTAL, true, 0, maximum);
            }
            if (options.rowTotals()) {
                drawCell(gc, x0 + rowHeaderWidth + columns.size() * cellWidth, y,
                        cellWidth, CELL_HEIGHT, format(grand, options), CellStyle.TOTAL, true, 0, maximum);
            }
        }
    }

    static int visualRowCount(ChartDataset data) {
        return displayRows(topRows(data, data.rowTuples(), data.matrixOptions().topN()), data.matrixOptions()).size()
                + (data.matrixOptions().columnTotals() && data.matrixOptions().grandTotals() ? 1 : 0);
    }

    private static List<DisplayRow> displayRows(List<List<String>> rows, MatrixDisplayOptions options) {
        List<DisplayRow> result = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            List<String> collapsed = collapsedPrefix(row, options.collapsedRowPaths());
            if (!collapsed.isEmpty()) {
                if (result.stream().noneMatch(existing -> existing.collapsed()
                        && existing.values().equals(collapsed))) result.add(new DisplayRow(collapsed, collapsed.size() - 1, true));
                continue;
            }
            result.add(new DisplayRow(row, -1, false));
            if (!options.subtotals() || row.size() < 2) continue;
            List<String> next = index + 1 < rows.size() ? rows.get(index + 1) : List.of();
            for (int level = row.size() - 2; level >= 0; level--) {
                if ((options.subtotalLevels().isEmpty() || options.subtotalLevels().contains(level))
                        && (next.isEmpty() || !samePrefix(row, next, level))) {
                    result.add(new DisplayRow(row.subList(0, level + 1), level, false));
                }
            }
        }
        return result;
    }

    private static List<String> collapsedPrefix(List<String> row, java.util.Set<String> collapsedPaths) {
        for (int length = 1; length < row.size(); length++) {
            List<String> prefix = row.subList(0, length);
            if (collapsedPaths.contains(path(prefix))) return List.copyOf(prefix);
        }
        return List.of();
    }

    public static String path(List<String> values) { return String.join("\u001f", values); }

    private static void drawSubtotal(ChartGraphics gc, ChartDataset data, List<List<String>> columns,
            DisplayRow row, int x0, int y, int rowLevels, int rowHeaderWidth,
            int cellWidth, double maximum, boolean rowTotals) {
        String label = (row.collapsed() ? "▸ " : "") + valueAt(row.values(), row.subtotalLevel())
                + (row.collapsed() ? "" : " subtotal");
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
                    cellWidth, CELL_HEIGHT, format(total, data.matrixOptions()), CellStyle.SUBTOTAL, true, 0, maximum);
        }
        if (rowTotals) {
            drawCell(gc, x0 + rowHeaderWidth + columns.size() * cellWidth, y,
                    cellWidth, CELL_HEIGHT, format(rowTotal, data.matrixOptions()), CellStyle.SUBTOTAL, true, 0, maximum);
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

    private static void drawCell(ChartGraphics gc, int x, int y, int width, int height,
            String text, CellStyle style, boolean numeric, double value, double maximum) {
        ChartTheme theme = gc.theme();
        boolean heat = style == CellStyle.HEAT && maximum > 0;
        ChartColor background;
        if (heat) {
            double ratio = Math.max(0, Math.min(1, value / maximum));
            background = new ChartColor(
                    (int) Math.round(196 - ratio * 120),
                    (int) Math.round(216 - ratio * 96),
                    (int) Math.round(231 - ratio * 63));
        } else if (style == CellStyle.HEADER) {
            background = ChartDrawing.seriesColor(gc, 0);
        } else if (style == CellStyle.TOTAL || style == CellStyle.SUBTOTAL) {
            background = ChartDrawing.seriesColor(gc, style == CellStyle.TOTAL ? 7 : 9);
        } else {
            background = theme.background();
        }
        gc.setBackground(background);
        gc.fillRectangle(x, y, width, height);
        if (style == CellStyle.DATA_BAR && maximum > 0) {
            gc.setAlpha(90); gc.setBackground(ChartDrawing.seriesColor(gc, 0));
            gc.fillRectangle(x + 2, y + 4, (int) Math.round((width - 4) * Math.max(0, value) / maximum), height - 8);
            gc.setAlpha(255);
        }
        if (style == CellStyle.ALTERNATE) {
            gc.setAlpha(18);
            gc.setBackground(theme.selection());
            gc.fillRectangle(x, y, width, height);
            gc.setAlpha(255);
        }
        gc.setForeground(theme.normalShadow());
        gc.drawLine(x, y + height - 1, x + width, y + height - 1);
        String shown = abbreviate(text, Math.max(4, width / 8));
        if (heat) {
            gc.setForeground(theme.black());
        } else if (style == CellStyle.HEADER || style == CellStyle.TOTAL || style == CellStyle.SUBTOTAL) {
            gc.setForeground(background.luminance() < 125_000 ? theme.white() : theme.black());
        } else {
            gc.setForeground(theme.foreground());
        }
        int textX = numeric ? Math.max(x + 6, x + width - gc.textExtent(shown).width() - 7) : x + 7;
        gc.drawText(shown, textX, y + Math.max(1, (height - gc.textExtent(shown).height()) / 2));
    }

    private static String abbreviate(String text, int maximum) {
        if (text == null) return "";
        return text.length() > maximum ? text.substring(0, Math.max(1, maximum - 1)) + "…" : text;
    }

    private static String format(double value, MatrixDisplayOptions options) {
        String pattern = options.thousandsSeparator() ? "#,##0" : "0";
        if (options.decimalPlaces() > 0) pattern += "." + "0".repeat(options.decimalPlaces());
        double shown = options.percentage() ? value * 100 : value;
        return new DecimalFormat(pattern).format(shown) + (options.percentage() ? "%" : "");
    }

    private static List<List<String>> topRows(ChartDataset data, List<List<String>> rows, int topN) {
        if (topN <= 0 || rows.size() <= topN) return rows;
        return rows.stream().sorted((left, right) -> Double.compare(
                data.points().stream().filter(p -> p.rowLevels().equals(right)).mapToDouble(ChartPoint::y).sum(),
                data.points().stream().filter(p -> p.rowLevels().equals(left)).mapToDouble(ChartPoint::y).sum()))
                .limit(topN).toList();
    }

    private enum CellStyle { HEADER, BODY, ALTERNATE, HEAT, DATA_BAR, SUBTOTAL, TOTAL }
    private record DisplayRow(List<String> values, int subtotalLevel, boolean collapsed) {}
}
