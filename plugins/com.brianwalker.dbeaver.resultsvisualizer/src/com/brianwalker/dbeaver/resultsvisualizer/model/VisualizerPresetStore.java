/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import com.brianwalker.dbeaver.resultsvisualizer.ResultsVisualizerPlugin;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.MatrixDisplayOptions;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/** Workspace-scoped save/load storage for result-visualization presets. */
public final class VisualizerPresetStore {
    private static final String NODE_NAME = "presets";
    private final Preferences root;

    public VisualizerPresetStore() {
        this(InstanceScope.INSTANCE.getNode(ResultsVisualizerPlugin.PLUGIN_ID).node(NODE_NAME));
    }

    VisualizerPresetStore(Preferences root) {
        this.root = root;
    }

    public void save(String name, ResultSetSnapshot snapshot, VisualizationConfiguration configuration,
            MatrixDisplayOptions matrixOptions) {
        String presetName = sanitizeName(name);
        if (presetName.isBlank()) {
            throw new IllegalArgumentException("Preset name is required.");
        }
        if (snapshot == null || configuration == null) {
            throw new IllegalArgumentException("Snapshot and configuration are required.");
        }
        VisualizerPreset preset = new VisualizerPreset(
                presetName,
                VisualizerPreset.sourceSignature(snapshot),
                configuration.chartType(),
                configuration.xColumnIndexes(),
                configuration.valueColumnIndex(),
                configuration.seriesColumnIndexes(),
                configuration.aggregation(),
                configuration.yAxisMaximum(),
                matrixOptions == null ? MatrixDisplayOptions.DEFAULT : matrixOptions);
        try {
            root.put(keyFor(presetName), preset.serialize());
            root.flush();
        } catch (BackingStoreException error) {
            throw new IllegalStateException("Unable to save preset.", error);
        }
    }

    public Optional<VisualizerPreset> load(String name, ResultSetSnapshot snapshot) {
        String presetName = sanitizeName(name);
        if (presetName.isBlank() || snapshot == null) {
            return Optional.empty();
        }
        try {
            String serialized = root.get(keyFor(presetName), null);
            if (serialized == null) {
                return Optional.empty();
            }
            VisualizerPreset preset = VisualizerPreset.deserialize(presetName, serialized);
            if (preset == null || !preset.matches(snapshot)) {
                return Optional.empty();
            }
            return Optional.of(preset);
        } catch (RuntimeException error) {
            return Optional.empty();
        }
    }

    public List<VisualizerPreset> listFor(ResultSetSnapshot snapshot) {
        if (snapshot == null) {
            return List.of();
        }
        String signature = VisualizerPreset.sourceSignature(snapshot);
        List<VisualizerPreset> presets = new ArrayList<>();
        try {
            for (String key : root.keys()) {
                String serialized = root.get(key, null);
                if (serialized == null) {
                    continue;
                }
                VisualizerPreset preset = VisualizerPreset.deserialize(keyToName(key), serialized);
                if (preset != null && preset.sourceSignature().equals(signature)) {
                    presets.add(preset);
                }
            }
        } catch (BackingStoreException error) {
            ResultsVisualizerPlugin.logError("Unable to list saved presets.", error);
            return List.of();
        }
        return presets;
    }

    public boolean delete(String name) {
        String presetName = sanitizeName(name);
        if (presetName.isBlank()) {
            return false;
        }
        try {
            String key = keyFor(presetName);
            if (root.get(key, null) == null) {
                return false;
            }
            root.remove(key);
            root.flush();
            return true;
        } catch (BackingStoreException error) {
            ResultsVisualizerPlugin.logError("Unable to delete saved preset.", error);
            return false;
        }
    }

    private static String keyFor(String presetName) {
        return "preset." + presetName.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static String keyToName(String key) {
        return key.startsWith("preset.") ? key.substring("preset.".length()) : key;
    }

    private static String sanitizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
