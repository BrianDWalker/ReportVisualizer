/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.ArrayList;
import java.util.List;

/** Pure date-level state transitions shared by the compact UI and regression tests. */
public final class DateHierarchyNavigator {
    private DateHierarchyNavigator() { }

    public static DateHierarchyLevel levelFor(List<DateHierarchySelection> selections, int fieldIndex) {
        if (selections == null) return null;
        return selections.stream().filter(selection -> selection.fieldIndex() == fieldIndex)
                .map(DateHierarchySelection::level).findFirst().orElse(null);
    }

    public static List<DateHierarchySelection> select(List<DateHierarchySelection> selections,
            int fieldIndex, DateHierarchyLevel level) {
        List<DateHierarchySelection> updated = new ArrayList<>(selections == null ? List.of() : selections);
        updated.removeIf(selection -> selection.fieldIndex() == fieldIndex);
        if (level != null) updated.add(new DateHierarchySelection(fieldIndex, level));
        return List.copyOf(updated);
    }

    /** Original drills down to Year; Day has no finer level. */
    public static DateHierarchyLevel drillDown(DateHierarchyLevel current) {
        if (current == null) return DateHierarchyLevel.YEAR;
        DateHierarchyLevel[] levels = DateHierarchyLevel.values();
        return current.ordinal() + 1 < levels.length ? levels[current.ordinal() + 1] : current;
    }

    /** Year drills up to Original; Original has no coarser level. */
    public static DateHierarchyLevel drillUp(DateHierarchyLevel current) {
        if (current == null || current == DateHierarchyLevel.YEAR) return null;
        return DateHierarchyLevel.values()[current.ordinal() - 1];
    }
}
