/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

/** Compact, renderer-independent presentation choices for charts. */
public record ChartDisplayOptions(boolean dataLabels, boolean markers, boolean secondaryAxis,
        LegendPosition legendPosition, PieLabelMode pieLabelMode, int topN) {
    public static final ChartDisplayOptions DEFAULT = new ChartDisplayOptions(true, true, false,
            LegendPosition.TOP, PieLabelMode.CATEGORY_PERCENT, 0);

    public ChartDisplayOptions(boolean dataLabels, boolean markers,
            LegendPosition legendPosition, PieLabelMode pieLabelMode, int topN) {
        this(dataLabels, markers, false, legendPosition, pieLabelMode, topN);
    }

    public ChartDisplayOptions {
        legendPosition = legendPosition == null ? LegendPosition.TOP : legendPosition;
        pieLabelMode = pieLabelMode == null ? PieLabelMode.CATEGORY_PERCENT : pieLabelMode;
        if (topN < 0 || topN > 100) throw new IllegalArgumentException("Top N must be between 0 and 100.");
    }

    public ChartDisplayOptions withDataLabels(boolean value) {
        return new ChartDisplayOptions(value, markers, secondaryAxis, legendPosition, pieLabelMode, topN);
    }
    public ChartDisplayOptions withMarkers(boolean value) {
        return new ChartDisplayOptions(dataLabels, value, secondaryAxis, legendPosition, pieLabelMode, topN);
    }
    public ChartDisplayOptions withSecondaryAxis(boolean value) {
        return new ChartDisplayOptions(dataLabels, markers, value, legendPosition, pieLabelMode, topN);
    }
    public ChartDisplayOptions withLegendPosition(LegendPosition value) {
        return new ChartDisplayOptions(dataLabels, markers, secondaryAxis, value, pieLabelMode, topN);
    }
    public ChartDisplayOptions withPieLabelMode(PieLabelMode value) {
        return new ChartDisplayOptions(dataLabels, markers, secondaryAxis, legendPosition, value, topN);
    }
    public ChartDisplayOptions withTopN(int value) {
        return new ChartDisplayOptions(dataLabels, markers, secondaryAxis, legendPosition, pieLabelMode, value);
    }

    public enum LegendPosition { TOP, RIGHT, NONE }
    public enum PieLabelMode { CATEGORY, VALUE, PERCENT, CATEGORY_PERCENT }
}
