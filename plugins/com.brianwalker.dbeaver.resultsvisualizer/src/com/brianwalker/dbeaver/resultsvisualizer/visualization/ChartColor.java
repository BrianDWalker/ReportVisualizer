/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

/**
 * Device-independent RGB color used by {@link ChartGraphics}. Unlike SWT's {@code Color}, this
 * carries no native resource and needs no disposal, which lets the same renderer code produce
 * on-screen SWT output and file-based vector/raster export output without a display connection.
 */
public record ChartColor(int red, int green, int blue) {
    public ChartColor {
        red = clamp(red);
        green = clamp(green);
        blue = clamp(blue);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /** Relative luminance used to choose a readable foreground against this color. */
    public int luminance() {
        return red * 299 + green * 587 + blue * 114;
    }

    public String toHex() {
        return String.format("#%02x%02x%02x", red, green, blue);
    }
}
