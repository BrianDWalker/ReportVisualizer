/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.brianwalker.dbeaver.resultsvisualizer.visualization.Aggregation;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartType;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.MatrixDisplayOptions;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationConfiguration;
import com.brianwalker.dbeaver.resultsvisualizer.services.AggregateExecutionRequest;
import com.brianwalker.dbeaver.resultsvisualizer.services.AggregateQuery;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.DateHierarchyLevel;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.DateHierarchySelection;
import java.time.Instant;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
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

    @Test
    public void preservesDisplayModeAcrossSessionRestoration() {
        VisualizerSessionManager manager = new VisualizerSessionManager();
        manager.update("results-panel-1", session -> session
                .withDisplayMode(VisualizerSession.DisplayMode.AGGREGATE));

        VisualizerSession restored = manager.getOrCreate("results-panel-1");

        assertEquals(VisualizerSession.DisplayMode.AGGREGATE, restored.displayMode());
    }

    @Test
    public void preservesAggregateSnapshotAndDisplayModeTogether() {
        VisualizerSessionManager manager = new VisualizerSessionManager();
        VisualizerSession session = manager.update("results-panel-1", current -> current
                .withAggregateSnapshot(new ResultSetSnapshot("aggregate", java.util.List.of(), java.util.List.of(), 0, false, java.time.Instant.now()))
                .withDisplayMode(VisualizerSession.DisplayMode.AGGREGATE));

        VisualizerSession restored = manager.getOrCreate("results-panel-1");

        assertEquals(session.aggregateSnapshot(), restored.aggregateSnapshot());
        assertEquals(VisualizerSession.DisplayMode.AGGREGATE, restored.displayMode());
    }

    @Test
    public void keepsPreAggregateConfigurationAndPendingRequestInTheirLaunchingSession() {
        VisualizerSessionManager manager = new VisualizerSessionManager();
        VisualizationConfiguration originalA = new VisualizationConfiguration(
                ChartType.LINE, 0, 1, -1, Aggregation.AVG, 500.0);
        AggregateExecutionRequest requestA = new AggregateExecutionRequest("results-panel-1", 17, 1,
                Instant.EPOCH, new AggregateQuery("SELECT 1", List.of("x"), List.of(), "value"));

        manager.update("results-panel-1", session -> session
                .withSourceConfigurationBeforeAggregate(originalA)
                .withPendingAggregateRequest(requestA));
        manager.update("results-panel-2", session -> session.withConfiguration(
                new VisualizationConfiguration(ChartType.PIE, 0, 1, -1, Aggregation.SUM, null)));

        VisualizerSession restoredA = manager.getOrCreate("results-panel-1");
        assertEquals(originalA, restoredA.sourceConfigurationBeforeAggregate());
        assertEquals(requestA, restoredA.pendingAggregateRequest());
        assertEquals(null, manager.getOrCreate("results-panel-2").sourceConfigurationBeforeAggregate());
        assertEquals(null, manager.getOrCreate("results-panel-2").pendingAggregateRequest());
    }

    @Test
    public void keepsCachedSnapshotsIndependentAcrossSeveralLoadedResultTabs() {
        VisualizerSessionManager manager = new VisualizerSessionManager();
        int[] loadedRows = {500, 600, 1_000};
        for (int index = 0; index < loadedRows.length; index++) {
            ResultSetSnapshot snapshot = snapshotWithRows(loadedRows[index]);
            manager.update("results-panel-" + index, session -> session.withBaseSnapshot(snapshot));
        }

        for (int index = 0; index < loadedRows.length; index++) {
            ResultSetSnapshot restored = manager.getOrCreate("results-panel-" + index).baseSnapshot();
            assertEquals(loadedRows[index], restored.rows().size());
            assertEquals(loadedRows[index], restored.availableRowCount());
        }
    }

    @Test
    public void keepsDateHierarchySelectionsIsolatedPerResultSession() {
        VisualizerSessionManager manager = new VisualizerSessionManager();
        List<DateHierarchySelection> firstHierarchy = List.of(new DateHierarchySelection(2, DateHierarchyLevel.MONTH));
        manager.update("results-panel-1", session -> session.withDateHierarchies(firstHierarchy));
        manager.update("results-panel-2", session -> session.withDateHierarchies(
                List.of(new DateHierarchySelection(1, DateHierarchyLevel.YEAR))));

        assertEquals(firstHierarchy, manager.getOrCreate("results-panel-1").dateHierarchies());
        assertEquals(List.of(new DateHierarchySelection(1, DateHierarchyLevel.YEAR)),
                manager.getOrCreate("results-panel-2").dateHierarchies());
    }

    @Test
    public void clearRemovesEverySession() {
        VisualizerSessionManager manager = new VisualizerSessionManager();
        manager.getOrCreate("results-panel-1");
        manager.getOrCreate("results-panel-2");

        manager.clear();

        assertEquals(0, manager.size());
    }

    @Test
    public void evictsLeastRecentlyUsedSessionsBeyondTheBoundedCapacity() {
        VisualizerSessionManager manager = new VisualizerSessionManager();
        for (int i = 0; i < 25; i++) {
            manager.getOrCreate("results-panel-" + i);
        }

        assertTrue("session count should be bounded", manager.size() <= 20);
        assertTrue("most recently touched session must still be present",
                manager.get("results-panel-24").isPresent());
        assertTrue("oldest session should have been evicted",
                manager.get("results-panel-0").isEmpty());
    }

    private static ResultSetSnapshot snapshotWithRows(int rowCount) {
        ResultColumn column = new ResultColumn(0, "value", "value", Types.INTEGER,
                "INTEGER", NormalizedDataType.INTEGER, Nullability.NOT_NULL);
        List<ResultRow> rows = new ArrayList<>(rowCount);
        for (int index = 0; index < rowCount; index++) {
            rows.add(new ResultRow(index, List.of(index)));
        }
        return new ResultSetSnapshot("loaded-" + rowCount, List.of(column), rows,
                rowCount, false, Instant.EPOCH);
    }
}
