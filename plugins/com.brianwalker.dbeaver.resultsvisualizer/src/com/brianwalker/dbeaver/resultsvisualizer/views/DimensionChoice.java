/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import com.brianwalker.dbeaver.resultsvisualizer.services.AggregateQueryBuilder;
import com.brianwalker.dbeaver.resultsvisualizer.services.CustomSqlDimension;
import com.brianwalker.dbeaver.resultsvisualizer.services.QueryDimension;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.DateHierarchyLevel;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.DateHierarchySelection;

/** A result field or custom database expression available in a dimension well. */
record DimensionChoice(String displayName, Integer resultIndex, CustomSqlDimension custom,
        DateHierarchySelection dateHierarchy) {
    static DimensionChoice result(ResultSetSnapshot snapshot, int index) {
        return new DimensionChoice(snapshot.columns().get(index).displayName(), index, null, null);
    }

    static DimensionChoice custom(CustomSqlDimension dimension) {
        return new DimensionChoice(dimension.name(), null, dimension, null);
    }

    static DimensionChoice dateHierarchy(ResultSetSnapshot snapshot, int index, DateHierarchyLevel level) {
        return new DimensionChoice(snapshot.columns().get(index).displayName() + " [" + level + "]",
                index, null, new DateHierarchySelection(index, level));
    }

    boolean isCustom() { return custom != null; }
    boolean isDateHierarchy() { return dateHierarchy != null; }

    QueryDimension queryDimension(ResultSetSnapshot snapshot) {
        return isCustom() ? AggregateQueryBuilder.customDimension(custom)
                : AggregateQueryBuilder.resultDimension(snapshot, resultIndex);
    }
}
