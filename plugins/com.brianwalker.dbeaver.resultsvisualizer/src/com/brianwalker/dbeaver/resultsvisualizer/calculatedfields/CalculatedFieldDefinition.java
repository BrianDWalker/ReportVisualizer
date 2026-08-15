/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.calculatedfields;

import java.util.Objects;

/** Persistable definition; expressions are recompiled against each refreshed snapshot. */
public record CalculatedFieldDefinition(String name, String expression) {
    public CalculatedFieldDefinition {
        name = Objects.requireNonNullElse(name, "").trim();
        expression = Objects.requireNonNullElse(expression, "").trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Calculated field name is required.");
        if (expression.isEmpty()) throw new IllegalArgumentException("Calculated field expression is required.");
    }
}
