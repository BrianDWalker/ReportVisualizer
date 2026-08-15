/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import com.brianwalker.dbeaver.resultsvisualizer.visualization.Aggregation;

/** One named database-side aggregation selected for a generated source query. */
public record QueryAggregation(String alias, QueryMeasure measure, Aggregation aggregation) {
    public QueryAggregation {
        if (alias == null || alias.isBlank()) throw new IllegalArgumentException("Output name is required.");
        if (measure == null) throw new IllegalArgumentException("Choose a field to aggregate.");
        if (aggregation == null) throw new IllegalArgumentException("Choose an aggregation.");
    }
}
