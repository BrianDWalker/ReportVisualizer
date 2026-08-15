/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.calculatedfields;

/** User-facing validation error for the restricted expression language. */
public final class CalculatedFieldException extends Exception {
    public CalculatedFieldException(String message) { super(message); }
}
