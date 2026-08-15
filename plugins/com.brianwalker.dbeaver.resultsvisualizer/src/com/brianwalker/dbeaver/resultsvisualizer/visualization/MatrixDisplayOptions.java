/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

/** User-selectable Matrix total and subtotal display controls. */
public record MatrixDisplayOptions(boolean rowTotals, boolean columnTotals, boolean subtotals) {
    public static final MatrixDisplayOptions DEFAULT = new MatrixDisplayOptions(true, true, false);
}
