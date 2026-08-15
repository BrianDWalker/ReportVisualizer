/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import com.brianwalker.dbeaver.resultsvisualizer.visualization.Aggregation;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartType;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.MatrixDisplayOptions;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A saved visualization layout keyed by immutable result metadata instead of DBeaver objects. */
public record VisualizerPreset(
        String name,
        String sourceSignature,
        ChartType chartType,
        List<Integer> xColumnIndexes,
        int valueColumnIndex,
        List<Integer> seriesColumnIndexes,
        Aggregation aggregation,
        Double yAxisMaximum,
        MatrixDisplayOptions matrixOptions) {

    public VisualizerPreset {
        name = Objects.requireNonNullElse(name, "").trim();
        sourceSignature = Objects.requireNonNullElse(sourceSignature, "");
        chartType = Objects.requireNonNull(chartType, "chartType");
        xColumnIndexes = List.copyOf(Objects.requireNonNullElse(xColumnIndexes, List.of()));
        seriesColumnIndexes = List.copyOf(Objects.requireNonNullElse(seriesColumnIndexes, List.of()));
        aggregation = Objects.requireNonNull(aggregation, "aggregation");
        matrixOptions = Objects.requireNonNullElse(matrixOptions, MatrixDisplayOptions.DEFAULT);
    }

    public static String sourceSignature(ResultSetSnapshot snapshot) {
        StringBuilder builder = new StringBuilder();
        builder.append(snapshot.sourceName()).append('|');
        for (ResultColumn column : snapshot.columns()) {
            builder.append(column.displayName())
                    .append(':')
                    .append(column.normalizedType().name())
                    .append(';');
        }
        return builder.toString();
    }

    public VisualizationConfiguration toConfiguration() {
        return new VisualizationConfiguration(chartType, xColumnIndexes, valueColumnIndex,
                seriesColumnIndexes, aggregation, yAxisMaximum);
    }

    public boolean matches(ResultSetSnapshot snapshot) {
        return sourceSignature.equals(sourceSignature(snapshot));
    }

    public String serialize() {
        List<String> values = new ArrayList<>();
        values.add(name);
        values.add(sourceSignature);
        values.add(chartType.name());
        values.add(joinInts(xColumnIndexes));
        values.add(Integer.toString(valueColumnIndex));
        values.add(joinInts(seriesColumnIndexes));
        values.add(aggregation.name());
        values.add(yAxisMaximum == null ? "" : Double.toString(yAxisMaximum));
        values.add(matrixOptions.rowTotals() + "," + matrixOptions.columnTotals() + "," + matrixOptions.subtotals());
        return String.join("\u001F", values);
    }

    public static VisualizerPreset deserialize(String name, String serialized) {
        String[] values = serialized.split("\u001F", -1);
        if (values.length < 9) {
            return null;
        }
        List<Integer> xIndexes = parseInts(values[3]);
        List<Integer> seriesIndexes = parseInts(values[5]);
        Double yAxisMaximum = values[7].isBlank() ? null : Double.valueOf(values[7]);
        String[] totals = values[8].split(",", -1);
        MatrixDisplayOptions options = totals.length == 3
                ? new MatrixDisplayOptions(Boolean.parseBoolean(totals[0]),
                        Boolean.parseBoolean(totals[1]), Boolean.parseBoolean(totals[2]))
                : MatrixDisplayOptions.DEFAULT;
        return new VisualizerPreset(
                name,
                values[1],
                ChartType.valueOf(values[2]),
                xIndexes,
                Integer.parseInt(values[4]),
                seriesIndexes,
                Aggregation.valueOf(values[6]),
                yAxisMaximum,
                options);
    }

    private static String joinInts(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse("");
    }

    private static List<Integer> parseInts(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Integer> indexes = new ArrayList<>();
        for (String part : value.split(",")) {
            if (!part.isBlank()) {
                indexes.add(Integer.parseInt(part));
            }
        }
        return List.copyOf(indexes);
    }
}
