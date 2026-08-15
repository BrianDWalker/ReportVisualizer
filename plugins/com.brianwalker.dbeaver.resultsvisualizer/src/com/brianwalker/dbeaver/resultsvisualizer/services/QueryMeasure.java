/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

/** Named value expression aggregated by a generated full-source query. */
public record QueryMeasure(String alias, String expression) {
    public QueryMeasure {
        if (alias == null || alias.isBlank()) throw new IllegalArgumentException("Measure name is required.");
        if (expression == null || expression.isBlank()) throw new IllegalArgumentException("Measure expression is required.");
    }
}
