/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import com.brianwalker.dbeaver.resultsvisualizer.services.AggregateQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Maps an executed source-query aggregate's aliases back onto its returned result schema. */
public record AggregateResultBinding(VisualizationConfiguration configuration, List<Integer> rows,
        List<Integer> columns, List<Integer> values) {
    public static Optional<AggregateResultBinding> bind(VisualizationConfiguration current,
            AggregateQuery query, ResultSetSnapshot result) {
        if (current == null || query == null || result == null) return Optional.empty();
        List<Integer> rows = indexes(result, query.rowAliases());
        List<Integer> columns = indexes(result, query.columnAliases());
        int value = index(result, query.valueAlias());
        if (rows.size() != query.rowAliases().size() || columns.size() != query.columnAliases().size() || value < 0) return Optional.empty();
        List<Integer> values = new ArrayList<>();
        for (int column = 0; column < result.columns().size(); column++) {
            if (!rows.contains(column) && !columns.contains(column) && ChartDataBuilder.isNumeric(result.columns().get(column))) values.add(column);
        }
        if (values.isEmpty()) values.add(value);
        return Optional.of(new AggregateResultBinding(new VisualizationConfiguration(current.chartType(), rows, value,
                columns, query.localAggregationFor(query.valueAlias()), current.yAxisMaximum()),
                List.copyOf(rows), List.copyOf(columns), List.copyOf(values)));
    }

    private static List<Integer> indexes(ResultSetSnapshot snapshot, List<String> names) {
        List<Integer> result = new ArrayList<>(); for (String name : names) { int index = index(snapshot, name); if (index >= 0) result.add(index); } return result;
    }
    private static int index(ResultSetSnapshot snapshot, String name) {
        for (int index = 0; index < snapshot.columns().size(); index++) if (snapshot.columns().get(index).displayName().equalsIgnoreCase(name)) return index;
        return -1;
    }
}
