/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable row containing only portable Java values. */
public record ResultRow(int sourceIndex, List<Object> values) {

    public ResultRow {
        if (sourceIndex < 0) {
            throw new IllegalArgumentException("Row index must be zero or greater.");
        }
        values = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(values, "values")));
    }
}
