/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultRow;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

/** Selects sensible axes and converts snapshot values into renderable points. */
public final class ChartDataBuilder {
    private ChartDataBuilder() {
    }

    public static ChartConfiguration defaultConfiguration(ResultSetSnapshot snapshot) {
        int yIndex = firstNumericColumn(snapshot.columns(), -1);
        if (yIndex < 0) {
            yIndex = snapshot.columns().isEmpty() ? 0 : snapshot.columns().size() - 1;
        }
        int xIndex = firstNonNumericColumn(snapshot.columns(), yIndex);
        if (xIndex < 0) {
            xIndex = firstColumnOtherThan(snapshot.columns().size(), yIndex);
        }
        return new ChartConfiguration(ChartType.BAR, Math.max(0, xIndex), Math.max(0, yIndex));
    }

    public static ChartDataset build(
            ResultSetSnapshot snapshot, ChartConfiguration configuration) {
        if (snapshot.columns().isEmpty()) {
            return new ChartDataset("", "", List.of());
        }
        validateIndex(configuration.xColumnIndex(), snapshot.columns().size());
        validateIndex(configuration.yColumnIndex(), snapshot.columns().size());

        ResultColumn xColumn = snapshot.columns().get(configuration.xColumnIndex());
        ResultColumn yColumn = snapshot.columns().get(configuration.yColumnIndex());
        List<ChartPoint> points = new ArrayList<>();
        for (ResultRow row : snapshot.rows()) {
            if (row.values().size() <= Math.max(configuration.xColumnIndex(),
                    configuration.yColumnIndex())) continue;
            Object yValue = row.values().get(configuration.yColumnIndex());
            Double y = toFiniteDouble(yValue);
            if (y == null) continue;
            Object xValue = row.values().get(configuration.xColumnIndex());
            points.add(new ChartPoint(formatLabel(xValue), toFiniteDouble(xValue), y));
        }
        return new ChartDataset(xColumn.displayName(), yColumn.displayName(), points);
    }

    public static VisualizationConfiguration defaultVisualization(ResultSetSnapshot snapshot) {
        if (firstNumericColumn(snapshot.columns(), -1) < 0) {
            return VisualizationConfiguration.empty(ChartType.BAR);
        }
        ChartConfiguration defaults = defaultConfiguration(snapshot);
        return new VisualizationConfiguration(defaults.chartType(), defaults.xColumnIndex(),
                defaults.yColumnIndex(), VisualizationConfiguration.UNASSIGNED, Aggregation.SUM);
    }

    /** Groups and aggregates snapshot rows entirely inside the visualization layer. */
    public static ChartDataset build(
            ResultSetSnapshot snapshot, VisualizationConfiguration configuration) {
        if (snapshot.columns().isEmpty() || !configuration.isComplete()) {
            return new ChartDataset("", "", List.of());
        }
        configuration.xColumnIndexes().forEach(index -> validateIndex(index, snapshot.columns().size()));
        validateIndex(configuration.valueColumnIndex(), snapshot.columns().size());
        configuration.seriesColumnIndexes().forEach(index -> validateIndex(index, snapshot.columns().size()));

        ResultColumn valueColumn = snapshot.columns().get(configuration.valueColumnIndex());
        Map<GroupKey, Accumulator> groups = new LinkedHashMap<>();
        int requiredIndex = Math.max(configuration.valueColumnIndex(), configuration.xColumnIndexes().stream()
                .mapToInt(Integer::intValue).max().orElse(-1));
        requiredIndex = Math.max(requiredIndex, configuration.seriesColumnIndexes().stream()
                .mapToInt(Integer::intValue).max().orElse(-1));
        for (ResultRow row : snapshot.rows()) {
            if (row.values().size() <= requiredIndex) continue;
            List<String> rowLevels = levelValues(row, configuration.xColumnIndexes());
            List<String> columnLevels = levelValues(row, configuration.seriesColumnIndexes());
            Object xValue = compositeValue(rowLevels);
            Object seriesValue = compositeValue(columnLevels);
            Object value = row.values().get(configuration.valueColumnIndex());
            Double numericValue = toFiniteDouble(value);
            if (!configuration.aggregation().isCount() && numericValue == null) continue;
            if (configuration.aggregation().isCount() && value == null) continue;

            GroupKey key = new GroupKey(formatLabel(xValue), toFiniteDouble(xValue),
                    configuration.seriesColumnIndexes().isEmpty() ? "" : formatLabel(seriesValue),
                    rowLevels, columnLevels);
            groups.computeIfAbsent(key, ignored -> new Accumulator()).add(value, numericValue);
        }

        List<ChartPoint> points = new ArrayList<>();
        groups.forEach((key, accumulator) -> points.add(new ChartPoint(
                key.label(), key.numericX(), accumulator.value(configuration.aggregation()),
                key.series(), key.rowLevels(), key.columnLevels())));
        String yTitle = configuration.aggregation() + "(" + valueColumn.displayName() + ")";
        return new ChartDataset(joinColumnNames(snapshot, configuration.xColumnIndexes()),
                yTitle, points, configuration.yAxisMaximum(),
                columnNames(snapshot, configuration.xColumnIndexes()),
                columnNames(snapshot, configuration.seriesColumnIndexes()),
                MatrixDisplayOptions.DEFAULT);
    }

    /** Builds a pivot dataset with one measure level for each selected numeric value. */
    public static ChartDataset buildMatrixValues(ResultSetSnapshot snapshot,
            VisualizationConfiguration configuration, List<Integer> valueIndexes) {
        if (valueIndexes == null || valueIndexes.isEmpty() || configuration.xColumnIndexes().isEmpty()) {
            return new ChartDataset("", "", List.of());
        }
        List<ChartPoint> combined = new ArrayList<>();
        ChartDataset first = null;
        List<String> valueNames = new ArrayList<>();
        for (int valueIndex : valueIndexes.stream().distinct().toList()) {
            validateIndex(valueIndex, snapshot.columns().size());
            VisualizationConfiguration selected = configuration.withValue(valueIndex);
            ChartDataset dataset = build(snapshot, selected);
            if (first == null) first = dataset;
            String valueName = snapshot.columns().get(valueIndex).displayName();
            valueNames.add(valueName);
            for (ChartPoint point : dataset.points()) {
                List<String> columnLevels = new ArrayList<>(point.columnLevels());
                columnLevels.add(valueName);
                combined.add(new ChartPoint(point.label(), point.numericX(), point.y(), point.series(),
                        point.rowLevels(), columnLevels));
            }
        }
        if (first == null) return new ChartDataset("", "", List.of());
        List<String> columnNames = new ArrayList<>(first.columnLevelNames());
        columnNames.add("Values");
        String yTitle = valueNames.stream().reduce((left, right) -> left + " / " + right).orElse("Values");
        return new ChartDataset(first.xAxisTitle(), yTitle, combined, configuration.yAxisMaximum(),
                first.rowLevelNames(), columnNames, MatrixDisplayOptions.DEFAULT);
    }

    public static boolean isNumeric(ResultColumn column) {
        return switch (column.normalizedType()) {
            case INTEGER, DECIMAL, NUMBER -> true;
            default -> false;
        };
    }

    public static int firstNumericColumn(List<ResultColumn> columns, int excludedIndex) {
        for (int index = 0; index < columns.size(); index++) {
            if (index != excludedIndex && isNumeric(columns.get(index))) return index;
        }
        return -1;
    }

    private static int firstNonNumericColumn(List<ResultColumn> columns, int excludedIndex) {
        for (int index = 0; index < columns.size(); index++) {
            if (index != excludedIndex && !isNumeric(columns.get(index))) return index;
        }
        return -1;
    }

    private static int firstColumnOtherThan(int columnCount, int excludedIndex) {
        for (int index = 0; index < columnCount; index++) {
            if (index != excludedIndex) return index;
        }
        return columnCount == 0 ? -1 : 0;
    }

    private static void validateIndex(int index, int columnCount) {
        if (index < 0 || index >= columnCount) {
            throw new IllegalArgumentException("Chart column index is outside the snapshot.");
        }
    }

    static Double toFiniteDouble(Object value) {
        if (value == null || value instanceof Boolean || value instanceof TemporalAccessor) return null;
        double number;
        if (value instanceof Number numeric) {
            number = numeric.doubleValue();
        } else {
            try {
                number = new BigDecimal(value.toString().trim()).doubleValue();
            } catch (NumberFormatException error) {
                return null;
            }
        }
        return Double.isFinite(number) ? number : null;
    }

    private static String formatLabel(Object value) {
        return value == null ? "(null)" : value.toString();
    }

    private static List<String> levelValues(ResultRow row, List<Integer> indexes) {
        return indexes.stream().map(index -> formatLabel(row.values().get(index))).toList();
    }

    private static Object compositeValue(List<String> levels) {
        return levels.stream().reduce((left, right) -> left + " › " + right).orElse("");
    }

    private static String joinColumnNames(ResultSetSnapshot snapshot, List<Integer> indexes) {
        return indexes.stream().map(index -> snapshot.columns().get(index).displayName())
                .reduce((left, right) -> left + " / " + right).orElse("");
    }

    private static List<String> columnNames(ResultSetSnapshot snapshot, List<Integer> indexes) {
        return indexes.stream().map(index -> snapshot.columns().get(index).displayName()).toList();
    }

    private record GroupKey(String label, Double numericX, String series,
            List<String> rowLevels, List<String> columnLevels) {
        private GroupKey {
            rowLevels = List.copyOf(rowLevels);
            columnLevels = List.copyOf(columnLevels);
        }
    }

    private static final class Accumulator {
        private double sum;
        private double minimum = Double.POSITIVE_INFINITY;
        private double maximum = Double.NEGATIVE_INFINITY;
        private int count;
        private final Set<Object> distinct = new LinkedHashSet<>();

        void add(Object rawValue, Double value) {
            count++;
            distinct.add(rawValue);
            if (value == null) return;
            sum += value;
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
        }

        double value(Aggregation aggregation) {
            return switch (aggregation) {
                case SUM -> sum;
                case AVG -> count == 0 ? 0 : sum / count;
                case MIN -> minimum;
                case MAX -> maximum;
                case COUNT -> count;
                case COUNT_DISTINCT -> distinct.size();
            };
        }
    }
}
