/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** One local distinct-value filter applied before aggregation. */
public record SlicerDefinition(String fieldName, Set<SlicerValue> selectedValues) {
    public SlicerDefinition {
        if (fieldName == null || fieldName.isBlank()) throw new IllegalArgumentException("Field is required.");
        selectedValues = Set.copyOf(selectedValues);
    }

    public static SlicerDefinition fromStrings(String fieldName, Set<String> values) {
        return new SlicerDefinition(fieldName, values.stream()
                .map(SlicerValue::fromDisplayValue)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public static SlicerDefinition typed(String fieldName, Set<SlicerValue> values) {
        return new SlicerDefinition(fieldName, Set.copyOf(values));
    }

    public Set<String> displayValues() {
        return selectedValues.stream().map(SlicerValue::displayValue).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
