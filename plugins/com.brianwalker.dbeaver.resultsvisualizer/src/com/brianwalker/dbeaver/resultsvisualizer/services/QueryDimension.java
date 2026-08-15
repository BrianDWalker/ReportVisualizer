/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

/** One selected database-side grouping expression and its result alias. */
public record QueryDimension(String alias, String expression) {
    public QueryDimension {
        if (alias == null || alias.isBlank()) throw new IllegalArgumentException("Dimension alias is required.");
        if (expression == null || expression.isBlank()) throw new IllegalArgumentException("Dimension expression is required.");
    }
}
