/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.Objects;
import java.util.List;

/** Portable point prepared for chart rendering, optionally assigned to a series. */
public record ChartPoint(String label, Double numericX, double y, String series,
        List<String> rowLevels, List<String> columnLevels) {
    public ChartPoint {
        label = Objects.requireNonNullElse(label, "");
        series = Objects.requireNonNullElse(series, "");
        rowLevels = List.copyOf(rowLevels == null ? List.of(label) : rowLevels);
        columnLevels = List.copyOf(columnLevels == null
                ? series.isBlank() ? List.of() : List.of(series) : columnLevels);
    }

    public ChartPoint(String label, Double numericX, double y, String series) {
        this(label, numericX, y, series, List.of(label),
                series == null || series.isBlank() ? List.of() : List.of(series));
    }

    public ChartPoint(String label, Double numericX, double y) {
        this(label, numericX, y, "");
    }
}
