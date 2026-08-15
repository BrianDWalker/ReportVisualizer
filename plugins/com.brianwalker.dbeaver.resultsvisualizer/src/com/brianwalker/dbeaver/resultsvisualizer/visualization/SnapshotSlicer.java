/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultRow;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import java.util.List;

/** Applies slicers to an immutable snapshot without touching DBeaver's result. */
public final class SnapshotSlicer {
    private SnapshotSlicer() {}

    public static ResultSetSnapshot apply(ResultSetSnapshot snapshot, List<SlicerDefinition> slicers) {
        if (slicers.isEmpty()) return snapshot;
        List<ResultRow> rows = snapshot.rows().stream().filter(row -> slicers.stream()
                .allMatch(slicer -> matches(snapshot, row, slicer))).toList();
        return new ResultSetSnapshot(snapshot.sourceName(), snapshot.columns(), rows,
                rows.size(), false, snapshot.capturedAt(), snapshot.configuredRowLimit());
    }

    private static boolean matches(ResultSetSnapshot snapshot, ResultRow row, SlicerDefinition slicer) {
        int index = -1;
        for (int i = 0; i < snapshot.columns().size(); i++) {
            ResultColumn column = snapshot.columns().get(i);
            if (column.displayName().equalsIgnoreCase(slicer.fieldName())) { index = i; break; }
        }
        if (index < 0 || index >= row.values().size()) return false;
        Object value = row.values().get(index);
        SlicerValue candidate = SlicerValue.fromValue(value);
        return slicer.selectedValues().stream().anyMatch(item -> item.matches(candidate));
    }
}
