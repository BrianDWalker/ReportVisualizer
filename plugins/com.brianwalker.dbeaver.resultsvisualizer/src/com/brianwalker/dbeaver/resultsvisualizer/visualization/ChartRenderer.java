/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import org.eclipse.swt.graphics.Rectangle;

/**
 * Extension point for a local chart renderer. Renderers draw through {@link ChartGraphics} only,
 * so the same implementation produces on-screen SWT output ({@link SwtChartGraphics}) and vector
 * SVG export output ({@link SvgChartGraphics}) without duplicating layout logic.
 */
public interface ChartRenderer {
    ChartType type();

    void render(ChartGraphics graphics, Rectangle bounds, ChartDataset dataset);
}
