/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import com.brianwalker.dbeaver.resultsvisualizer.model.Nullability;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.Aggregation;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartDataBuilder;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartType;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SortRule;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationConfiguration;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class AggregateQueryBuilderTest {
    @Test public void usesDirectRewriteForSimpleSourceQuery() {
        String sql = AggregateQueryBuilder.build("SELECT order_dt, revenue, region FROM sales;",
                snapshot(), new VisualizationConfiguration(ChartType.BAR, 0, 1, 2, Aggregation.SUM),
                List.of(new CustomSqlDimension("month", "DATE_TRUNC('month', order_dt)")),
                List.of(SlicerDefinition.fromStrings("region", Set.of("East", "O'Brien"))));
        assertTrue(sql.contains("DATE_TRUNC('month', order_dt) AS \"month\""));
        assertTrue(sql.contains("SUM(\"revenue\")"));
        assertTrue(sql.contains("\"region\" IN ("));
        assertTrue(sql.contains("'East'"));
        assertTrue(sql.contains("'O''Brien'"));
        assertTrue(sql.contains("FROM sales"));
        assertTrue(!sql.contains("rv_source"));
    }

    @Test public void usesConfiguredQuoteCharacterWhenAvailable() {
        DBeaverSqlDialectService.installQuoteString("`");
        try {
            assertTrue(DBeaverSqlDialectService.quoteIdentifier("region").equals("`region`"));
            assertTrue(DBeaverSqlDialectService.quoteIdentifier("O`Brien").equals("`O``Brien`"));
        } finally {
            DBeaverSqlDialectService.clearQuoteString();
        }
    }

    @Test public void fallsBackToDerivedQueryForComplexSourceSql() {
        String sql = AggregateQueryBuilder.buildQuery(
                "SELECT order_dt, revenue FROM sales UNION SELECT order_dt, revenue FROM archived_sales",
                snapshot(), new VisualizationConfiguration(ChartType.BAR, 0, 1, 2, Aggregation.SUM),
                List.of(AggregateQueryBuilder.resultDimension(snapshot(), 0)),
                List.of(AggregateQueryBuilder.resultDimension(snapshot(), 2)), List.of()).sql();
        assertTrue(sql.contains("FROM (\nSELECT order_dt, revenue FROM sales UNION SELECT order_dt, revenue FROM archived_sales\n) rv_source"));
    }

    @Test public void directRewriteCorrectlySkipsParenthesizedSubqueriesInTheSelectList() {
        // A naive "first FROM after SELECT" split would incorrectly treat "archive" as the
        // source table. Depth-aware scanning must skip the FROM nested inside the scalar
        // subquery and find the true top-level source table, "sales".
        AggregateQuery query = AggregateQueryBuilder.buildQuery(
                "SELECT (SELECT MAX(order_dt) FROM archive) AS latest, revenue FROM sales",
                snapshot(), new VisualizationConfiguration(ChartType.BAR, 0, 1, 2, Aggregation.SUM),
                List.of(AggregateQueryBuilder.resultDimension(snapshot(), 0)), List.of(), List.of());
        assertTrue(query.strategy() == DBeaverSqlDialectService.QueryStrategy.DIRECT_REWRITE);
        assertTrue(query.sql().contains("FROM sales"));
        assertTrue(!query.sql().contains("rv_source"));
    }

    @Test public void buildsChartDatasetsWithoutExceptionsAtLargeRowCounts() {
        int[] sizes = {10_000, 50_000, 100_000};
        for (int size : sizes) {
            ResultSetSnapshot large = largeSnapshot(size);
            VisualizationConfiguration configuration = new VisualizationConfiguration(
                    ChartType.BAR, 0, 1, VisualizationConfiguration.UNASSIGNED, Aggregation.SUM);
            assertTrue(ChartDataBuilder.build(large, configuration).points().size() > 0);
        }
    }

    @Test public void fallsBackWhenFromClauseItselfIsASubqueryOrJoinExpression() {
        AggregateQuery query = AggregateQueryBuilder.buildQuery(
                "SELECT a.order_dt, a.revenue FROM (SELECT * FROM sales) a",
                snapshot(), new VisualizationConfiguration(ChartType.BAR, 0, 1, 2, Aggregation.SUM),
                List.of(AggregateQueryBuilder.resultDimension(snapshot(), 0)), List.of(), List.of());
        assertTrue(query.strategy() == DBeaverSqlDialectService.QueryStrategy.DERIVED_TABLE_FALLBACK);
        assertTrue(query.sql().contains("rv_source"));
    }

    @Test public void rejectsSourceSqlWithAStackedSecondStatement() {
        try {
            AggregateQueryBuilder.buildQuery(
                    "SELECT order_dt, revenue FROM sales; DROP TABLE sales;",
                    snapshot(), new VisualizationConfiguration(ChartType.BAR, 0, 1, 2, Aggregation.SUM),
                    List.of(AggregateQueryBuilder.resultDimension(snapshot(), 0)), List.of(), List.of());
            fail("Expected multi-statement SQL to be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("exactly one SQL statement"));
        }
    }

    @Test public void safelyRemovesASourceSqlComment() {
        AggregateQuery query = AggregateQueryBuilder.buildQuery(
                "SELECT order_dt, revenue FROM sales -- trailing comment\n",
                snapshot(), new VisualizationConfiguration(ChartType.BAR, 0, 1, 2, Aggregation.SUM),
                List.of(AggregateQueryBuilder.resultDimension(snapshot(), 0)), List.of(), List.of());
        assertTrue(query.sql().contains("FROM sales"));
        assertTrue(!query.sql().contains("trailing comment"));
    }

    @Test public void wrapsSQLiteGroupingWithCommentsFunctionsAndATrailingSemicolon() {
        String source = """
                SELECT strftime('%Y-%m', order_dt) AS order_dt, region, SUM(revenue) AS revenue
                FROM sales
                -- WHERE revenue > 0
                /* SQLite aggregate source */
                GROUP BY strftime('%Y-%m', order_dt), region;
                """;
        AggregateQuery query = AggregateQueryBuilder.buildQuery(source, snapshot(),
                new VisualizationConfiguration(ChartType.BAR, 0, 1, 2, Aggregation.SUM),
                List.of(AggregateQueryBuilder.resultDimension(snapshot(), 0)), List.of(), List.of());

        assertTrue(query.strategy() == DBeaverSqlDialectService.QueryStrategy.DERIVED_TABLE_FALLBACK);
        assertTrue(query.sql().contains("strftime('%Y-%m', order_dt)"));
        assertTrue(query.sql().contains("GROUP BY strftime('%Y-%m', order_dt), region"));
        assertTrue(!query.sql().contains("WHERE revenue > 0"));
        assertTrue(!query.sql().contains(";\n) rv_source"));
    }

    @Test public void preservesCommentMarkersAndSemicolonsInsideQuotedStrings() {
        String normalized = DBeaverSqlDialectService.normalizedSingleStatement(
                "SELECT '-- not a comment', '/* neither */', ';' FROM sales;");
        assertTrue(normalized.contains("'-- not a comment'"));
        assertTrue(normalized.contains("'/* neither */'"));
        assertTrue(normalized.contains("';'"));
        assertTrue(!normalized.endsWith(";"));
    }

    @Test public void directRewriteMarksOptimizedStrategyOnAggregateQuery() {
        AggregateQuery query = AggregateQueryBuilder.buildQuery(
                "SELECT order_dt, revenue, region FROM sales",
                snapshot(), new VisualizationConfiguration(ChartType.BAR, 0, 1, 2, Aggregation.SUM),
                List.of(AggregateQueryBuilder.resultDimension(snapshot(), 0)), List.of(), List.of());
        assertTrue(query.strategy() == DBeaverSqlDialectService.QueryStrategy.DIRECT_REWRITE);
    }

    @Test public void rejectsMultiStatementCustomExpressions() {
        try {
            new CustomSqlDimension("bad", "region; DELETE FROM sales");
            fail("Expected rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("one SQL expression"));
        }
    }

    @Test public void buildsMultipleDimensionsWithCountDistinct() {
        VisualizationConfiguration configuration = new VisualizationConfiguration(
                ChartType.MATRIX, List.of(2, 0), 1, List.of(0), Aggregation.COUNT_DISTINCT, null);
        AggregateQuery query = AggregateQueryBuilder.buildQuery(
                "SELECT order_dt, revenue, region FROM sales", snapshot(), configuration,
                List.of(AggregateQueryBuilder.resultDimension(snapshot(), 2),
                        AggregateQueryBuilder.customDimension(new CustomSqlDimension(
                                "month", "DATE_TRUNC('month', order_dt)"))),
                List.of(AggregateQueryBuilder.resultDimension(snapshot(), 0)), List.of());

        assertTrue(query.sql().contains("COUNT(DISTINCT \"revenue\")"));
        assertTrue(query.sql().contains("\"region\" AS \"region\""));
        assertTrue(query.sql().contains("DATE_TRUNC('month', order_dt) AS \"month\""));
        assertTrue(query.sql().contains("GROUP BY \"region\", DATE_TRUNC('month', order_dt), \"order_dt\""));
        assertTrue(query.rowAliases().equals(List.of("region", "month")));
    }

    @Test public void emitsOrderedMultiFieldSortForAggregateResults() {
        VisualizationConfiguration configuration = new VisualizationConfiguration(
                ChartType.MATRIX, List.of(2), 1, List.of(0), Aggregation.SUM, null);
        AggregateQuery query = AggregateQueryBuilder.buildQuery(
                "SELECT order_dt, revenue, region FROM sales", snapshot(), configuration,
                List.of(AggregateQueryBuilder.resultDimension(snapshot(), 2)),
                List.of(AggregateQueryBuilder.resultDimension(snapshot(), 0)), List.of(),
                List.of(new SortRule("region", SortRule.Direction.DESC),
                        new SortRule("order_dt", SortRule.Direction.ASC)));

        assertTrue(query.sql().contains("ORDER BY \"region\" DESC, \"order_dt\" ASC"));
    }

    @Test public void aggregatesCustomSqlValueExpressionAsAFirstClassMeasure() {
        VisualizationConfiguration configuration = new VisualizationConfiguration(
                ChartType.BAR, List.of(2), -1, List.of(), Aggregation.SUM, null);
        CustomSqlDimension value = new CustomSqlDimension(
                "weighted_revenue", "revenue * 1.15");

        AggregateQuery query = AggregateQueryBuilder.buildQuery(
                "SELECT order_dt, revenue, region FROM sales", configuration,
                List.of(AggregateQueryBuilder.resultDimension(snapshot(), 2)), List.of(),
                List.of(), List.of(), AggregateQueryBuilder.customMeasure(value));

        assertTrue(query.sql().contains("SUM(revenue * 1.15) AS \"sum_weighted_revenue\""));
        assertTrue(query.valueAlias().equals("sum_weighted_revenue"));
    }

    @Test public void buildsMultipleNamedAggregationsInOneSourceQuery() {
        QueryMeasure revenue = AggregateQueryBuilder.resultMeasure(snapshot(), 1);
        QueryMeasure region = AggregateQueryBuilder.resultMeasure(snapshot(), 2);
        AggregateQuery query = AggregateQueryBuilder.buildQuery(
                "SELECT order_dt, revenue, region FROM sales",
                List.of(AggregateQueryBuilder.resultDimension(snapshot(), 0)), List.of(), List.of(),
                List.of(new SortRule("unique_regions", SortRule.Direction.DESC)),
                List.of(new QueryAggregation("total_revenue", revenue, Aggregation.SUM),
                        new QueryAggregation("unique_regions", region, Aggregation.COUNT_DISTINCT)));

        assertTrue(query.sql().contains("SUM(\"revenue\") AS \"total_revenue\""));
        assertTrue(query.sql().contains("COUNT(DISTINCT \"region\") AS \"unique_regions\""));
        assertTrue(query.sql().contains("ORDER BY \"unique_regions\" DESC"));
        assertTrue(query.valueAlias().equals("total_revenue"));
    }

    private static ResultSetSnapshot snapshot() {
        return new ResultSetSnapshot("sales", List.of(
                column(0, "order_dt", NormalizedDataType.DATETIME),
                column(1, "revenue", NormalizedDataType.DECIMAL),
                column(2, "region", NormalizedDataType.STRING)), List.of(), 0, false, Instant.now());
    }

    private static ResultSetSnapshot largeSnapshot(int rowCount) {
        List<ResultColumn> columns = List.of(
                column(0, "region", NormalizedDataType.STRING),
                column(1, "revenue", NormalizedDataType.DECIMAL));
        List<com.brianwalker.dbeaver.resultsvisualizer.model.ResultRow> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            String region = switch (rowIndex % 4) {
                case 0 -> "North";
                case 1 -> "South";
                case 2 -> "East";
                default -> "West";
            };
            rows.add(new com.brianwalker.dbeaver.resultsvisualizer.model.ResultRow(rowIndex, List.of(region, (double) (rowIndex % 1000 + 1))));
        }
        return new ResultSetSnapshot("large_sales", columns, rows, rowCount, false, Instant.now());
    }

    private static ResultColumn column(int index, String name, NormalizedDataType type) {
        return new ResultColumn(index, name, name, Types.OTHER, type.name(), type, Nullability.NULLABLE);
    }
}
