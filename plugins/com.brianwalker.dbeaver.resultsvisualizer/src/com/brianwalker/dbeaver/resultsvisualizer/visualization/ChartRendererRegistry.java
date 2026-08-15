/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Registry that allows renderers to be added without changing the view. */
public final class ChartRendererRegistry {
    private final Map<ChartType, ChartRenderer> renderers;

    public ChartRendererRegistry(List<ChartRenderer> renderers) {
        EnumMap<ChartType, ChartRenderer> byType = new EnumMap<>(ChartType.class);
        for (ChartRenderer renderer : renderers) {
            if (byType.put(renderer.type(), renderer) != null) {
                throw new IllegalArgumentException("Duplicate chart renderer: " + renderer.type());
            }
        }
        this.renderers = Map.copyOf(byType);
    }

    public static ChartRendererRegistry defaults() {
        return new ChartRendererRegistry(List.of(
                new BarChartRenderer(), new HorizontalBarChartRenderer(),
                new StackedBarChartRenderer(), new LineChartRenderer(),
                new AreaChartRenderer(ChartType.AREA, false),
                new AreaChartRenderer(ChartType.STACKED_AREA, true),
                new ScatterChartRenderer(), new PieChartRenderer(ChartType.PIE),
                new PieChartRenderer(ChartType.DONUT),
                new MatrixChartRenderer(ChartType.HEATMAP),
                new MatrixChartRenderer(ChartType.MATRIX)));
    }

    public ChartRenderer renderer(ChartType type) {
        ChartRenderer renderer = renderers.get(type);
        if (renderer == null) throw new IllegalArgumentException("No renderer for " + type);
        return renderer;
    }

    public List<ChartType> availableTypes() {
        return renderers.keySet().stream().sorted().toList();
    }
}
