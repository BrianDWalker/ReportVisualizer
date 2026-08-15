/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import java.util.Objects;

/** One ordered visualization sort key. */
public record SortRule(String fieldName, Direction direction) {
    public enum Direction {
        ASC("ASC"), DESC("DESC");
        private final String displayName;
        Direction(String displayName) { this.displayName = displayName; }
        @Override public String toString() { return displayName; }
    }

    public SortRule {
        fieldName = Objects.requireNonNull(fieldName, "fieldName");
        direction = Objects.requireNonNull(direction, "direction");
    }
}
