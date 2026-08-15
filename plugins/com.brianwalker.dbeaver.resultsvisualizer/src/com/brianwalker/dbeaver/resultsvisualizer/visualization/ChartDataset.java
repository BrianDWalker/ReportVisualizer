/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.List;
import java.util.Objects;

/** DBeaver-independent chart data and axis metadata. */
public record ChartDataset(
        String xAxisTitle, String yAxisTitle, List<ChartPoint> points, Double yAxisMaximum,
        List<String> rowLevelNames, List<String> columnLevelNames,
        MatrixDisplayOptions matrixOptions) {
    public ChartDataset {
        xAxisTitle = Objects.requireNonNullElse(xAxisTitle, "");
        yAxisTitle = Objects.requireNonNullElse(yAxisTitle, "");
        points = List.copyOf(points);
        rowLevelNames = List.copyOf(rowLevelNames == null ? List.of(xAxisTitle) : rowLevelNames);
        columnLevelNames = List.copyOf(columnLevelNames == null ? List.of() : columnLevelNames);
        matrixOptions = matrixOptions == null ? MatrixDisplayOptions.DEFAULT : matrixOptions;
        if (yAxisMaximum != null && !Double.isFinite(yAxisMaximum)) {
            throw new IllegalArgumentException("Y-axis maximum must be finite.");
        }
    }

    public ChartDataset(String xAxisTitle, String yAxisTitle, List<ChartPoint> points) {
        this(xAxisTitle, yAxisTitle, points, null, List.of(xAxisTitle), List.of(), MatrixDisplayOptions.DEFAULT);
    }

    public ChartDataset(String xAxisTitle, String yAxisTitle, List<ChartPoint> points,
            Double yAxisMaximum) {
        this(xAxisTitle, yAxisTitle, points, yAxisMaximum,
                List.of(xAxisTitle), List.of(), MatrixDisplayOptions.DEFAULT);
    }

    public ChartDataset withMatrixOptions(MatrixDisplayOptions value) {
        return new ChartDataset(xAxisTitle, yAxisTitle, points, yAxisMaximum,
                rowLevelNames, columnLevelNames, value);
    }

    public boolean hasNumericX() {
        return !points.isEmpty() && points.stream().allMatch(point -> point.numericX() != null);
    }

    public List<String> categories() {
        return points.stream().map(ChartPoint::label).distinct().toList();
    }

    public List<String> seriesNames() {
        return points.stream().map(ChartPoint::series).distinct().toList();
    }

    public List<List<String>> rowTuples() {
        return points.stream().map(ChartPoint::rowLevels).distinct().toList();
    }

    public List<List<String>> columnTuples() {
        return points.stream().map(ChartPoint::columnLevels).distinct().toList();
    }

    public int rowLevelCount() {
        return points.stream().mapToInt(point -> point.rowLevels().size()).max().orElse(1);
    }

    public int columnLevelCount() {
        return points.stream().mapToInt(point -> point.columnLevels().size()).max().orElse(1);
    }

    public List<ChartPoint> pointsForSeries(String series) {
        return points.stream().filter(point -> point.series().equals(series)).toList();
    }
}
