/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import com.brianwalker.dbeaver.resultsvisualizer.model.Nullability;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import com.brianwalker.dbeaver.resultsvisualizer.services.AggregateQuery;
import com.brianwalker.dbeaver.resultsvisualizer.services.DBeaverSqlDialectService;
import com.brianwalker.dbeaver.resultsvisualizer.services.QueryAggregation;
import com.brianwalker.dbeaver.resultsvisualizer.services.QueryMeasure;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import org.junit.Test;

public class AggregateResultBindingTest {
    @Test public void rebindsReturnedAliasesForSourceQueryAggregate() {
        ResultSetSnapshot aggregate = new ResultSetSnapshot("Aggregate", List.of(
                column(0, "data_dt", Types.DATE, NormalizedDataType.DATE),
                column(1, "sum_cnt", Types.NUMERIC, NormalizedDataType.NUMBER)), List.of(), 0, false, Instant.EPOCH);
        AggregateQuery query = new AggregateQuery("SELECT ...", List.of("data_dt"), List.of(), "sum_cnt");
        VisualizationConfiguration original = new VisualizationConfiguration(ChartType.BAR, List.of(0), 3,
                List.of(1), Aggregation.COUNT, 250.0);

        AggregateResultBinding binding = AggregateResultBinding.bind(original, query, aggregate).orElseThrow();

        assertEquals(List.of(0), binding.configuration().xColumnIndexes());
        assertEquals(1, binding.configuration().valueColumnIndex());
        assertEquals(List.of(), binding.configuration().seriesColumnIndexes());
        assertEquals(Aggregation.SUM, binding.configuration().aggregation());
        assertEquals(List.of(0), binding.rows());
        assertEquals(List.of(1), binding.values());
        assertTrue(AggregateResultBinding.bind(original,
                new AggregateQuery("SELECT ...", List.of("data_dt"), List.of(), "missing"), aggregate).isEmpty());
    }

    @Test public void preservesMathematicallySafeLocalAggregateSemantics() {
        assertEquals(Aggregation.SUM, query(Aggregation.COUNT).localAggregationFor("metric"));
        assertEquals(Aggregation.SUM, query(Aggregation.SUM).localAggregationFor("metric"));
        assertEquals(Aggregation.MIN, query(Aggregation.MIN).localAggregationFor("metric"));
        assertEquals(Aggregation.MAX, query(Aggregation.MAX).localAggregationFor("metric"));
        assertEquals(Aggregation.AVG, query(Aggregation.AVG).localAggregationFor("metric"));
        assertTrue(query(Aggregation.AVG).requiresExactDimensions("metric"));
        assertTrue(query(Aggregation.COUNT_DISTINCT).requiresExactDimensions("metric"));
    }

    private static AggregateQuery query(Aggregation aggregation) {
        QueryAggregation output = new QueryAggregation(
                "metric", new QueryMeasure("value", "\"value\""), aggregation);
        return new AggregateQuery("SELECT ...", List.of("data_dt"), List.of(), "metric",
                DBeaverSqlDialectService.QueryStrategy.DERIVED_TABLE_FALLBACK, List.of(output));
    }

    private static ResultColumn column(int index, String name, int jdbcType, NormalizedDataType type) {
        return new ResultColumn(index, name, name, jdbcType, "", type, Nullability.NULLABLE);
    }
}
