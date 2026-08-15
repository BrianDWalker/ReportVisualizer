/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

/** Local aggregation applied to values without changing SQL or database data. */
public enum Aggregation {
    SUM,
    AVG,
    MIN,
    MAX,
    COUNT,
    COUNT_DISTINCT;

    public boolean isCount() {
        return this == COUNT || this == COUNT_DISTINCT;
    }

    @Override
    public String toString() {
        return this == COUNT_DISTINCT ? "COUNT DISTINCT" : name();
    }
}
