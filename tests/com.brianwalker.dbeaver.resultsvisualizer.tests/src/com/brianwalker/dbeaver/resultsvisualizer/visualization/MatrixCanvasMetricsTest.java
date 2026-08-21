/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * Verifies the full (unscrolled) matrix content-size formulas used both to size scrollbars and
 * to size the image captured for export, ensuring Matrix/Heatmap export always covers the whole
 * dataset rather than only the currently visible, scrolled viewport.
 */
public class MatrixCanvasMetricsTest {

    @Test
    public void identifiesMatrixAndHeatmapAsMatrixLike() {
        assertTrue(MatrixCanvasMetrics.isMatrixLike(ChartType.MATRIX));
        assertTrue(MatrixCanvasMetrics.isMatrixLike(ChartType.HEATMAP));
        assertFalse(MatrixCanvasMetrics.isMatrixLike(ChartType.BAR));
        assertFalse(MatrixCanvasMetrics.isMatrixLike(ChartType.LINE));
    }

    @Test
    public void widthAndHeightGrowWithMoreRowsAndColumns() {
        ChartDataset small = matrixDataset(2, 2);
        ChartDataset large = matrixDataset(20, 20);

        assertTrue("A larger matrix must report a larger full width than a small one",
                MatrixCanvasMetrics.width(large) > MatrixCanvasMetrics.width(small));
        assertTrue("A larger matrix must report a larger full height than a small one",
                MatrixCanvasMetrics.height(large) > MatrixCanvasMetrics.height(small));
    }

    @Test
    public void fullSizeCanExceedTypicalViewportForLargeMatrices() {
        ChartDataset large = matrixDataset(40, 40);
        int typicalViewportWidth = 800;
        int typicalViewportHeight = 600;

        assertTrue("A 40x40 matrix's full content should exceed a typical on-screen viewport width",
                MatrixCanvasMetrics.width(large) > typicalViewportWidth);
        assertTrue("A 40x40 matrix's full content should exceed a typical on-screen viewport height",
                MatrixCanvasMetrics.height(large) > typicalViewportHeight);
    }

    @Test
    public void collapsedHierarchyAndTopNReduceRenderedRows() {
        List<ChartPoint> points = List.of(
                new ChartPoint("A › One", null, 10, "Value", List.of("A", "One"), List.of("Value")),
                new ChartPoint("A › Two", null, 8, "Value", List.of("A", "Two"), List.of("Value")),
                new ChartPoint("B › One", null, 2, "Value", List.of("B", "One"), List.of("Value")));
        MatrixDisplayOptions collapsed = new MatrixDisplayOptions(true, true, false, true,
                MatrixDisplayOptions.Layout.STEPPED, 2, false, true,
                MatrixDisplayOptions.ConditionalFormat.DATA_BARS, true, 2, 120, java.util.Set.of(),
                java.util.Set.of(MatrixChartRenderer.path(List.of("A"))));
        ChartDataset data = new ChartDataset("Rows", "Value", points, null, List.of("Group", "Item"),
                List.of("Measure"), collapsed);

        assertEquals(2, MatrixChartRenderer.visualRowCount(data));
        assertEquals(120, data.matrixOptions().columnWidth());
    }

    private static ChartDataset matrixDataset(int rows, int cols) {
        List<ChartPoint> points = new java.util.ArrayList<>();
        for (int r = 0; r < rows; r++) {
            String row = "Row" + r;
            for (int c = 0; c < cols; c++) {
                String col = "Col" + c;
                points.add(new ChartPoint(row, null, r * 10.0 + c, col, List.of(row), List.of(col)));
            }
        }
        return new ChartDataset("Row", "Value", points, null, List.of("Row"), List.of("Col"),
                MatrixDisplayOptions.DEFAULT);
    }
}
