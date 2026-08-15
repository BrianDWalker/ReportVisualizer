/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.Objects;

/** Immutable selection of chart type and source fields. */
public record ChartConfiguration(ChartType chartType, int xColumnIndex, int yColumnIndex) {
    public ChartConfiguration {
        chartType = Objects.requireNonNull(chartType, "chartType");
        if (xColumnIndex < 0 || yColumnIndex < 0) {
            throw new IllegalArgumentException("Chart column indexes must be zero or greater.");
        }
    }
}
