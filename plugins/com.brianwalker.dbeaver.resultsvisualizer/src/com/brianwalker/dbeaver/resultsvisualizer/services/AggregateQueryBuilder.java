/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SortRule;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationConfiguration;
import java.util.ArrayList;
import java.util.List;

/** Generates a reviewable read-only aggregate query around the original SQL. */
public final class AggregateQueryBuilder {
    private AggregateQueryBuilder() {}

    public static String build(String sourceSql, ResultSetSnapshot snapshot,
            VisualizationConfiguration configuration, List<CustomSqlDimension> dimensions,
            List<SlicerDefinition> slicers) {
        List<QueryDimension> rows = new ArrayList<>(configuration.xColumnIndexes().stream()
                .map(index -> resultDimension(snapshot, index)).toList());
        dimensions.stream().map(AggregateQueryBuilder::customDimension).forEach(rows::add);
        List<QueryDimension> columns = configuration.seriesColumnIndexes().stream()
                .map(index -> resultDimension(snapshot, index)).toList();
        return buildQuery(sourceSql, snapshot, configuration, rows, columns, slicers, List.of()).sql();
    }

    public static AggregateQuery buildQuery(String sourceSql, ResultSetSnapshot snapshot,
            VisualizationConfiguration configuration, List<QueryDimension> rows,
            List<QueryDimension> columns, List<SlicerDefinition> slicers) {
        return buildQuery(sourceSql, snapshot, configuration, rows, columns, slicers, List.of());
    }

    public static AggregateQuery buildQuery(String sourceSql, ResultSetSnapshot snapshot,
            VisualizationConfiguration configuration, List<QueryDimension> rows,
            List<QueryDimension> columns, List<SlicerDefinition> slicers,
            List<SortRule> sortRules) {
        if (configuration.valueColumnIndex() < 0
                || configuration.valueColumnIndex() >= snapshot.columns().size()) {
            throw new IllegalArgumentException("Choose a Values field first.");
        }
        return buildQuery(sourceSql, configuration, rows, columns, slicers, sortRules,
                resultMeasure(snapshot, configuration.valueColumnIndex()));
    }

    public static AggregateQuery buildQuery(String sourceSql,
            VisualizationConfiguration configuration, List<QueryDimension> rows,
            List<QueryDimension> columns, List<SlicerDefinition> slicers,
            List<SortRule> sortRules, QueryMeasure measure) {
        String alias = configuration.aggregation().name().toLowerCase() + "_" + measure.alias();
        return buildQuery(sourceSql, rows, columns, slicers, sortRules,
                List.of(new QueryAggregation(alias, measure, configuration.aggregation())));
    }

    public static AggregateQuery buildQuery(String sourceSql, List<QueryDimension> rows,
            List<QueryDimension> columns, List<SlicerDefinition> slicers,
            List<SortRule> sortRules, List<QueryAggregation> aggregations) {
        if (sourceSql == null || sourceSql.isBlank()) throw new IllegalArgumentException("Original query text is unavailable.");
        if (rows.isEmpty()) throw new IllegalArgumentException("Choose at least one Available Field.");
        if (aggregations.isEmpty()) throw new IllegalArgumentException("Add at least one aggregation.");
        long uniqueAliases = aggregations.stream().map(value -> value.alias().toLowerCase()).distinct().count();
        if (uniqueAliases != aggregations.size()) throw new IllegalArgumentException("Aggregation output names must be unique.");

        List<QueryDimension> allDimensions = new ArrayList<>(rows);
        allDimensions.addAll(columns);
        StringBuilder sql = new StringBuilder("SELECT\n    ");
        sql.append(allDimensions.stream().map(d -> d.expression() + " AS " + quote(d.alias()))
                .collect(java.util.stream.Collectors.joining(",\n    ")));
        for (QueryAggregation selection : aggregations) {
            String expression = selection.measure().expression();
            String aggregate = selection.aggregation() == com.brianwalker.dbeaver.resultsvisualizer.visualization.Aggregation.COUNT_DISTINCT
                    ? "COUNT(DISTINCT " + expression + ")"
                    : selection.aggregation().name() + "(" + expression + ")";
            sql.append(",\n    ").append(aggregate).append(" AS ").append(quote(selection.alias()));
        }
        sql.append("\nFROM (\n").append(stripTerminator(sourceSql)).append("\n) rv_source");
        List<String> predicates = predicates(slicers);
        if (!predicates.isEmpty()) sql.append("\nWHERE ").append(String.join("\n  AND ", predicates));
        sql.append("\nGROUP BY ").append(allDimensions.stream().map(QueryDimension::expression)
                .collect(java.util.stream.Collectors.joining(", ")));
        List<String> orderBy = orderBy(sortRules, allDimensions, aggregations);
        if (orderBy.isEmpty()) {
            for (int index = 1; index <= allDimensions.size(); index++) orderBy.add(Integer.toString(index));
        }
        sql.append("\nORDER BY ").append(String.join(", ", orderBy));
        sql.append(";");
        return new AggregateQuery(sql.toString(), rows.stream().map(QueryDimension::alias).toList(),
                columns.stream().map(QueryDimension::alias).toList(), aggregations.get(0).alias());
    }

    public static QueryMeasure resultMeasure(ResultSetSnapshot snapshot, int index) {
        String name = snapshot.columns().get(index).displayName();
        return new QueryMeasure(name, quote(name));
    }

    public static QueryMeasure customMeasure(CustomSqlDimension field) {
        return new QueryMeasure(field.name(), field.expression());
    }

    private static List<String> orderBy(List<SortRule> rules,
            List<QueryDimension> dimensions, List<QueryAggregation> aggregations) {
        List<String> result = new ArrayList<>();
        for (SortRule rule : rules) {
            String alias = dimensions.stream()
                    .map(QueryDimension::alias)
                    .filter(name -> name.equalsIgnoreCase(rule.fieldName()))
                    .findFirst().orElse(null);
            if (alias == null) {
                alias = aggregations.stream()
                        .filter(value -> value.alias().equalsIgnoreCase(rule.fieldName())
                                || value.measure().alias().equalsIgnoreCase(rule.fieldName()))
                        .map(QueryAggregation::alias).findFirst().orElse(null);
            }
            if (alias != null) result.add(quote(alias) + " " + rule.direction());
        }
        return result;
    }

    public static String distinct(String sourceSql, String fieldName) {
        return "SELECT DISTINCT " + quote(fieldName) + "\nFROM (\n"
                + stripTerminator(sourceSql) + "\n) rv_source\nORDER BY 1;";
    }

    private static List<String> predicates(List<SlicerDefinition> slicers) {
        List<String> result = new ArrayList<>();
        for (SlicerDefinition slicer : slicers) {
            List<String> values = slicer.selectedValues().stream()
                    .filter(value -> !value.equals("(null)"))
                    .map(value -> "'" + value.replace("'", "''") + "'").toList();
            boolean includesNull = slicer.selectedValues().contains("(null)");
            String field = quote(slicer.fieldName());
            String valuePredicate = values.isEmpty() ? "" : field + " IN (" + String.join(", ", values) + ")";
            result.add(includesNull && !valuePredicate.isEmpty() ? "(" + valuePredicate + " OR " + field + " IS NULL)"
                    : includesNull ? field + " IS NULL" : valuePredicate);
        }
        return result.stream().filter(value -> !value.isBlank()).toList();
    }

    public static QueryDimension resultDimension(ResultSetSnapshot snapshot, int index) {
        String name = snapshot.columns().get(index).displayName();
        return new QueryDimension(name, quote(name));
    }

    public static QueryDimension customDimension(CustomSqlDimension dimension) {
        return new QueryDimension(dimension.name(), dimension.expression());
    }

    private static String stripTerminator(String sql) {
        String value = sql.trim();
        return value.endsWith(";") ? value.substring(0, value.length() - 1) : value;
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
