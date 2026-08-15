/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import java.util.List;

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

    /**
     * Aggregations that produce a meaningful result for a Values column of the given
     * normalized type. Numeric columns support the full set. Non-numeric columns (string,
     * boolean, date/time) are restricted to counting: SUM/AVG require numeric math, and
     * MIN/MAX of a non-numeric value has no well-defined numeric chart representation in
     * this local aggregation layer (a date/time MIN/MAX would need calendar-aware axis
     * rendering, which is out of scope here), so only COUNT and COUNT DISTINCT are offered.
     */
    public static List<Aggregation> compatibleWith(NormalizedDataType type) {
        return switch (type) {
            case INTEGER, DECIMAL, NUMBER -> List.of(SUM, AVG, MIN, MAX, COUNT, COUNT_DISTINCT);
            case STRING, BOOLEAN, DATE, DATETIME, TIME, OTHER -> List.of(COUNT, COUNT_DISTINCT);
        };
    }
}
