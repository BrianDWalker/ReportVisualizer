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
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class SnapshotSlicerTest {
    @Test public void combinesDistinctValueSlicersWithoutMutatingSource() {
        ResultSetSnapshot source = new ResultSetSnapshot("test", List.of(
                new ResultColumn(0, "region", "region", Types.VARCHAR, "VARCHAR",
                        NormalizedDataType.STRING, Nullability.NULLABLE),
                new ResultColumn(1, "year", "year", Types.INTEGER, "INTEGER",
                        NormalizedDataType.INTEGER, Nullability.NULLABLE)),
                List.of(new ResultRow(1, List.of("East", 2025)),
                        new ResultRow(2, List.of("East", 2026)),
                        new ResultRow(3, List.of("West", 2026))), 3, false, Instant.now());

        ResultSetSnapshot filtered = SnapshotSlicer.apply(source, List.of(
                SlicerDefinition.fromStrings("region", Set.of("East")),
                SlicerDefinition.fromStrings("year", Set.of("2026"))));

        assertEquals(1, filtered.rows().size());
        assertEquals(3, source.rows().size());
    }

    @Test public void comparesNumericAndDatePredicatesUsingTheirTypedValues() {
        ResultSetSnapshot source = new ResultSetSnapshot("test", List.of(
                new ResultColumn(0, "amount", "amount", Types.DECIMAL, "DECIMAL",
                        NormalizedDataType.NUMBER, Nullability.NULLABLE),
                new ResultColumn(1, "invoice_date", "invoice_date", Types.DATE, "DATE",
                        NormalizedDataType.DATE, Nullability.NULLABLE)),
                List.of(new ResultRow(1, List.of("2.5", LocalDate.of(2026, 1, 10))),
                        new ResultRow(2, List.of("10.0", LocalDate.of(2026, 2, 15))),
                        new ResultRow(3, List.of("20.0", LocalDate.of(2026, 3, 20))),
                        new ResultRow(4, java.util.Arrays.asList(null, null))), 4, false, Instant.now());

        ResultSetSnapshot filtered = SnapshotSlicer.apply(source, List.of(
                SlicerDefinition.predicate("amount", SlicerOperator.GREATER_THAN, "9", ""),
                SlicerDefinition.predicate("invoice_date", SlicerOperator.BETWEEN,
                        "2026-02-01", "2026-03-01")));

        assertEquals(1, filtered.rows().size());
        assertEquals("10.0", filtered.rows().get(0).values().get(0));
        assertEquals(4, source.rows().size());
    }

    @Test public void supportsNullAndRelativeDatePredicates() {
        LocalDate today = LocalDate.now();
        ResultSetSnapshot source = new ResultSetSnapshot("test", List.of(new ResultColumn(0,
                "invoice_date", "invoice_date", Types.DATE, "DATE", NormalizedDataType.DATE,
                Nullability.NULLABLE)), List.of(new ResultRow(1, List.of(today)),
                        new ResultRow(2, List.of(today.minusDays(40))), new ResultRow(3, java.util.Collections.singletonList(null))),
                3, false, Instant.now());

        assertEquals(1, SnapshotSlicer.apply(source, List.of(SlicerDefinition.predicate(
                "invoice_date", SlicerOperator.LAST_N_DAYS, "7", ""))).rows().size());
        assertEquals(1, SnapshotSlicer.apply(source, List.of(SlicerDefinition.predicate(
                "invoice_date", SlicerOperator.IS_NULL, "", ""))).rows().size());
    }
}
