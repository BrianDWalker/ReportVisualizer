/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

/** Extension point for a local SWT chart renderer. */
public interface ChartRenderer {
    ChartType type();

    void render(GC graphics, Rectangle bounds, ChartDataset dataset);
}
