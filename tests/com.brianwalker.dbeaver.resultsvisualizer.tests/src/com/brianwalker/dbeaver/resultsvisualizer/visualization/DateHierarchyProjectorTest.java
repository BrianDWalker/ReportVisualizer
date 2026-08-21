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
import org.junit.Test;

public class DateHierarchyProjectorTest {
    @Test public void derivesDateLabelsWithoutChangingTheSourceSnapshot() {
        ResultSetSnapshot source = new ResultSetSnapshot("invoices", List.of(new ResultColumn(0,
                "invoice_date", "Invoice Date", Types.DATE, "DATE", NormalizedDataType.DATE,
                Nullability.NOT_NULL)), List.of(new ResultRow(1, List.of(LocalDate.of(2026, 8, 20)))),
                1, false, Instant.EPOCH);

        ResultSetSnapshot projected = DateHierarchyProjector.apply(source,
                List.of(new DateHierarchySelection(0, DateHierarchyLevel.QUARTER)));

        assertEquals("Invoice Date [Quarter]", projected.columns().get(0).displayName());
        assertEquals("2026 Q3", projected.rows().get(0).values().get(0));
        assertEquals(LocalDate.of(2026, 8, 20), source.rows().get(0).values().get(0));
    }

    @Test public void projectsOriginalYearQuarterMonthAndDay() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        assertEquals("2026", DateHierarchyProjector.label(date, DateHierarchyLevel.YEAR));
        assertEquals("2026 Q3", DateHierarchyProjector.label(date, DateHierarchyLevel.QUARTER));
        assertEquals("2026-08", DateHierarchyProjector.label(date, DateHierarchyLevel.MONTH));
        assertEquals("2026-08-20", DateHierarchyProjector.label(date, DateHierarchyLevel.DAY));

        ResultSetSnapshot source = new ResultSetSnapshot("invoices", List.of(new ResultColumn(0,
                "invoice_date", "Invoice Date", Types.DATE, "DATE", NormalizedDataType.DATE,
                Nullability.NOT_NULL)), List.of(new ResultRow(1, List.of(date))), 1, false, Instant.EPOCH);
        assertEquals(source, DateHierarchyProjector.apply(source, List.of()));
    }

    @Test public void projectsJdbcTimestampStringsReturnedForDateColumns() {
        String jdbcTimestamp = "2026-08-20 14:30:00.000";

        assertEquals("2026", DateHierarchyProjector.label(jdbcTimestamp, DateHierarchyLevel.YEAR));
        assertEquals("2026 Q3", DateHierarchyProjector.label(jdbcTimestamp, DateHierarchyLevel.QUARTER));
        assertEquals("2026-08", DateHierarchyProjector.label(jdbcTimestamp, DateHierarchyLevel.MONTH));
        assertEquals("2026-08-20", DateHierarchyProjector.label(jdbcTimestamp, DateHierarchyLevel.DAY));
    }
}
