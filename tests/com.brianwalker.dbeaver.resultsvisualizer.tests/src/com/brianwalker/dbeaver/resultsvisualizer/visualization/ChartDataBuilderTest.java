/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import com.brianwalker.dbeaver.resultsvisualizer.model.Nullability;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultRow;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class ChartDataBuilderTest {

    @Test public void combinesMultipleSelectedMeasuresIntoRendererSeries() {
        ResultSetSnapshot snapshot = salesSnapshot();
        VisualizationConfiguration configuration = new VisualizationConfiguration(ChartType.BAR,
                List.of(0), 1, List.of(), Aggregation.SUM, null).withValues(List.of(1, 2));

        ChartDataset dataset = ChartDataBuilder.build(snapshot, configuration);

        assertEquals(5, dataset.points().size());
        assertTrue(dataset.seriesNames().stream().anyMatch(name -> name.contains("revenue")));
        assertTrue(dataset.seriesNames().stream().anyMatch(name -> name.contains("quantity")));
    }

    @Test public void aggregatesEachSelectedMeasureIndependently() {
        ResultSetSnapshot snapshot = new ResultSetSnapshot("mixed-aggregations", List.of(
                column(0, "category", Types.VARCHAR, "VARCHAR", NormalizedDataType.STRING),
                column(1, "total", Types.DECIMAL, "DECIMAL", NormalizedDataType.DECIMAL),
                column(2, "invoice_id", Types.INTEGER, "INTEGER", NormalizedDataType.INTEGER)), List.of(
                new ResultRow(0, List.of("A", new BigDecimal("10"), 1)),
                new ResultRow(1, List.of("A", new BigDecimal("20"), 1)),
                new ResultRow(2, List.of("A", new BigDecimal("5"), 2))), 3, false, Instant.EPOCH);
        VisualizationConfiguration configuration = new VisualizationConfiguration(ChartType.BAR,
                List.of(0), 1, List.of(1, 2), List.of(), Aggregation.SUM, null,
                ChartDisplayOptions.DEFAULT, Map.of(2, Aggregation.COUNT_DISTINCT));

        ChartDataset dataset = ChartDataBuilder.build(snapshot, configuration);

        assertEquals(35.0, dataset.pointsForSeries("total").get(0).y(), 0.0001);
        assertEquals(2.0, dataset.pointsForSeries("invoice_id").get(0).y(), 0.0001);
        assertTrue(dataset.yAxisTitle().contains("total"));
        assertTrue(dataset.yAxisTitle().contains("invoice_id"));
    }

    @Test public void usesSecondBubbleMeasureForPointSize() {
        VisualizationConfiguration configuration = new VisualizationConfiguration(ChartType.BUBBLE,
                List.of(0), 1, List.of(), Aggregation.SUM, null).withValues(List.of(1, 2));

        ChartDataset dataset = ChartDataBuilder.build(salesSnapshot(), configuration);

        assertEquals(2, dataset.points().size());
        assertEquals(10.0, dataset.points().get(0).size(), 0.0001);
        assertTrue(dataset.yAxisTitle().contains("Size: quantity"));
    }

    @Test
    public void registersEverySupportedChartAndMatrixType() {
        ChartRendererRegistry registry = ChartRendererRegistry.defaults();
        for (ChartType type : ChartType.values()) {
            assertEquals(type, registry.renderer(type).type());
        }
    }

    @Test
    public void roundsAutomaticYAxisMaximumToReadableBounds() {
        assertEquals(100.0, ChartDrawing.niceCeiling(91), 0.0001);
        assertEquals(250.0, ChartDrawing.niceCeiling(204.96), 0.0001);
        assertEquals(1.0, ChartDrawing.niceCeiling(0.91), 0.0001);
    }

    @Test
    public void choosesCategoryAndFirstNumericValueByDefault() {
        ResultSetSnapshot snapshot = salesSnapshot();

        ChartConfiguration configuration = ChartDataBuilder.defaultConfiguration(snapshot);

        assertEquals(ChartType.BAR, configuration.chartType());
        assertEquals(0, configuration.xColumnIndex());
        assertEquals(1, configuration.yColumnIndex());
    }

    @Test
    public void buildsCategoryRevenueDatasetAndSkipsNullValues() {
        ResultSetSnapshot snapshot = salesSnapshot();

        ChartDataset dataset = ChartDataBuilder.build(snapshot,
                new ChartConfiguration(ChartType.BAR, 0, 1));

        assertEquals("category", dataset.xAxisTitle());
        assertEquals("revenue", dataset.yAxisTitle());
        assertEquals(2, dataset.points().size());
        assertEquals("Books", dataset.points().get(0).label());
        assertEquals(120.5, dataset.points().get(0).y(), 0.0001);
        assertFalse(dataset.hasNumericX());
    }

    @Test
    public void exposesNumericXForScatterData() {
        ResultSetSnapshot snapshot = salesSnapshot();

        ChartDataset dataset = ChartDataBuilder.build(snapshot,
                new ChartConfiguration(ChartType.SCATTER, 2, 1));

        assertTrue(dataset.hasNumericX());
        assertEquals(10.0, dataset.points().get(0).numericX(), 0.0001);
    }

    @Test
    public void aggregatesRepeatedCategoriesLocally() {
        ResultSetSnapshot snapshot = regionalSalesSnapshot();

        ChartDataset sum = ChartDataBuilder.build(snapshot,
                new VisualizationConfiguration(ChartType.BAR, 0, 2, -1, Aggregation.SUM));
        ChartDataset average = ChartDataBuilder.build(snapshot,
                new VisualizationConfiguration(ChartType.BAR, 0, 2, -1, Aggregation.AVG));
        ChartDataset minimum = ChartDataBuilder.build(snapshot,
                new VisualizationConfiguration(ChartType.BAR, 0, 2, -1, Aggregation.MIN));
        ChartDataset maximum = ChartDataBuilder.build(snapshot,
                new VisualizationConfiguration(ChartType.BAR, 0, 2, -1, Aggregation.MAX));
        ChartDataset count = ChartDataBuilder.build(snapshot,
                new VisualizationConfiguration(ChartType.BAR, 0, 2, -1, Aggregation.COUNT));

        assertEquals(30.0, sum.points().get(0).y(), 0.0001);
        assertEquals(15.0, average.points().get(0).y(), 0.0001);
        assertEquals(10.0, minimum.points().get(0).y(), 0.0001);
        assertEquals(20.0, maximum.points().get(0).y(), 0.0001);
        assertEquals(2.0, count.points().get(0).y(), 0.0001);
        assertEquals("SUM(revenue)", sum.yAxisTitle());
    }

    @Test
    public void buildsOneSeriesPerSeriesFieldValue() {
        ChartDataset dataset = ChartDataBuilder.build(regionalSalesSnapshot(),
                new VisualizationConfiguration(ChartType.LINE, 0, 2, 1, Aggregation.SUM));

        assertEquals(List.of("North", "South"), dataset.seriesNames());
        assertEquals(3, dataset.points().size());
        assertEquals(2, dataset.pointsForSeries("North").size());
        assertEquals(20.0, dataset.pointsForSeries("North").get(0).y(), 0.0001);
    }

    @Test
    public void countsDistinctValuesWithinEachGroup() {
        ResultSetSnapshot snapshot = new ResultSetSnapshot("distinct.sql", List.of(
                column(0, "category", Types.VARCHAR, "VARCHAR", NormalizedDataType.STRING),
                column(1, "customer", Types.VARCHAR, "VARCHAR", NormalizedDataType.STRING)), List.of(
                new ResultRow(0, List.of("Books", "A")),
                new ResultRow(1, List.of("Books", "A")),
                new ResultRow(2, List.of("Books", "B"))), 3, false, Instant.EPOCH);

        ChartDataset distinct = ChartDataBuilder.build(snapshot,
                new VisualizationConfiguration(ChartType.BAR, 0, 1, -1, Aggregation.COUNT_DISTINCT));

        assertEquals(2.0, distinct.points().get(0).y(), 0.0001);
        assertEquals("COUNT DISTINCT(customer)", distinct.yAxisTitle());
    }

    @Test
    public void countsDistinctNumericValuesAsEquivalentRegardlessOfJavaRepresentation() {
        ResultSetSnapshot snapshot = new ResultSetSnapshot("distinct-numeric.sql", List.of(
                column(0, "category", Types.VARCHAR, "VARCHAR", NormalizedDataType.STRING),
                column(1, "amount", Types.DECIMAL, "DECIMAL", NormalizedDataType.DECIMAL)), List.of(
                new ResultRow(0, List.of("Books", new BigDecimal("1"))),
                new ResultRow(1, List.of("Books", new BigDecimal("1.0"))),
                new ResultRow(2, List.of("Books", new BigDecimal("1.00"))),
                new ResultRow(3, List.of("Books", 1)),
                new ResultRow(4, List.of("Books", new BigDecimal("2")))),
                5, false, Instant.EPOCH);

        ChartDataset distinct = ChartDataBuilder.build(snapshot,
                new VisualizationConfiguration(ChartType.BAR, 0, 1, -1, Aggregation.COUNT_DISTINCT));

        assertEquals(2.0, distinct.points().get(0).y(), 0.0001);
    }

    @Test
    public void combinesMultipleMatrixRowAndColumnFields() {
        ResultSetSnapshot snapshot = new ResultSetSnapshot("matrix.sql", List.of(
                column(0, "country", Types.VARCHAR, "VARCHAR", NormalizedDataType.STRING),
                column(1, "state", Types.VARCHAR, "VARCHAR", NormalizedDataType.STRING),
                column(2, "year", Types.INTEGER, "INTEGER", NormalizedDataType.INTEGER),
                column(3, "quarter", Types.VARCHAR, "VARCHAR", NormalizedDataType.STRING),
                column(4, "revenue", Types.DECIMAL, "DECIMAL", NormalizedDataType.DECIMAL)), List.of(
                new ResultRow(0, List.of("US", "IL", 2026, "Q1", new BigDecimal("10"))),
                new ResultRow(1, List.of("US", "IL", 2026, "Q2", new BigDecimal("20")))),
                2, false, Instant.EPOCH);

        ChartDataset matrix = ChartDataBuilder.build(snapshot, new VisualizationConfiguration(
                ChartType.MATRIX, List.of(0, 1), 4, List.of(2, 3), Aggregation.SUM, null));

        assertEquals(List.of("US › IL"), matrix.categories());
        assertEquals(List.of("2026 › Q1", "2026 › Q2"), matrix.seriesNames());
        assertEquals("country / state", matrix.xAxisTitle());
        assertEquals(List.of("country", "state"), matrix.rowLevelNames());
        assertEquals(List.of("year", "quarter"), matrix.columnLevelNames());
        assertEquals(List.of(List.of("US", "IL")), matrix.rowTuples());
        assertEquals(List.of(List.of("2026", "Q1"), List.of("2026", "Q2")),
                matrix.columnTuples());
        assertEquals(MatrixDisplayOptions.DEFAULT, matrix.matrixOptions());
        ChartDataset withSubtotals = matrix.withMatrixOptions(
                new MatrixDisplayOptions(false, false, true));
        assertEquals(2, MatrixChartRenderer.visualRowCount(withSubtotals));
    }

    @Test
    public void buildsMatrixWithMultipleValueMeasures() {
        VisualizationConfiguration configuration = new VisualizationConfiguration(
                ChartType.MATRIX, List.of(0), 1, List.of(), Aggregation.SUM, null);

        ChartDataset matrix = ChartDataBuilder.buildMatrixValues(
                salesSnapshot(), configuration, List.of(1, 2));

        assertEquals(List.of("Values"), matrix.columnLevelNames());
        assertEquals(List.of(List.of("revenue"), List.of("quantity")), matrix.columnTuples());
        assertEquals(List.of(List.of("Books"), List.of("Games"), List.of("Music")), matrix.rowTuples());
        assertEquals(5, matrix.points().size());
        assertEquals("revenue / quantity", matrix.yAxisTitle());
    }

    @Test
    public void emptyConfigurationDoesNotBuildChartData() {
        VisualizationConfiguration configuration = VisualizationConfiguration.empty(ChartType.BAR);

        assertFalse(configuration.isComplete());
        assertTrue(ChartDataBuilder.build(salesSnapshot(), configuration).points().isEmpty());
    }

    private static ResultSetSnapshot salesSnapshot() {
        List<ResultColumn> columns = List.of(
                column(0, "category", Types.VARCHAR, "VARCHAR", NormalizedDataType.STRING),
                column(1, "revenue", Types.DECIMAL, "DECIMAL", NormalizedDataType.DECIMAL),
                column(2, "quantity", Types.INTEGER, "INTEGER", NormalizedDataType.INTEGER));
        List<ResultRow> rows = List.of(
                new ResultRow(0, List.of("Books", new BigDecimal("120.50"), 10)),
                new ResultRow(1, List.of("Games", new BigDecimal("80.00"), 4)),
                new ResultRow(2, Arrays.asList("Music", null, 7)));
        return new ResultSetSnapshot("sales.sql", columns, rows, 3, false, Instant.EPOCH);
    }

    private static ResultSetSnapshot regionalSalesSnapshot() {
        List<ResultColumn> columns = List.of(
                column(0, "category", Types.VARCHAR, "VARCHAR", NormalizedDataType.STRING),
                column(1, "region", Types.VARCHAR, "VARCHAR", NormalizedDataType.STRING),
                column(2, "revenue", Types.DECIMAL, "DECIMAL", NormalizedDataType.DECIMAL));
        List<ResultRow> rows = List.of(
                new ResultRow(0, List.of("Books", "North", new BigDecimal("20"))),
                new ResultRow(1, List.of("Books", "South", new BigDecimal("10"))),
                new ResultRow(2, List.of("Games", "North", new BigDecimal("5"))));
        return new ResultSetSnapshot("regional.sql", columns, rows, 3, false, Instant.EPOCH);
    }

    private static ResultColumn column(int index, String name, int typeId,
            String typeName, NormalizedDataType normalizedType) {
        return new ResultColumn(index, name, name, typeId, typeName,
                normalizedType, Nullability.NULLABLE);
    }
}
