/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.eclipse.swt.graphics.Rectangle;
import org.junit.Test;

public class ChartHoverTest {
    private static final Rectangle BOUNDS = new Rectangle(0, 0, 500, 320);

    @Test public void describesOnlyTheStackedSegmentUnderThePointer() {
        ChartDataset data = new ChartDataset("Month", "Values", List.of(
                new ChartPoint("Jan", null, 10, "Total"),
                new ChartPoint("Jan", null, 2, "Invoices"),
                new ChartPoint("Feb", null, 8, "Total")));

        String tooltip = ChartHover.textAt(ChartType.STACKED_BAR, data, BOUNDS, 169, 130);

        assertTrue(tooltip.contains("Jan"));
        assertTrue(tooltip.contains("Invoices"));
        assertTrue(tooltip.contains("Value: 2"));
        assertFalse(tooltip.contains("Total"));
    }

    @Test public void describesNearbyScatterPointsAndExcludesMatrix() {
        ChartDataset data = new ChartDataset("X", "Y", List.of(
                new ChartPoint("Customer 12", 10.0, 20, "Revenue")));
        Rectangle plot = ChartDrawing.plotBounds(BOUNDS);
        ChartDrawing.Range xRange = ChartDrawing.xRange(data.points());
        ChartDrawing.Range yRange = ChartDrawing.yRange(data.points(), false, null);
        int x = ChartDrawing.numericX(plot, 10, xRange);
        int y = ChartDrawing.y(plot, 20, yRange);

        assertTrue(ChartHover.textAt(ChartType.SCATTER, data, BOUNDS, x, y).contains("Value: 20"));
        assertNull(ChartHover.textAt(ChartType.MATRIX, data, BOUNDS, x, y));
    }
}
