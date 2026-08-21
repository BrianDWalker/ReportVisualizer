/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.Objects;

/** The selected local hierarchy level for one source result column. */
public record DateHierarchySelection(int fieldIndex, DateHierarchyLevel level) {
    public DateHierarchySelection {
        if (fieldIndex < 0) throw new IllegalArgumentException("A date field index is required.");
        level = Objects.requireNonNull(level, "level");
    }
}
