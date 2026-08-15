/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

/**
 * {@link ChartGraphics} implementation that builds a real vector SVG document instead of
 * painting pixels. It is driven by exactly the same {@link ChartRenderer}/{@link ChartDrawing}
 * layout code used for on-screen rendering, so exported SVG content (title, axes, grid lines,
 * bars/lines/points, labels, legend, and matrix/pivot cells) does not drift from what is shown
 * in {@link ChartCanvas}.
 *
 * <p>Text is measured with a simple, device-independent average-character-width estimate rather
 * than live SWT font metrics, so SVG export never requires a running display connection (useful
 * for headless builds/tests). Label positions may therefore differ from the on-screen pixel
 * layout by a few pixels; this is a documented, deliberate trade-off.
 */
public final class SvgChartGraphics implements ChartGraphics {
    private static final double AVERAGE_CHAR_WIDTH_FACTOR = 0.58;
    private static final int FONT_SIZE = 12;
    private static final int LINE_HEIGHT = 15;

    private final StringBuilder body = new StringBuilder();
    private final ChartTheme theme;
    private final int width;
    private final int height;
    private ChartColor foreground;
    private ChartColor background;
    private int alpha = 255;
    private int lineWidth = 1;
    private LineStyle lineStyle = LineStyle.SOLID;

    public SvgChartGraphics(int width, int height, ChartTheme theme) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.theme = theme;
        this.foreground = theme.foreground();
        this.background = theme.background();
    }

    @Override
    public ChartTheme theme() {
        return theme;
    }

    @Override
    public void setForeground(ChartColor color) {
        this.foreground = color;
    }

    @Override
    public void setBackground(ChartColor color) {
        this.background = color;
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = Math.max(0, Math.min(255, alpha));
    }

    @Override
    public void setLineWidth(int width) {
        this.lineWidth = Math.max(1, width);
    }

    @Override
    public void setLineStyle(LineStyle style) {
        this.lineStyle = style;
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        body.append("<line x1=\"").append(x1).append("\" y1=\"").append(y1)
                .append("\" x2=\"").append(x2).append("\" y2=\"").append(y2)
                .append("\" stroke=\"").append(foreground.toHex()).append('"')
                .append(strokeOpacityAttribute())
                .append(" stroke-width=\"").append(lineWidth).append('"')
                .append(lineStyle == LineStyle.DOT ? " stroke-dasharray=\"2,3\"" : "")
                .append(" />\n");
    }

    @Override
    public void drawText(String text, int x, int y) {
        if (text == null || text.isEmpty()) return;
        int baselineY = y + Math.round(FONT_SIZE * 0.8f);
        body.append("<text x=\"").append(x).append("\" y=\"").append(baselineY)
                .append("\" font-family=\"sans-serif\" font-size=\"").append(FONT_SIZE)
                .append("\" fill=\"").append(foreground.toHex()).append('"')
                .append(fillOpacityAttribute()).append('>')
                .append(escape(text)).append("</text>\n");
    }

    @Override
    public void fillRectangle(int x, int y, int rectWidth, int rectHeight) {
        appendRect(x, y, rectWidth, rectHeight, 0, 0);
    }

    @Override
    public void fillRoundRectangle(int x, int y, int rectWidth, int rectHeight, int arcWidth, int arcHeight) {
        appendRect(x, y, rectWidth, rectHeight, arcWidth / 2, arcHeight / 2);
    }

    private void appendRect(int x, int y, int rectWidth, int rectHeight, int rx, int ry) {
        body.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                .append("\" width=\"").append(Math.max(0, rectWidth))
                .append("\" height=\"").append(Math.max(0, rectHeight)).append('"');
        if (rx > 0 || ry > 0) {
            body.append(" rx=\"").append(rx).append("\" ry=\"").append(ry).append('"');
        }
        body.append(" fill=\"").append(background.toHex()).append('"')
                .append(fillOpacityAttribute()).append(" />\n");
    }

    @Override
    public void drawOval(int x, int y, int ovalWidth, int ovalHeight) {
        body.append("<ellipse cx=\"").append(x + ovalWidth / 2.0).append("\" cy=\"")
                .append(y + ovalHeight / 2.0).append("\" rx=\"").append(ovalWidth / 2.0)
                .append("\" ry=\"").append(ovalHeight / 2.0)
                .append("\" fill=\"none\" stroke=\"").append(foreground.toHex()).append('"')
                .append(strokeOpacityAttribute())
                .append(" stroke-width=\"").append(lineWidth).append("\" />\n");
    }

    @Override
    public void fillOval(int x, int y, int ovalWidth, int ovalHeight) {
        body.append("<ellipse cx=\"").append(x + ovalWidth / 2.0).append("\" cy=\"")
                .append(y + ovalHeight / 2.0).append("\" rx=\"").append(ovalWidth / 2.0)
                .append("\" ry=\"").append(ovalHeight / 2.0)
                .append("\" fill=\"").append(background.toHex()).append('"')
                .append(fillOpacityAttribute()).append(" />\n");
    }

    @Override
    public void fillArc(int x, int y, int arcWidth, int arcHeight, int startAngle, int arcAngle) {
        double centerX = x + arcWidth / 2.0;
        double centerY = y + arcHeight / 2.0;
        double radiusX = arcWidth / 2.0;
        double radiusY = arcHeight / 2.0;
        if (Math.abs(arcAngle) >= 360) {
            body.append("<ellipse cx=\"").append(centerX).append("\" cy=\"").append(centerY)
                    .append("\" rx=\"").append(radiusX).append("\" ry=\"").append(radiusY)
                    .append("\" fill=\"").append(background.toHex()).append('"')
                    .append(fillOpacityAttribute()).append(" />\n");
            return;
        }
        double startRad = Math.toRadians(startAngle);
        double endRad = Math.toRadians(startAngle + arcAngle);
        double startX = centerX + radiusX * Math.cos(startRad);
        double startY = centerY - radiusY * Math.sin(startRad);
        double endX = centerX + radiusX * Math.cos(endRad);
        double endY = centerY - radiusY * Math.sin(endRad);
        int largeArc = Math.abs(arcAngle) > 180 ? 1 : 0;
        int sweep = arcAngle > 0 ? 0 : 1;
        body.append("<path d=\"M ").append(centerX).append(' ').append(centerY)
                .append(" L ").append(startX).append(' ').append(startY)
                .append(" A ").append(radiusX).append(' ').append(radiusY)
                .append(" 0 ").append(largeArc).append(' ').append(sweep)
                .append(' ').append(endX).append(' ').append(endY)
                .append(" Z\" fill=\"").append(background.toHex()).append('"')
                .append(fillOpacityAttribute()).append(" />\n");
    }

    @Override
    public void fillPolygon(int[] points) {
        StringBuilder pointList = new StringBuilder();
        for (int index = 0; index + 1 < points.length; index += 2) {
            if (index > 0) pointList.append(' ');
            pointList.append(points[index]).append(',').append(points[index + 1]);
        }
        body.append("<polygon points=\"").append(pointList)
                .append("\" fill=\"").append(background.toHex()).append('"')
                .append(fillOpacityAttribute()).append(" />\n");
    }

    @Override
    public TextSize textExtent(String text) {
        if (text == null || text.isEmpty()) return new TextSize(0, LINE_HEIGHT);
        int estimatedWidth = (int) Math.ceil(text.length() * FONT_SIZE * AVERAGE_CHAR_WIDTH_FACTOR);
        return new TextSize(Math.max(1, estimatedWidth), LINE_HEIGHT);
    }

    private String fillOpacityAttribute() {
        return alpha >= 255 ? "" : " fill-opacity=\"" + formatOpacity() + "\"";
    }

    private String strokeOpacityAttribute() {
        return alpha >= 255 ? "" : " stroke-opacity=\"" + formatOpacity() + "\"";
    }

    private String formatOpacity() {
        return String.format(java.util.Locale.ROOT, "%.3f", alpha / 255.0);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /** Returns the completed SVG document. Call once rendering into this instance is finished. */
    public String toSvg() {
        StringBuilder document = new StringBuilder();
        document.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        document.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
                .append("\" height=\"").append(height)
                .append("\" viewBox=\"0 0 ").append(width).append(' ').append(height)
                .append("\">\n");
        document.append("<rect x=\"0\" y=\"0\" width=\"").append(width).append("\" height=\"")
                .append(height).append("\" fill=\"").append(theme.background().toHex())
                .append("\" />\n");
        document.append(body);
        document.append("</svg>\n");
        return document.toString();
    }
}
