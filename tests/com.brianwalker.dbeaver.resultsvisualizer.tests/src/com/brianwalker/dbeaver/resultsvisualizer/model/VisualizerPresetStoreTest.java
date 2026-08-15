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
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerValue;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SortRule;
import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldDefinition;
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

        store.save("Q1 overview", snapshot, config, MatrixDisplayOptions.DEFAULT, List.of(1), List.of(), List.of(), List.of());
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

        store.save("Revenue view", base, config, MatrixDisplayOptions.DEFAULT, List.of(1), List.of(), List.of(), List.of());

        assertFalse(store.load("Revenue view", changed).isPresent());
    }

    @Test
    public void restoresExactMatrixStateAfterVisualizationIsChanged() {
        Preferences prefs = InstanceScope.INSTANCE.getNode(
                "com.brianwalker.dbeaver.resultsvisualizer.tests." + UUID.randomUUID()).node("presets");
        VisualizerPresetStore store = new VisualizerPresetStore(prefs);
        ResultSetSnapshot snapshot = snapshot("Sales", "Region", "Category", "Month", "Revenue", "Cost");
        VisualizationConfiguration saved = new VisualizationConfiguration(ChartType.MATRIX, List.of(0, 1), 3,
                List.of(2), Aggregation.AVG, 999.0);
        MatrixDisplayOptions totals = new MatrixDisplayOptions(true, false, true);
        List<SlicerDefinition> slicers = List.of(SlicerDefinition.typed("Region",
                new java.util.LinkedHashSet<>(List.of(SlicerValue.fromValue("East"), SlicerValue.nullValue()))));
        List<SortRule> sorts = List.of(new SortRule("Revenue", SortRule.Direction.DESC), new SortRule("Month", SortRule.Direction.ASC));
        List<CalculatedFieldDefinition> formulas = List.of(new CalculatedFieldDefinition("Margin", "Revenue - Cost"));

        store.save("FY/26 overview", snapshot, saved, totals, List.of(3, 4), slicers, sorts, formulas);
        VisualizationConfiguration changed = new VisualizationConfiguration(ChartType.BAR, List.of(2), 4,
                List.of(), Aggregation.SUM, null);

        VisualizerPreset restored = store.load("FY/26 overview", snapshot).orElseThrow();
        assertEquals(saved, restored.toConfiguration());
        assertEquals(totals, restored.matrixOptions());
        assertEquals(List.of(3, 4), restored.matrixValueColumnIndexes());
        assertEquals(slicers, restored.slicers());
        assertEquals(sorts, restored.sortRules());
        assertEquals(formulas, restored.calculatedFields());
        assertFalse(changed.equals(restored.toConfiguration()));
    }

    @Test
    public void keepsNamesThatPreviouslySanitizedToTheSamePreferenceKeyAndIgnoresCorruption() throws Exception {
        Preferences prefs = InstanceScope.INSTANCE.getNode(
                "com.brianwalker.dbeaver.resultsvisualizer.tests." + UUID.randomUUID()).node("presets");
        VisualizerPresetStore store = new VisualizerPresetStore(prefs);
        ResultSetSnapshot snapshot = snapshot("Sales", "Region", "Revenue");
        VisualizationConfiguration config = new VisualizationConfiguration(ChartType.BAR, List.of(0), 1, List.of(), Aggregation.SUM, null);
        store.save("Q1/West", snapshot, config, MatrixDisplayOptions.DEFAULT, List.of(1), List.of(), List.of(), List.of());
        store.save("Q1:West", snapshot, config, MatrixDisplayOptions.DEFAULT, List.of(1), List.of(), List.of(), List.of());
        prefs.put("preset.v2.corrupt", "not-base64"); prefs.flush();

        assertEquals(List.of("Q1/West", "Q1:West"), store.listFor(snapshot).stream().map(VisualizerPreset::name).toList());
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
