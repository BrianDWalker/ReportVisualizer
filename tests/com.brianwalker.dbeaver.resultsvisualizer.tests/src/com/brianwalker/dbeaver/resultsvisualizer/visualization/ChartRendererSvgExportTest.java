/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Verifies that every registered chart/matrix renderer produces well-formed, non-trivial SVG
 * output through {@link SvgChartGraphics} alone, with no SWT {@code Display}/{@code GC}
 * involved. This exercises the exact same {@link ChartRenderer}/{@link ChartDrawing} code paths
 * used for on-screen rendering, so it doubles as a headless regression test for the shared
 * rendering abstraction itself.
 */
public class ChartRendererSvgExportTest {

    @Test
    public void everyChartTypeRendersWellFormedSvgWithExpectedSize() throws Exception {
        ChartRendererRegistry registry = ChartRendererRegistry.defaults();
        for (ChartType type : ChartType.values()) {
            ChartDataset dataset = MatrixCanvasMetrics.isMatrixLike(type) ? matrixDataset() : seriesDataset();
            int width = 400;
            int height = 300;
            SvgChartGraphics svg = new SvgChartGraphics(width, height, ChartTheme.light());
            registry.renderer(type).render(svg, new org.eclipse.swt.graphics.Rectangle(0, 0, width, height), dataset);
            String document = svg.toSvg();

            assertTrue(type + ": SVG should declare an XML header",
                    document.startsWith("<?xml version=\"1.0\""));
            assertTrue(type + ": SVG should contain more than just the background rect",
                    document.chars().filter(c -> c == '<').count() > 2);

            Document parsed = parse(document);
            assertEquals("svg", parsed.getDocumentElement().getTagName());
            assertEquals(String.valueOf(width), parsed.getDocumentElement().getAttribute("width"));
            assertEquals(String.valueOf(height), parsed.getDocumentElement().getAttribute("height"));
        }
    }

    @Test
    public void promptMessageRendersAsSvgTextElement() throws Exception {
        SvgChartGraphics svg = new SvgChartGraphics(200, 100, ChartTheme.light());
        ChartDrawing.drawMessage(svg, new org.eclipse.swt.graphics.Rectangle(0, 0, 200, 100), "No data available");
        Document parsed = parse(svg.toSvg());
        NodeList textNodes = parsed.getElementsByTagName("text");
        assertTrue("Prompt message should render at least one <text> element", textNodes.getLength() > 0);
    }

    @Test
    public void comboSecondaryAxisRendersItsOwnScaleAndTitle() {
        ChartDataset dataset = new ChartDataset("Month", "Revenue", List.of(
                new ChartPoint("Jan", null, 1000, "Revenue"),
                new ChartPoint("Feb", null, 2000, "Revenue"),
                new ChartPoint("Jan", null, 10, "Margin %"),
                new ChartPoint("Feb", null, 20, "Margin %"))).withDisplayOptions(
                        ChartDisplayOptions.DEFAULT.withSecondaryAxis(true));
        SvgChartGraphics svg = new SvgChartGraphics(500, 300, ChartTheme.light());

        new ComboChartRenderer().render(svg,
                new org.eclipse.swt.graphics.Rectangle(0, 0, 500, 300), dataset);

        assertTrue(svg.toSvg().contains("Line series"));
    }

    @Test
    public void chartOptionsControlDataLabelsMarkersAndPieLabelsAcrossRenderers() {
        ChartDataset categorical = new ChartDataset("Category", "Value", List.of(
                new ChartPoint("Alpha", null, 37, "First"),
                new ChartPoint("Beta", null, 61, "Second")));
        ChartDisplayOptions noLabels = new ChartDisplayOptions(false, false, false,
                ChartDisplayOptions.LegendPosition.NONE, ChartDisplayOptions.PieLabelMode.CATEGORY, 0);
        ChartDisplayOptions labelsAndMarkers = new ChartDisplayOptions(true, true, false,
                ChartDisplayOptions.LegendPosition.TOP, ChartDisplayOptions.PieLabelMode.CATEGORY, 0);

        assertFalse("Horizontal bars must hide values when data labels are disabled",
                render(new HorizontalBarChartRenderer(), categorical.withDisplayOptions(noLabels)).contains(">37<"));
        assertTrue("Combo columns must render data labels when enabled",
                render(new ComboChartRenderer(), categorical.withDisplayOptions(labelsAndMarkers)).contains(">37<"));
        assertFalse("Pie labels must respect the data-label/legend settings",
                render(new PieChartRenderer(ChartType.PIE), categorical.withDisplayOptions(noLabels)).contains(">Alpha<"));
        assertTrue("Pie labels must honor the selected label mode",
                render(new PieChartRenderer(ChartType.PIE), categorical.withDisplayOptions(labelsAndMarkers)).contains(">Alpha<"));

        String noMarkers = render(new AreaChartRenderer(ChartType.AREA, false), categorical.withDisplayOptions(noLabels));
        String markers = render(new AreaChartRenderer(ChartType.AREA, false), categorical.withDisplayOptions(labelsAndMarkers));
        assertTrue("Area markers must add marker ellipses", occurrences(markers, "<ellipse") > occurrences(noMarkers, "<ellipse"));
    }

    @Test
    public void fillArcHandlesFullCircleWithoutDegenerateArc() {
        SvgChartGraphics svg = new SvgChartGraphics(100, 100, ChartTheme.light());
        svg.fillArc(10, 10, 80, 80, 0, 360);
        String document = svg.toSvg();
        assertTrue("A 360-degree arc should render as an <ellipse>, not a degenerate <path>",
                document.contains("<ellipse"));
    }

    private static Document parse(String svg) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private static ChartDataset seriesDataset() {
        return new ChartDataset("Category", "Value", List.of(
                new ChartPoint("A", 1.0, 4.0, "Series 1"),
                new ChartPoint("B", 2.0, 7.0, "Series 1"),
                new ChartPoint("C", 3.0, 2.0, "Series 1")));
    }

    private static String render(ChartRenderer renderer, ChartDataset dataset) {
        SvgChartGraphics svg = new SvgChartGraphics(500, 300, ChartTheme.light());
        renderer.render(svg, new org.eclipse.swt.graphics.Rectangle(0, 0, 500, 300), dataset);
        return svg.toSvg();
    }

    private static int occurrences(String text, String needle) {
        return text.split(java.util.regex.Pattern.quote(needle), -1).length - 1;
    }

    private static ChartDataset matrixDataset() {
        return new ChartDataset("Row", "Value", List.of(
                new ChartPoint("Row1", null, 10.0, "Col1", List.of("Row1"), List.of("Col1")),
                new ChartPoint("Row1", null, 20.0, "Col2", List.of("Row1"), List.of("Col2")),
                new ChartPoint("Row2", null, 30.0, "Col1", List.of("Row2"), List.of("Col1")),
                new ChartPoint("Row2", null, 40.0, "Col2", List.of("Row2"), List.of("Col2"))),
                null, List.of("Row"), List.of("Col"), MatrixDisplayOptions.DEFAULT);
    }
}
