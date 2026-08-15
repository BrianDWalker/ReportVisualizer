/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.calculatedfields;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import com.brianwalker.dbeaver.resultsvisualizer.model.Nullability;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultRow;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import org.junit.Test;

public class CalculatedFieldServiceTest {
    private final CalculatedFieldService service = new CalculatedFieldService();

    @Test
    public void projectsCalculatedFieldsBesideOriginalFields() {
        CalculatedFieldProjection projection = service.project(snapshot(100, 60),
                List.of(new CalculatedFieldDefinition("Profit", "[revenue] - [cost]")));

        assertTrue(projection.errors().isEmpty());
        assertEquals(4, projection.snapshot().columns().size());
        assertEquals("Profit (Calculated)", projection.snapshot().columns().get(3).displayName());
        assertEquals(NormalizedDataType.NUMBER,
                projection.snapshot().columns().get(3).normalizedType());
        assertEquals(40.0, (Double) projection.snapshot().rows().get(0).values().get(3), 0.0001);
    }

    @Test
    public void calculatedFieldsCanReferenceEarlierCalculatedFields() {
        CalculatedFieldProjection projection = service.project(snapshot(100, 60), List.of(
                new CalculatedFieldDefinition("Profit", "[revenue] - [cost]"),
                new CalculatedFieldDefinition("Profit Percent", "[Profit (Calculated)] / [revenue] * 100")));

        assertTrue(projection.errors().isEmpty());
        assertEquals(40.0, (Double) projection.snapshot().rows().get(0).values().get(4), 0.0001);
    }

    @Test
    public void definitionsRecalculateAgainstRefreshedRows() {
        List<CalculatedFieldDefinition> definitions =
                List.of(new CalculatedFieldDefinition("Profit", "[revenue] - [cost]"));

        assertEquals(40.0, (Double) service.project(snapshot(100, 60), definitions)
                .snapshot().rows().get(0).values().get(3), 0.0001);
        assertEquals(25.0, (Double) service.project(snapshot(80, 55), definitions)
                .snapshot().rows().get(0).values().get(3), 0.0001);
    }

    @Test
    public void invalidDefinitionIsIsolatedFromOtherFields() {
        CalculatedFieldProjection projection = service.project(snapshot(100, 60),
                List.of(new CalculatedFieldDefinition("Bad", "[missing] + 1")));

        assertEquals(3, projection.snapshot().columns().size());
        assertEquals(1, projection.errors().size());
        assertTrue(projection.errors().get(0).contains("Unknown field"));
    }

    private static ResultSetSnapshot snapshot(double revenue, double cost) {
        List<ResultColumn> columns = List.of(
                column(0, "category", Types.VARCHAR, NormalizedDataType.STRING),
                column(1, "revenue", Types.DECIMAL, NormalizedDataType.DECIMAL),
                column(2, "cost", Types.DECIMAL, NormalizedDataType.DECIMAL));
        return new ResultSetSnapshot("sales.sql", columns,
                List.of(new ResultRow(0, List.of("Books", revenue, cost))),
                1, false, Instant.EPOCH);
    }

    private static ResultColumn column(int index, String name, int jdbcType,
            NormalizedDataType type) {
        return new ResultColumn(index, name, name, jdbcType, type.name(),
                type, Nullability.NULLABLE);
    }
}
