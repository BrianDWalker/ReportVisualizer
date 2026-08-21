/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.Objects;
import java.util.Set;

/** Complete persisted Matrix layout, hierarchy, totals, and formatting state. */
public record MatrixDisplayOptions(boolean rowTotals, boolean columnTotals, boolean subtotals,
        boolean grandTotals, Layout layout, int decimalPlaces, boolean percentage,
        boolean thousandsSeparator, ConditionalFormat conditionalFormat, boolean dataBars,
        int topN, int columnWidth, Set<Integer> subtotalLevels, Set<String> collapsedRowPaths) {
    public static final MatrixDisplayOptions DEFAULT = new MatrixDisplayOptions(true, true, false,
            true, Layout.STEPPED, 2, false, true, ConditionalFormat.NONE, false,
            0, 110, Set.of(), Set.of());

    public MatrixDisplayOptions(boolean rowTotals, boolean columnTotals, boolean subtotals) {
        this(rowTotals, columnTotals, subtotals, true, Layout.STEPPED, 2, false, true,
                ConditionalFormat.NONE, false, 0, 110, Set.of(), Set.of());
    }

    public MatrixDisplayOptions {
        layout = Objects.requireNonNullElse(layout, Layout.STEPPED);
        conditionalFormat = Objects.requireNonNullElse(conditionalFormat, ConditionalFormat.NONE);
        subtotalLevels = Set.copyOf(Objects.requireNonNullElse(subtotalLevels, Set.of()));
        collapsedRowPaths = Set.copyOf(Objects.requireNonNullElse(collapsedRowPaths, Set.of()));
        if (decimalPlaces < 0 || decimalPlaces > 8) throw new IllegalArgumentException("Decimal places must be 0-8.");
        if (topN < 0 || topN > 1_000) throw new IllegalArgumentException("Top N must be 0-1000.");
        if (columnWidth < 72 || columnWidth > 320) throw new IllegalArgumentException("Column width must be 72-320.");
    }

    public MatrixDisplayOptions withCollapsedRowPaths(Set<String> value) {
        return new MatrixDisplayOptions(rowTotals, columnTotals, subtotals, grandTotals, layout,
                decimalPlaces, percentage, thousandsSeparator, conditionalFormat, dataBars,
                topN, columnWidth, subtotalLevels, value);
    }
    public MatrixDisplayOptions withTotals(boolean rows, boolean columns, boolean subtotalValues) {
        return new MatrixDisplayOptions(rows, columns, subtotalValues, grandTotals, layout,
                decimalPlaces, percentage, thousandsSeparator, conditionalFormat, dataBars,
                topN, columnWidth, subtotalLevels, collapsedRowPaths);
    }

    public enum Layout { STEPPED, TABULAR }
    public enum ConditionalFormat { NONE, COLOR_SCALE, DATA_BARS }
}
