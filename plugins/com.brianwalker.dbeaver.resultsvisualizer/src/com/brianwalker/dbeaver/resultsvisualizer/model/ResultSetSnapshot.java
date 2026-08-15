/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** A bounded, immutable copy of the active DBeaver result set. */
public record ResultSetSnapshot(
        String sourceName,
        List<ResultColumn> columns,
        List<ResultRow> rows,
        int availableRowCount,
        boolean truncated,
        Instant capturedAt) {

    public ResultSetSnapshot {
        sourceName = Objects.requireNonNullElse(sourceName, "");
        columns = List.copyOf(columns);
        rows = List.copyOf(rows);
        if (availableRowCount < rows.size()) {
            throw new IllegalArgumentException("Available row count cannot be smaller than copied rows.");
        }
        capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
    }
}
