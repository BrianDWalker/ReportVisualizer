/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.Set;

/** One local distinct-value filter applied before aggregation. */
public record SlicerDefinition(String fieldName, Set<String> selectedValues) {
    public SlicerDefinition {
        if (fieldName == null || fieldName.isBlank()) throw new IllegalArgumentException("Field is required.");
        selectedValues = Set.copyOf(selectedValues);
    }
}
