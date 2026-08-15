/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import java.util.Objects;

/** Current state delivered by the result-set service to the UI. */
public record ResultSetUpdate(Status status, ResultSetSnapshot snapshot, String message) {

    public enum Status { LOADING, READY, NO_ACTIVE_RESULT, ERROR }

    public ResultSetUpdate {
        status = Objects.requireNonNull(status, "status");
        message = Objects.requireNonNullElse(message, "");
        if (status == Status.READY && snapshot == null) {
            throw new IllegalArgumentException("A ready update requires a snapshot.");
        }
    }

    public static ResultSetUpdate loading() {
        return new ResultSetUpdate(Status.LOADING, null, "Reading active result set…");
    }

    public static ResultSetUpdate ready(ResultSetSnapshot snapshot) {
        return new ResultSetUpdate(Status.READY, snapshot, "");
    }

    public static ResultSetUpdate noActiveResult() {
        return new ResultSetUpdate(Status.NO_ACTIVE_RESULT, null, "No active result set available.");
    }

    public static ResultSetUpdate error(String message) {
        return new ResultSetUpdate(Status.ERROR, null, message);
    }
}
