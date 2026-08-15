/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.brianwalker.dbeaver.resultsvisualizer.visualization.Aggregation;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartType;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.MatrixDisplayOptions;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationConfiguration;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.Test;
import org.osgi.service.prefs.Preferences;

public class VisualizerPresetStoreTest {

    @Test
    public void savesAndLoadsForMatchingResultSignature() {
        Preferences prefs = InstanceScope.INSTANCE.getNode(
                "com.brianwalker.dbeaver.resultsvisualizer.tests." + UUID.randomUUID()).node("presets");
        VisualizerPresetStore store = new VisualizerPresetStore(prefs);
        ResultSetSnapshot snapshot = snapshot("InvoiceReport", "Category", "Total");
        VisualizationConfiguration config = new VisualizationConfiguration(
                ChartType.BAR, List.of(0), 1, List.of(), Aggregation.SUM, 250.0);

        store.save("Q1 overview", snapshot, config, MatrixDisplayOptions.DEFAULT);
        Optional<VisualizerPreset> loaded = store.load("Q1 overview", snapshot);

        assertTrue(loaded.isPresent());
        assertEquals(config, loaded.get().toConfiguration());
        assertEquals(MatrixDisplayOptions.DEFAULT, loaded.get().matrixOptions());
    }

    @Test
    public void ignoresNonMatchingResultSignature() {
        Preferences prefs = InstanceScope.INSTANCE.getNode(
                "com.brianwalker.dbeaver.resultsvisualizer.tests." + UUID.randomUUID()).node("presets");
        VisualizerPresetStore store = new VisualizerPresetStore(prefs);
        ResultSetSnapshot base = snapshot("InvoiceReport", "Category", "Total");
        ResultSetSnapshot changed = snapshot("InvoiceReport", "Category", "Revenue");
        VisualizationConfiguration config = new VisualizationConfiguration(
                ChartType.LINE, List.of(0), 1, List.of(), Aggregation.AVG, 500.0);

        store.save("Revenue view", base, config, MatrixDisplayOptions.DEFAULT);

        assertFalse(store.load("Revenue view", changed).isPresent());
    }

    private static ResultSetSnapshot snapshot(String sourceName, String... columnNames) {
        List<ResultColumn> columns = new java.util.ArrayList<>();
        for (int index = 0; index < columnNames.length; index++) {
            columns.add(new ResultColumn(index, columnNames[index], columnNames[index],
                    Types.VARCHAR, "VARCHAR", NormalizedDataType.STRING, Nullability.NULLABLE));
        }
        return new ResultSetSnapshot(sourceName, columns, List.of(), 0, false, Instant.now());
    }
}
