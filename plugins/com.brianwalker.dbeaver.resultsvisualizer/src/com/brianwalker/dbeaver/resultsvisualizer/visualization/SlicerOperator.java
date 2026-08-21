/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

/** Typed local filter operations. */
public enum SlicerOperator {
    IN("In", 0),
    EQUALS("=", 1), NOT_EQUALS("!=", 1), GREATER_THAN(">", 1),
    GREATER_THAN_OR_EQUAL(">=", 1), LESS_THAN("<", 1),
    LESS_THAN_OR_EQUAL("<=", 1), BETWEEN("Between", 2), NOT_BETWEEN("Not Between", 2),
    BEFORE("Before", 1), AFTER("After", 1), ON_OR_BEFORE("On or Before", 1),
    ON_OR_AFTER("On or After", 1),
    IS_NULL("Is Null", 0), IS_NOT_NULL("Is Not Null", 0),
    THIS_MONTH("This Month", 0), THIS_QUARTER("This Quarter", 0), THIS_YEAR("This Year", 0),
    LAST_N_DAYS("Last N Days", 1), LAST_N_MONTHS("Last N Months", 1), LAST_N_YEARS("Last N Years", 1);

    private final String displayName;
    private final int valueCount;

    SlicerOperator(String displayName, int valueCount) {
        this.displayName = displayName;
        this.valueCount = valueCount;
    }

    public int valueCount() { return valueCount; }
    public boolean isRelativeDate() { return ordinal() >= THIS_MONTH.ordinal(); }
    @Override public String toString() { return displayName; }
}
