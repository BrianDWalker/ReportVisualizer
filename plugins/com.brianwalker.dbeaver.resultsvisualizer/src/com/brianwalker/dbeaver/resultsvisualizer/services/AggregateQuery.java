/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import java.util.List;

/** Generated SQL plus the output fields used to restore the visualization. */
public record AggregateQuery(
        String sql, List<String> rowAliases, List<String> columnAliases, String valueAlias) {
    public AggregateQuery {
        rowAliases = List.copyOf(rowAliases);
        columnAliases = List.copyOf(columnAliases);
    }
}
