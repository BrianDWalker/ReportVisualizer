/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

/** A local calendar level derived from a DATE or DATETIME result value. */
public enum DateHierarchyLevel {
    YEAR("Year"), QUARTER("Quarter"), MONTH("Month"), DAY("Day");
    private final String displayName;
    DateHierarchyLevel(String displayName) { this.displayName = displayName; }
    @Override public String toString() { return displayName; }
}
