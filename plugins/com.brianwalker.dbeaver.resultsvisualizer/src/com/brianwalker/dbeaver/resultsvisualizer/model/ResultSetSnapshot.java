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
        Instant capturedAt,
        int configuredRowLimit) {

    public ResultSetSnapshot {
        sourceName = Objects.requireNonNullElse(sourceName, "");
        columns = List.copyOf(columns);
        rows = List.copyOf(rows);
        if (availableRowCount < rows.size()) {
            throw new IllegalArgumentException("Available row count cannot be smaller than copied rows.");
        }
        capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
    }

    /**
     * Backward-compatible constructor for call sites that predate DBeaver row-limit
     * tracking; {@code configuredRowLimit} defaults to {@code 0} (unknown/unlimited),
     * matching DBeaver's own convention that a non-positive
     * {@code ModelPreferences.RESULT_SET_MAX_ROWS} means "no limit".
     */
    public ResultSetSnapshot(String sourceName, List<ResultColumn> columns, List<ResultRow> rows,
            int availableRowCount, boolean truncated, Instant capturedAt) {
        this(sourceName, columns, rows, availableRowCount, truncated, capturedAt, 0);
    }

    /**
     * True when {@code other} carries the same result data as {@code this} — same source
     * name, columns, rows, row count, and truncation state — ignoring {@link #capturedAt}
     * (always fresh per extraction) and {@link #configuredRowLimit} (a preference, not
     * data). Used to distinguish a genuine rerun/refresh of the underlying query from a
     * mere re-extraction triggered by focus moving between DBeaver result tabs, so that
     * transient refocus events don't discard a still-valid aggregate view.
     */
    public boolean sameData(ResultSetSnapshot other) {
        if (other == null) return false;
        if (this == other) return true;
        return truncated == other.truncated
                && availableRowCount == other.availableRowCount
                && sourceName.equals(other.sourceName)
                && columns.equals(other.columns)
                && rows.equals(other.rows);
    }
}
