/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

/** Chart types available in the Part 3 renderer. */
public enum ChartType {
    BAR("Bar"),
    HORIZONTAL_BAR("Horizontal Bar"),
    STACKED_BAR("Stacked Bar"),
    STACKED_100_BAR("100% Stacked Bar"),
    LINE("Line"),
    AREA("Area"),
    STACKED_AREA("Stacked Area"),
    STACKED_100_AREA("100% Stacked Area"),
    COMBO("Column + Line"),
    SCATTER("Scatter"),
    BUBBLE("Bubble"),
    PIE("Pie"),
    DONUT("Donut"),
    HEATMAP("Heatmap"),
    MATRIX("Matrix / Pivot Table");

    private final String displayName;

    ChartType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
