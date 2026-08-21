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
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerOperator;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.DateHierarchyLevel;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.DateHierarchySelection;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartDisplayOptions;
import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldDefinition;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
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

    @Test
    public void roundTripsTypedSlicersAndDateHierarchySelections() {
        Preferences prefs = InstanceScope.INSTANCE.getNode(
                "com.brianwalker.dbeaver.resultsvisualizer.tests." + UUID.randomUUID()).node("presets");
        VisualizerPresetStore store = new VisualizerPresetStore(prefs);
        ResultSetSnapshot snapshot = snapshot("Sales", "InvoiceDate", "Revenue");
        VisualizationConfiguration config = new VisualizationConfiguration(
                ChartType.BAR, List.of(0), 1, List.of(), Aggregation.SUM, null);
        List<SlicerDefinition> slicers = List.of(
                SlicerDefinition.predicate("Revenue", SlicerOperator.BETWEEN, "10.25", "99.50"),
                SlicerDefinition.predicate("InvoiceDate", SlicerOperator.LAST_N_DAYS, "30", ""));
        List<DateHierarchySelection> hierarchies = List.of(
                new DateHierarchySelection(0, DateHierarchyLevel.MONTH));

        store.save("typed filters", snapshot, config, MatrixDisplayOptions.DEFAULT, List.of(1),
                slicers, hierarchies, List.of(), List.of());

        VisualizerPreset restored = store.load("typed filters", snapshot).orElseThrow();
        assertEquals(slicers, restored.slicers());
        assertEquals(hierarchies, restored.dateHierarchies());
    }

    @Test
    public void roundTripsMultipleValuesAndChartDisplayOptions() {
        Preferences prefs = InstanceScope.INSTANCE.getNode(
                "com.brianwalker.dbeaver.resultsvisualizer.tests." + UUID.randomUUID()).node("presets");
        VisualizerPresetStore store = new VisualizerPresetStore(prefs);
        ResultSetSnapshot snapshot = snapshot("Sales", "Category", "Revenue", "Quantity");
        VisualizationConfiguration config = new VisualizationConfiguration(ChartType.COMBO,
                List.of(0), 1, List.of(), Aggregation.SUM, null)
                .withValues(List.of(1, 2))
                .withDisplayOptions(new ChartDisplayOptions(false, false, true,
                        ChartDisplayOptions.LegendPosition.RIGHT,
                        ChartDisplayOptions.PieLabelMode.VALUE, 5));

        store.save("multi values", snapshot, config, MatrixDisplayOptions.DEFAULT, List.of(), List.of(), List.of(), List.of());

        assertEquals(config, store.load("multi values", snapshot).orElseThrow().toConfiguration());
    }

    @Test
    public void roundTripsExpandedMatrixOptions() {
        Preferences prefs = InstanceScope.INSTANCE.getNode(
                "com.brianwalker.dbeaver.resultsvisualizer.tests." + UUID.randomUUID()).node("presets");
        VisualizerPresetStore store = new VisualizerPresetStore(prefs);
        ResultSetSnapshot snapshot = snapshot("Matrix", "Region", "Category", "Revenue");
        VisualizationConfiguration config = new VisualizationConfiguration(ChartType.MATRIX,
                List.of(0, 1), 2, List.of(), Aggregation.SUM, null);
        MatrixDisplayOptions options = new MatrixDisplayOptions(true, true, true, true,
                MatrixDisplayOptions.Layout.TABULAR, 3, true, true,
                MatrixDisplayOptions.ConditionalFormat.COLOR_SCALE, false, 25, 144,
                java.util.Set.of(0), java.util.Set.of("North"));

        store.save("matrix options", snapshot, config, options, List.of(2), List.of(), List.of(), List.of());

        assertEquals(options, store.load("matrix options", snapshot).orElseThrow().matrixOptions());
    }

    @Test
    public void restoresTheExactCompletePresetAfterRadicalStateChanges() {
        Preferences prefs = InstanceScope.INSTANCE.getNode(
                "com.brianwalker.dbeaver.resultsvisualizer.tests." + UUID.randomUUID()).node("presets");
        VisualizerPresetStore store = new VisualizerPresetStore(prefs);
        ResultSetSnapshot snapshot = new ResultSetSnapshot("Full preset", List.of(
                column(0, "Region", Types.VARCHAR, NormalizedDataType.STRING),
                column(1, "Category", Types.VARCHAR, NormalizedDataType.STRING),
                column(2, "InvoiceDate", Types.DATE, NormalizedDataType.DATE),
                column(3, "Revenue", Types.DECIMAL, NormalizedDataType.NUMBER),
                column(4, "Quantity", Types.INTEGER, NormalizedDataType.INTEGER),
                column(5, "Cost", Types.DECIMAL, NormalizedDataType.NUMBER),
                column(6, "ShipDate", Types.DATE, NormalizedDataType.DATE)),
                List.of(), 0, false, Instant.EPOCH);
        ChartDisplayOptions chartOptions = new ChartDisplayOptions(true, false, true,
                ChartDisplayOptions.LegendPosition.RIGHT,
                ChartDisplayOptions.PieLabelMode.PERCENT, 12);
        VisualizationConfiguration saved = new VisualizationConfiguration(ChartType.MATRIX,
                List.of(0, 1), 3, List.of(3, 4), List.of(2), Aggregation.SUM, 12_500.0,
                chartOptions);
        MatrixDisplayOptions matrix = new MatrixDisplayOptions(true, false, true, true,
                MatrixDisplayOptions.Layout.TABULAR, 2, false, true,
                MatrixDisplayOptions.ConditionalFormat.COLOR_SCALE, true, 20, 132,
                java.util.Set.of(0), java.util.Set.of("East"));
        List<SlicerDefinition> slicers = List.of(
                SlicerDefinition.typed("Region", java.util.Set.of(SlicerValue.fromValue("East"))),
                SlicerDefinition.typed("Quantity", java.util.Set.of(SlicerValue.fromValue(5))),
                SlicerDefinition.typed("InvoiceDate", java.util.Set.of(
                        SlicerValue.fromValue(LocalDate.of(2026, 8, 20)))),
                SlicerDefinition.predicate("Revenue", SlicerOperator.GREATER_THAN_OR_EQUAL, "100.50", ""),
                SlicerDefinition.predicate("ShipDate", SlicerOperator.BETWEEN,
                        "2026-08-01", "2026-08-31"));
        List<DateHierarchySelection> hierarchies = List.of(
                new DateHierarchySelection(2, DateHierarchyLevel.MONTH));
        List<SortRule> sorts = List.of(new SortRule("Revenue", SortRule.Direction.DESC),
                new SortRule("Region", SortRule.Direction.ASC));
        List<CalculatedFieldDefinition> formulas = List.of(
                new CalculatedFieldDefinition("Margin", "Revenue - Cost"));

        store.save("exact state", snapshot, saved, matrix, List.of(3, 4), slicers,
                hierarchies, sorts, formulas);

        VisualizationConfiguration radicallyChanged = VisualizationConfiguration.empty(ChartType.PIE)
                .withValue(5).withX(6).withAggregation(Aggregation.COUNT)
                .withDisplayOptions(ChartDisplayOptions.DEFAULT);
        assertFalse(saved.equals(radicallyChanged));

        VisualizerPreset restored = store.load("exact state", snapshot).orElseThrow();
        assertEquals(saved, restored.toConfiguration());
        assertEquals(matrix, restored.matrixOptions());
        assertEquals(List.of(3, 4), restored.matrixValueColumnIndexes());
        assertEquals(slicers, restored.slicers());
        assertEquals(hierarchies, restored.dateHierarchies());
        assertEquals(sorts, restored.sortRules());
        assertEquals(formulas, restored.calculatedFields());
    }

    private static ResultColumn column(int index, String name, int jdbcType,
            NormalizedDataType type) {
        return new ResultColumn(index, name, name, jdbcType, type.name(), type,
                Nullability.NULLABLE);
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
