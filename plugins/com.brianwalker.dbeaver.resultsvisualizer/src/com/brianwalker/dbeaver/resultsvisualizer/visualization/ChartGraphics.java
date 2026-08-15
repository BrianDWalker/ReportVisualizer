/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

/**
 * Shared drawing surface implemented once for on-screen SWT rendering ({@link SwtChartGraphics})
 * and once for vector SVG export ({@link SvgChartGraphics}). Every {@link ChartRenderer} and
 * {@link ChartDrawing} helper draws through this interface only, so exported output cannot drift
 * visually from what is rendered on screen and no chart-layout logic is duplicated per format.
 */
public interface ChartGraphics {
    /** Theme colors captured once for this rendering pass. */
    ChartTheme theme();

    void setForeground(ChartColor color);

    void setBackground(ChartColor color);

    /** 0 (fully transparent) to 255 (fully opaque); applies to subsequent fill/line operations. */
    void setAlpha(int alpha);

    void setLineWidth(int width);

    void setLineStyle(LineStyle style);

    void drawLine(int x1, int y1, int x2, int y2);

    /** Draws text with (x, y) as the top-left corner of its bounding box, matching SWT's GC. */
    void drawText(String text, int x, int y);

    void fillRectangle(int x, int y, int width, int height);

    void fillRoundRectangle(int x, int y, int width, int height, int arcWidth, int arcHeight);

    void drawOval(int x, int y, int width, int height);

    void fillOval(int x, int y, int width, int height);

    /** Angles in degrees; 0 degrees is the 3-o'clock position, increasing counter-clockwise. */
    void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle);

    /** Coordinates alternate x, y, x, y, ... as in SWT's {@code GC.fillPolygon}. */
    void fillPolygon(int[] points);

    /** Measures {@code text} using the same font metrics that will be used to draw it. */
    TextSize textExtent(String text);

    enum LineStyle { SOLID, DOT }

    record TextSize(int width, int height) {}
}
