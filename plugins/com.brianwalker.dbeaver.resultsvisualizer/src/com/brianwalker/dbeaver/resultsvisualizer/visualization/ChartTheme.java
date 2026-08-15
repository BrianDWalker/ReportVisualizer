/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;

/**
 * The small set of theme colors renderers need, captured once as plain RGB values so the same
 * rendering code can run against a live SWT {@code Device} (on-screen) or against a file export
 * target with no display connection at all (SVG/PDF).
 */
public record ChartTheme(ChartColor background, ChartColor foreground, ChartColor gridLine,
        ChartColor selection, ChartColor normalShadow, ChartColor black, ChartColor white) {

    /** Captures the current SWT theme colors so exported output matches on-screen rendering. */
    public static ChartTheme captureFrom(Device device) {
        return new ChartTheme(
                toColor(device.getSystemColor(SWT.COLOR_LIST_BACKGROUND)),
                toColor(device.getSystemColor(SWT.COLOR_LIST_FOREGROUND)),
                toColor(device.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW)),
                toColor(device.getSystemColor(SWT.COLOR_LIST_SELECTION)),
                toColor(device.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW)),
                toColor(device.getSystemColor(SWT.COLOR_BLACK)),
                toColor(device.getSystemColor(SWT.COLOR_WHITE)));
    }

    /** A safe default light theme for callers with no live SWT device available (e.g. tests). */
    public static ChartTheme light() {
        return new ChartTheme(
                new ChartColor(255, 255, 255), new ChartColor(0, 0, 0),
                new ChartColor(200, 200, 200), new ChartColor(51, 153, 255),
                new ChartColor(120, 120, 120), new ChartColor(0, 0, 0),
                new ChartColor(255, 255, 255));
    }

    private static ChartColor toColor(org.eclipse.swt.graphics.Color color) {
        return new ChartColor(color.getRed(), color.getGreen(), color.getBlue());
    }

    public boolean isDark() {
        return background.luminance() < 128_000;
    }
}
