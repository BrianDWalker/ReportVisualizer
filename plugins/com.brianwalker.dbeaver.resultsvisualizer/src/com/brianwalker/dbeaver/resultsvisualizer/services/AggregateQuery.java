/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import com.brianwalker.dbeaver.resultsvisualizer.visualization.Aggregation;
import java.util.List;
import java.util.Optional;

/** Generated SQL plus the output fields used to restore the visualization. */
public record AggregateQuery(
        String sql, List<String> rowAliases, List<String> columnAliases, String valueAlias,
        DBeaverSqlDialectService.QueryStrategy strategy,
        List<QueryAggregation> aggregations) {
    public AggregateQuery {
        rowAliases = List.copyOf(rowAliases);
        columnAliases = List.copyOf(columnAliases);
        aggregations = aggregations == null ? List.of() : List.copyOf(aggregations);
    }

    public AggregateQuery(String sql, List<String> rowAliases, List<String> columnAliases, String valueAlias) {
        this(sql, rowAliases, columnAliases, valueAlias,
                DBeaverSqlDialectService.QueryStrategy.DERIVED_TABLE_FALLBACK, List.of());
    }

    public AggregateQuery(String sql, List<String> rowAliases, List<String> columnAliases,
            String valueAlias, DBeaverSqlDialectService.QueryStrategy strategy) {
        this(sql, rowAliases, columnAliases, valueAlias, strategy, List.of());
    }

    public Optional<Aggregation> aggregationFor(String alias) {
        return aggregations.stream()
                .filter(value -> value.alias().equalsIgnoreCase(alias))
                .map(QueryAggregation::aggregation).findFirst();
    }

    /** Local operation that preserves the meaning of this source-level output. */
    public Aggregation localAggregationFor(String alias) {
        return aggregationFor(alias).map(value -> switch (value) {
            case COUNT -> Aggregation.SUM;
            default -> value;
        }).orElse(Aggregation.SUM);
    }

    /** AVG and COUNT DISTINCT cannot be combined across changed dimensions without extra state. */
    public boolean requiresExactDimensions(String alias) {
        return aggregationFor(alias)
                .map(value -> value == Aggregation.AVG || value == Aggregation.COUNT_DISTINCT)
                .orElse(false);
    }
}
