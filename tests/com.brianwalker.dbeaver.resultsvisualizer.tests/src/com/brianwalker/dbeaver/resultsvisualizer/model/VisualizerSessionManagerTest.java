/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import com.brianwalker.dbeaver.resultsvisualizer.visualization.Aggregation;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartType;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.MatrixDisplayOptions;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationConfiguration;
import org.junit.Test;

public class VisualizerSessionManagerTest {

    @Test
    public void keepsIndependentSessionsPerResultIdentity() {
        VisualizerSessionManager manager = new VisualizerSessionManager();

        VisualizerSession first = manager.getOrCreate("results-panel-1");
        VisualizerSession second = manager.getOrCreate("results-panel-2");

        assertEquals("results-panel-1", first.resultIdentity());
        assertEquals("results-panel-2", second.resultIdentity());
        assertNotEquals(first.id(), second.id());
    }

    @Test
    public void preservesVisualizationStateAcrossRefreshes() {
        VisualizerSessionManager manager = new VisualizerSessionManager();
        VisualizationConfiguration config = new VisualizationConfiguration(
                ChartType.BAR, 0, 1, 2, Aggregation.SUM, 2500.0);

        manager.update("results-panel-1", session -> session
                .withConfiguration(config)
                .withMatrixOptions(new MatrixDisplayOptions(false, true, true)));

        VisualizerSession restored = manager.getOrCreate("results-panel-1");

        assertNotNull(restored.configuration());
        assertEquals(config, restored.configuration());
        assertEquals(new MatrixDisplayOptions(false, true, true), restored.matrixOptions());
    }
}
