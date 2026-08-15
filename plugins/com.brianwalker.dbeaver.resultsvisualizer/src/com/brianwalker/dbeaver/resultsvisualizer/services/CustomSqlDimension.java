/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

/** Arbitrary database SQL expression reusable as a source-query field. */
public record CustomSqlDimension(String name, String expression) {
    public CustomSqlDimension {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required.");
        if (expression == null || expression.isBlank()) throw new IllegalArgumentException("SQL expression is required.");
        if (expression.contains(";") || expression.contains("--") || expression.contains("/*")) {
            throw new IllegalArgumentException("Enter one SQL expression without statement terminators or comments.");
        }
    }
}
