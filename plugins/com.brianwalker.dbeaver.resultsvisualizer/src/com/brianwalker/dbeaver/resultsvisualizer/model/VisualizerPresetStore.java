/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import com.brianwalker.dbeaver.resultsvisualizer.ResultsVisualizerPlugin;
import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/** Workspace-scoped save/load storage for complete result-visualization presets. */
public final class VisualizerPresetStore {
    private static final String NODE_NAME = "presets";
    private final Preferences root;
    public VisualizerPresetStore() { this(InstanceScope.INSTANCE.getNode(ResultsVisualizerPlugin.PLUGIN_ID).node(NODE_NAME)); }
    VisualizerPresetStore(Preferences root) { this.root = root; }

    public void save(String name, ResultSetSnapshot snapshot, VisualizationConfiguration configuration,
            MatrixDisplayOptions matrixOptions, List<Integer> matrixValues, List<SlicerDefinition> slicers,
            List<SortRule> sortRules, List<CalculatedFieldDefinition> calculatedFields) {
        save(name, snapshot, configuration, matrixOptions, matrixValues, slicers, List.of(),
                sortRules, calculatedFields);
    }

    public void save(String name, ResultSetSnapshot snapshot, VisualizationConfiguration configuration,
            MatrixDisplayOptions matrixOptions, List<Integer> matrixValues, List<SlicerDefinition> slicers,
            List<com.brianwalker.dbeaver.resultsvisualizer.visualization.DateHierarchySelection> dateHierarchies,
            List<SortRule> sortRules, List<CalculatedFieldDefinition> calculatedFields) {
        String presetName = name(name); if (presetName.isBlank()) throw new IllegalArgumentException("Preset name is required.");
        if (snapshot == null || configuration == null) throw new IllegalArgumentException("Snapshot and configuration are required.");
        VisualizerPreset preset = new VisualizerPreset(presetName, VisualizerPreset.sourceSignature(snapshot), configuration.chartType(),
                configuration.xColumnIndexes(), configuration.valueColumnIndex(), configuration.seriesColumnIndexes(), configuration.aggregation(),
                configuration.yAxisMaximum(), matrixOptions, configuration.valueColumnIndexes(), configuration.displayOptions(), matrixValues, slicers, dateHierarchies, sortRules, calculatedFields);
        try { root.put(key(presetName), preset.serialize()); root.flush(); }
        catch (BackingStoreException error) { throw new IllegalStateException("Unable to save preset.", error); }
    }
    public Optional<VisualizerPreset> load(String name, ResultSetSnapshot snapshot) {
        String presetName = name(name); if (presetName.isBlank() || snapshot == null) return Optional.empty();
        return listFor(snapshot).stream().filter(preset -> preset.name().equals(presetName)).findFirst();
    }
    public List<VisualizerPreset> listFor(ResultSetSnapshot snapshot) {
        if (snapshot == null) return List.of(); String signature = VisualizerPreset.sourceSignature(snapshot); List<VisualizerPreset> found = new ArrayList<>();
        try { for (String storedKey : root.keys()) { VisualizerPreset preset = VisualizerPreset.deserialize(storedKey, root.get(storedKey, null)); if (preset != null && preset.sourceSignature().equals(signature)) found.add(preset); } }
        catch (BackingStoreException error) { ResultsVisualizerPlugin.logError("Unable to list saved presets.", error); return List.of(); }
        return found.stream().sorted(Comparator.comparing(VisualizerPreset::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }
    public boolean delete(String name) {
        String presetName = name(name); if (presetName.isBlank()) return false;
        try { String storedKey = key(presetName); if (root.get(storedKey, null) == null) return false; root.remove(storedKey); root.flush(); return true; }
        catch (BackingStoreException error) { ResultsVisualizerPlugin.logError("Unable to delete saved preset.", error); return false; }
    }
    private static String name(String value) { return value == null ? "" : value.trim(); }
    /** Exact-name SHA-256 keys cannot collide when two display names sanitize to the same text. */
    private static String key(String value) {
        try { return "preset.v2." + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
