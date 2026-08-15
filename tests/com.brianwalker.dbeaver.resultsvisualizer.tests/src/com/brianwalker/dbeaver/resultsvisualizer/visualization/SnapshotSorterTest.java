/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import static org.junit.Assert.assertEquals;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import com.brianwalker.dbeaver.resultsvisualizer.model.Nullability;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultRow;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import org.junit.Test;

public class SnapshotSorterTest {
    @Test public void appliesMultipleKeysInPriorityOrder() {
        ResultSetSnapshot source = new ResultSetSnapshot("sort", List.of(
                column(0, "country"), column(1, "state")), List.of(
                new ResultRow(0, List.of("US", "IL")),
                new ResultRow(1, List.of("CA", "ON")),
                new ResultRow(2, List.of("US", "NY"))), 3, false, Instant.EPOCH);

        ResultSetSnapshot sorted = SnapshotSorter.apply(source, List.of(
                new SortRule("country", SortRule.Direction.ASC),
                new SortRule("state", SortRule.Direction.DESC)));

        assertEquals(List.of("CA", "US", "US"),
                sorted.rows().stream().map(row -> row.values().get(0)).toList());
        assertEquals(List.of("ON", "NY", "IL"),
                sorted.rows().stream().map(row -> row.values().get(1)).toList());
        assertEquals("IL", source.rows().get(0).values().get(1));
    }

    private static ResultColumn column(int index, String name) {
        return new ResultColumn(index, name, name, Types.VARCHAR, "VARCHAR",
                NormalizedDataType.STRING, Nullability.NULLABLE);
    }
}
