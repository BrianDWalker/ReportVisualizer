/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** One typed local filter applied before aggregation. */
public record SlicerDefinition(String fieldName, Set<SlicerValue> selectedValues,
        SlicerOperator operator, String firstValue, String secondValue) {
    public SlicerDefinition {
        if (fieldName == null || fieldName.isBlank()) throw new IllegalArgumentException("Field is required.");
        selectedValues = Set.copyOf(Objects.requireNonNullElse(selectedValues, Set.of()));
        operator = Objects.requireNonNullElse(operator, SlicerOperator.IN);
        firstValue = Objects.requireNonNullElse(firstValue, "").trim();
        secondValue = Objects.requireNonNullElse(secondValue, "").trim();
        if (operator == SlicerOperator.IN && selectedValues.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one value.");
        }
        if (operator.valueCount() >= 1 && firstValue.isBlank()) {
            throw new IllegalArgumentException("A filter value is required.");
        }
        if (operator.valueCount() == 2 && secondValue.isBlank()) {
            throw new IllegalArgumentException("A second filter value is required.");
        }
    }

    public SlicerDefinition(String fieldName, Set<SlicerValue> selectedValues) {
        this(fieldName, selectedValues, SlicerOperator.IN, "", "");
    }

    public static SlicerDefinition fromStrings(String fieldName, Set<String> values) {
        return new SlicerDefinition(fieldName, values.stream()
                .map(SlicerValue::fromDisplayValue)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public static SlicerDefinition typed(String fieldName, Set<SlicerValue> values) {
        return new SlicerDefinition(fieldName, Set.copyOf(values));
    }

    public static SlicerDefinition predicate(String fieldName, SlicerOperator operator,
            String firstValue, String secondValue) {
        return new SlicerDefinition(fieldName, Set.of(), operator, firstValue, secondValue);
    }

    public boolean isCategorySelection() { return operator == SlicerOperator.IN; }

    public Set<String> displayValues() {
        return selectedValues.stream().map(SlicerValue::displayValue).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
