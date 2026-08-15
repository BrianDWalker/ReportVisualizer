/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ResultSetSnapshotTest {

    @Test
    public void defensivelyCopiesColumnsRowsAndValues() {
        List<ResultColumn> columns = new ArrayList<>();
        columns.add(column());
        List<Object> values = new ArrayList<>(List.of("alpha"));
        List<ResultRow> rows = new ArrayList<>(List.of(new ResultRow(0, values)));

        ResultSetSnapshot snapshot = new ResultSetSnapshot(
                "query.sql", columns, rows, 1, false, Instant.EPOCH);
        columns.clear();
        rows.clear();
        values.set(0, "changed");

        assertEquals(1, snapshot.columns().size());
        assertEquals(1, snapshot.rows().size());
        assertEquals("alpha", snapshot.rows().get(0).values().get(0));
        assertThrows(UnsupportedOperationException.class, snapshot.columns()::clear);
        assertThrows(UnsupportedOperationException.class, snapshot.rows()::clear);
    }

    @Test
    public void rejectsAnAvailableCountSmallerThanCopiedRows() {
        assertThrows(IllegalArgumentException.class, () -> new ResultSetSnapshot(
                "", List.of(column()), List.of(new ResultRow(0, List.of("alpha"))),
                0, false, Instant.EPOCH));
    }

    @Test
    public void preservesSqlNullValuesInRows() {
        ResultRow row = new ResultRow(0, java.util.Arrays.asList("alpha", null));

        assertEquals(2, row.values().size());
        assertEquals(null, row.values().get(1));
        assertThrows(UnsupportedOperationException.class,
                () -> row.values().set(0, "changed"));
    }

    @Test
    public void defaultsConfiguredRowLimitToZeroForTheBackwardCompatibleConstructor() {
        ResultSetSnapshot snapshot = new ResultSetSnapshot(
                "query.sql", List.of(column()), List.of(), 0, false, Instant.EPOCH);

        assertEquals(0, snapshot.configuredRowLimit());
    }

    @Test
    public void sameDataIgnoresCapturedAtAndConfiguredRowLimit() {
        List<ResultColumn> columns = List.of(column());
        List<ResultRow> rows = List.of(new ResultRow(0, List.of("alpha")));
        ResultSetSnapshot first = new ResultSetSnapshot(
                "query.sql", columns, rows, 1, false, Instant.EPOCH, 100);
        ResultSetSnapshot second = new ResultSetSnapshot(
                "query.sql", columns, rows, 1, false, Instant.now(), 200);

        assertEquals(true, first.sameData(second));
        assertEquals(true, second.sameData(first));
    }

    @Test
    public void sameDataDetectsGenuineDifferencesInRowsColumnsOrTruncation() {
        ResultSetSnapshot base = new ResultSetSnapshot("query.sql", List.of(column()),
                List.of(new ResultRow(0, List.of("alpha"))), 1, false, Instant.EPOCH);
        ResultSetSnapshot differentRows = new ResultSetSnapshot("query.sql", List.of(column()),
                List.of(new ResultRow(0, List.of("beta"))), 1, false, Instant.EPOCH);
        ResultSetSnapshot differentTruncation = new ResultSetSnapshot("query.sql", List.of(column()),
                List.of(new ResultRow(0, List.of("alpha"))), 1, true, Instant.EPOCH);
        ResultSetSnapshot differentSource = new ResultSetSnapshot("other.sql", List.of(column()),
                List.of(new ResultRow(0, List.of("alpha"))), 1, false, Instant.EPOCH);

        assertEquals(false, base.sameData(differentRows));
        assertEquals(false, base.sameData(differentTruncation));
        assertEquals(false, base.sameData(differentSource));
        assertEquals(false, base.sameData(null));
    }

    private static ResultColumn column() {
        return new ResultColumn(0, "name", "Name", java.sql.Types.VARCHAR,
                "VARCHAR", NormalizedDataType.STRING, Nullability.NULLABLE);
    }
}
