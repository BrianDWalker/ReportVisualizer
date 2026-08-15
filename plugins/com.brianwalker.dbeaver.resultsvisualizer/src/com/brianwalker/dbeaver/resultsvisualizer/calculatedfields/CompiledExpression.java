/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.calculatedfields;

import java.util.List;

/** Safe, immutable arithmetic expression compiled without a scripting runtime. */
public interface CompiledExpression {
    /** Returns null for null/incompatible inputs, divide-by-zero, or non-finite results. */
    Double evaluate(List<Object> rowValues);
}
