/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

/**
 * Full (unscrolled) content size of a Matrix/Heatmap render, shared by {@link ChartCanvas}
 * (which uses it to size the scrollbars) and export (which uses it to capture the entire
 * matrix instead of only the currently visible, scrolled viewport).
 */
public final class MatrixCanvasMetrics {
    /** Approximate on-screen width of one value column outside an actual render pass. */
    private static final int APPROXIMATE_COLUMN_WIDTH = 110;

    private MatrixCanvasMetrics() {
    }

    public static int width(ChartDataset dataset) {
        return dataset.rowLevelCount() * MatrixChartRenderer.ROW_WIDTH
                + (dataset.columnTuples().size() + (dataset.matrixOptions().rowTotals() ? 1 : 0))
                        * APPROXIMATE_COLUMN_WIDTH
                + 18;
    }

    public static int height(ChartDataset dataset) {
        return 18 + (MatrixChartRenderer.visualRowCount(dataset) + dataset.columnLevelCount())
                * MatrixChartRenderer.CELL_HEIGHT;
    }

    public static boolean isMatrixLike(ChartType type) {
        return type == ChartType.MATRIX || type == ChartType.HEATMAP;
    }
}
