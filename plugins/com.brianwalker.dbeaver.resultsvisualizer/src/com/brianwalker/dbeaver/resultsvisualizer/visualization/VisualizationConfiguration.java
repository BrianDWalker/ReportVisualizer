/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.Objects;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable Part 4 field-well and chart selection state. */
public record VisualizationConfiguration(
        ChartType chartType,
        List<Integer> xColumnIndexes,
        int valueColumnIndex,
        List<Integer> valueColumnIndexes,
        List<Integer> seriesColumnIndexes,
        Aggregation aggregation,
        Double yAxisMaximum,
        ChartDisplayOptions displayOptions,
        Map<Integer, Aggregation> valueAggregations) {

    public static final int UNASSIGNED = -1;

    public VisualizationConfiguration {
        chartType = Objects.requireNonNull(chartType, "chartType");
        xColumnIndexes = List.copyOf(Objects.requireNonNull(xColumnIndexes, "xColumnIndexes"));
        valueColumnIndexes = List.copyOf(Objects.requireNonNullElse(valueColumnIndexes,
                valueColumnIndex < 0 ? List.of() : List.of(valueColumnIndex)));
        seriesColumnIndexes = List.copyOf(Objects.requireNonNull(seriesColumnIndexes, "seriesColumnIndexes"));
        aggregation = Objects.requireNonNull(aggregation, "aggregation");
        Map<Integer, Aggregation> selectedAggregations = new LinkedHashMap<>();
        if (valueAggregations != null) for (Map.Entry<Integer, Aggregation> entry : valueAggregations.entrySet()) {
            Integer index = entry.getKey();
            Aggregation value = entry.getValue();
            if (index != null && value != null && valueColumnIndexes.contains(index)) {
                selectedAggregations.put(index, value);
            }
        }
        valueAggregations = Map.copyOf(selectedAggregations);
        if (valueColumnIndex < UNASSIGNED || xColumnIndexes.stream().anyMatch(i -> i < 0)
                || valueColumnIndexes.stream().anyMatch(i -> i < 0)
                || seriesColumnIndexes.stream().anyMatch(i -> i < 0)) {
            throw new IllegalArgumentException("Field indexes must be -1 or greater.");
        }
        if (yAxisMaximum != null && !Double.isFinite(yAxisMaximum)) {
            throw new IllegalArgumentException("Y-axis maximum must be finite.");
        }
        displayOptions = displayOptions == null ? ChartDisplayOptions.DEFAULT : displayOptions;
    }

    /** Retains the original constructor shape for callers and older saved state. */
    public VisualizationConfiguration(ChartType chartType, List<Integer> xColumnIndexes,
            int valueColumnIndex, List<Integer> valueColumnIndexes, List<Integer> seriesColumnIndexes,
            Aggregation aggregation, Double yAxisMaximum, ChartDisplayOptions displayOptions) {
        this(chartType, xColumnIndexes, valueColumnIndex, valueColumnIndexes, seriesColumnIndexes,
                aggregation, yAxisMaximum, displayOptions, Map.of());
    }

    public VisualizationConfiguration(ChartType chartType, List<Integer> xColumnIndexes,
            int valueColumnIndex, List<Integer> seriesColumnIndexes, Aggregation aggregation,
            Double yAxisMaximum) {
        this(chartType, xColumnIndexes, valueColumnIndex,
                valueColumnIndex < 0 ? List.of() : List.of(valueColumnIndex), seriesColumnIndexes,
                aggregation, yAxisMaximum, ChartDisplayOptions.DEFAULT);
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
        return !xColumnIndexes.isEmpty() && !valueColumnIndexes.isEmpty();
    }

    public int xColumnIndex() {
        return xColumnIndexes.isEmpty() ? UNASSIGNED : xColumnIndexes.get(0);
    }

    public int seriesColumnIndex() {
        return seriesColumnIndexes.isEmpty() ? UNASSIGNED : seriesColumnIndexes.get(0);
    }

    public VisualizationConfiguration withChartType(ChartType value) {
        return copy(value, xColumnIndexes, valueColumnIndex, valueColumnIndexes,
                seriesColumnIndexes, aggregation, yAxisMaximum, displayOptions, valueAggregations);
    }

    public VisualizationConfiguration withX(int index) {
        return withXColumns(indexes(index));
    }

    public VisualizationConfiguration withXColumns(List<Integer> indexes) {
        return copy(chartType, indexes, valueColumnIndex, valueColumnIndexes,
                seriesColumnIndexes, aggregation, yAxisMaximum, displayOptions, valueAggregations);
    }

    public VisualizationConfiguration withValue(int index) {
        return copy(chartType, xColumnIndexes, index, index < 0 ? List.of() : List.of(index),
                seriesColumnIndexes, aggregation, yAxisMaximum, displayOptions, Map.of());
    }

    public VisualizationConfiguration withValues(List<Integer> indexes) {
        int primary = indexes == null || indexes.isEmpty() ? UNASSIGNED : indexes.get(0);
        return copy(chartType, xColumnIndexes, primary, indexes, seriesColumnIndexes,
                aggregation, yAxisMaximum, displayOptions, valueAggregations);
    }

    public VisualizationConfiguration withSeries(int index) {
        return withSeriesColumns(indexes(index));
    }

    public VisualizationConfiguration withSeriesColumns(List<Integer> indexes) {
        return copy(chartType, xColumnIndexes, valueColumnIndex, valueColumnIndexes,
                indexes, aggregation, yAxisMaximum, displayOptions, valueAggregations);
    }

    public VisualizationConfiguration withAggregation(Aggregation value) {
        return copy(chartType, xColumnIndexes, valueColumnIndex, valueColumnIndexes,
                seriesColumnIndexes, value, yAxisMaximum, displayOptions, Map.of());
    }

    /** Returns the aggregation selected for this measure, or the legacy shared default. */
    public Aggregation aggregationFor(int valueIndex) {
        return valueAggregations.getOrDefault(valueIndex, aggregation);
    }

    public VisualizationConfiguration withValueAggregation(int valueIndex, Aggregation value) {
        if (!valueColumnIndexes.contains(valueIndex)) return this;
        Map<Integer, Aggregation> updated = new LinkedHashMap<>(valueAggregations);
        if (value == null || value == aggregation) updated.remove(valueIndex);
        else updated.put(valueIndex, value);
        return copy(chartType, xColumnIndexes, valueColumnIndex, valueColumnIndexes,
                seriesColumnIndexes, aggregation, yAxisMaximum, displayOptions, updated);
    }

    public VisualizationConfiguration withYAxisMaximum(Double value) {
        return copy(chartType, xColumnIndexes, valueColumnIndex, valueColumnIndexes,
                seriesColumnIndexes, aggregation, value, displayOptions, valueAggregations);
    }

    public VisualizationConfiguration withDisplayOptions(ChartDisplayOptions value) {
        return copy(chartType, xColumnIndexes, valueColumnIndex, valueColumnIndexes,
                seriesColumnIndexes, aggregation, yAxisMaximum, value, valueAggregations);
    }

    private static VisualizationConfiguration copy(ChartType chartType, List<Integer> xIndexes,
            int valueIndex, List<Integer> valueIndexes, List<Integer> seriesIndexes,
            Aggregation aggregation, Double maximum, ChartDisplayOptions options,
            Map<Integer, Aggregation> valueAggregations) {
        return new VisualizationConfiguration(chartType, xIndexes, valueIndex, valueIndexes,
                seriesIndexes, aggregation, maximum, options, valueAggregations);
    }

    private static List<Integer> indexes(int index) {
        return index < 0 ? List.of() : List.of(index);
    }
}
