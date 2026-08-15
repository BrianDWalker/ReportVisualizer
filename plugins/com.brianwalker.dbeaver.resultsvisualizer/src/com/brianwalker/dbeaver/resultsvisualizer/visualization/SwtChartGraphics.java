/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

/** {@link ChartGraphics} adapter that delegates every drawing call to a live SWT {@code GC}. */
public final class SwtChartGraphics implements ChartGraphics {
    private final GC gc;
    private final ChartTheme theme;

    public SwtChartGraphics(GC gc, ChartTheme theme) {
        this.gc = gc;
        this.theme = theme;
    }

    @Override
    public ChartTheme theme() {
        return theme;
    }

    @Override
    public void setForeground(ChartColor color) {
        withDisposable(color, gc::setForeground);
    }

    @Override
    public void setBackground(ChartColor color) {
        withDisposable(color, gc::setBackground);
    }

    private void withDisposable(ChartColor color, java.util.function.Consumer<Color> setter) {
        Color swtColor = new Color(gc.getDevice(), color.red(), color.green(), color.blue());
        try {
            setter.accept(swtColor);
        } finally {
            swtColor.dispose();
        }
    }

    @Override
    public void setAlpha(int alpha) {
        gc.setAlpha(alpha);
    }

    @Override
    public void setLineWidth(int width) {
        gc.setLineWidth(width);
    }

    @Override
    public void setLineStyle(LineStyle style) {
        gc.setLineStyle(style == LineStyle.DOT ? SWT.LINE_DOT : SWT.LINE_SOLID);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        gc.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void drawText(String text, int x, int y) {
        gc.drawText(text, x, y, true);
    }

    @Override
    public void fillRectangle(int x, int y, int width, int height) {
        gc.fillRectangle(x, y, width, height);
    }

    @Override
    public void fillRoundRectangle(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        gc.fillRoundRectangle(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        gc.drawOval(x, y, width, height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        gc.fillOval(x, y, width, height);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        gc.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillPolygon(int[] points) {
        gc.fillPolygon(points);
    }

    @Override
    public TextSize textExtent(String text) {
        Point size = gc.textExtent(text);
        return new TextSize(size.x, size.y);
    }
}
