/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.Objects;
import java.util.List;

/** Immutable Part 4 field-well and chart selection state. */
public record VisualizationConfiguration(
        ChartType chartType,
        List<Integer> xColumnIndexes,
        int valueColumnIndex,
        List<Integer> seriesColumnIndexes,
        Aggregation aggregation,
        Double yAxisMaximum) {

    public static final int UNASSIGNED = -1;

    public VisualizationConfiguration {
        chartType = Objects.requireNonNull(chartType, "chartType");
        xColumnIndexes = List.copyOf(Objects.requireNonNull(xColumnIndexes, "xColumnIndexes"));
        seriesColumnIndexes = List.copyOf(Objects.requireNonNull(seriesColumnIndexes, "seriesColumnIndexes"));
        aggregation = Objects.requireNonNull(aggregation, "aggregation");
        if (valueColumnIndex < UNASSIGNED || xColumnIndexes.stream().anyMatch(i -> i < 0)
                || seriesColumnIndexes.stream().anyMatch(i -> i < 0)) {
            throw new IllegalArgumentException("Field indexes must be -1 or greater.");
        }
        if (yAxisMaximum != null && !Double.isFinite(yAxisMaximum)) {
            throw new IllegalArgumentException("Y-axis maximum must be finite.");
        }
    }

    public VisualizationConfiguration(ChartType chartType, int xColumnIndex,
            int valueColumnIndex, int seriesColumnIndex, Aggregation aggregation) {
        this(chartType, indexes(xColumnIndex), valueColumnIndex, indexes(seriesColumnIndex), aggregation, null);
    }

    public VisualizationConfiguration(ChartType chartType, int xColumnIndex,
            int valueColumnIndex, int seriesColumnIndex, Aggregation aggregation, Double yAxisMaximum) {
        this(chartType, indexes(xColumnIndex), valueColumnIndex, indexes(seriesColumnIndex), aggregation, yAxisMaximum);
    }

    public static VisualizationConfiguration empty(ChartType chartType) {
        return new VisualizationConfiguration(
                chartType, List.of(), UNASSIGNED, List.of(), Aggregation.SUM, null);
    }

    public boolean isComplete() {
        return !xColumnIndexes.isEmpty() && valueColumnIndex >= 0;
    }

    public int xColumnIndex() {
        return xColumnIndexes.isEmpty() ? UNASSIGNED : xColumnIndexes.get(0);
    }

    public int seriesColumnIndex() {
        return seriesColumnIndexes.isEmpty() ? UNASSIGNED : seriesColumnIndexes.get(0);
    }

    public VisualizationConfiguration withChartType(ChartType value) {
        return new VisualizationConfiguration(value, xColumnIndexes, valueColumnIndex,
                seriesColumnIndexes, aggregation, yAxisMaximum);
    }

    public VisualizationConfiguration withX(int index) {
        return withXColumns(indexes(index));
    }

    public VisualizationConfiguration withXColumns(List<Integer> indexes) {
        return new VisualizationConfiguration(chartType, indexes, valueColumnIndex,
                seriesColumnIndexes, aggregation, yAxisMaximum);
    }

    public VisualizationConfiguration withValue(int index) {
        return new VisualizationConfiguration(chartType, xColumnIndexes, index,
                seriesColumnIndexes, aggregation, yAxisMaximum);
    }

    public VisualizationConfiguration withSeries(int index) {
        return withSeriesColumns(indexes(index));
    }

    public VisualizationConfiguration withSeriesColumns(List<Integer> indexes) {
        return new VisualizationConfiguration(chartType, xColumnIndexes, valueColumnIndex,
                indexes, aggregation, yAxisMaximum);
    }

    public VisualizationConfiguration withAggregation(Aggregation value) {
        return new VisualizationConfiguration(chartType, xColumnIndexes, valueColumnIndex,
                seriesColumnIndexes, value, yAxisMaximum);
    }

    public VisualizationConfiguration withYAxisMaximum(Double value) {
        return new VisualizationConfiguration(chartType, xColumnIndexes, valueColumnIndex,
                seriesColumnIndexes, aggregation, value);
    }

    private static List<Integer> indexes(int index) {
        return index < 0 ? List.of() : List.of(index);
    }
}
