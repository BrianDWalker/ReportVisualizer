/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import com.brianwalker.dbeaver.resultsvisualizer.services.AggregateExecutionRequest;
import java.util.Objects;

/** Current state delivered by the result-set service to the UI. */
public record ResultSetUpdate(Status status, ResultSetSnapshot snapshot, String message,
        AggregateExecutionRequest aggregateRequest) {

    public enum Status {
        LOADING, READY, NO_ACTIVE_RESULT, ERROR,
        AGGREGATE_READY, AGGREGATE_ERROR, AGGREGATE_CANCELLED
    }

    public ResultSetUpdate {
        status = Objects.requireNonNull(status, "status");
        message = Objects.requireNonNullElse(message, "");
        if ((status == Status.READY || status == Status.AGGREGATE_READY) && snapshot == null) {
            throw new IllegalArgumentException("A ready update requires a snapshot.");
        }
        if (status.name().startsWith("AGGREGATE_") && aggregateRequest == null) {
            throw new IllegalArgumentException("An aggregate update requires request context.");
        }
    }

    public static ResultSetUpdate loading() {
        return new ResultSetUpdate(Status.LOADING, null, "Reading active result set…", null);
    }

    public static ResultSetUpdate ready(ResultSetSnapshot snapshot) {
        return new ResultSetUpdate(Status.READY, snapshot, "", null);
    }

    public static ResultSetUpdate noActiveResult() {
        return new ResultSetUpdate(Status.NO_ACTIVE_RESULT, null, "No active result set available.", null);
    }

    public static ResultSetUpdate error(String message) {
        return new ResultSetUpdate(Status.ERROR, null, message, null);
    }

    public static ResultSetUpdate aggregateReady(
            AggregateExecutionRequest request, ResultSetSnapshot snapshot) {
        return new ResultSetUpdate(Status.AGGREGATE_READY, snapshot, "", request);
    }

    public static ResultSetUpdate aggregateError(
            AggregateExecutionRequest request, String message) {
        return new ResultSetUpdate(Status.AGGREGATE_ERROR, null, message, request);
    }

    public static ResultSetUpdate aggregateCancelled(AggregateExecutionRequest request) {
        return new ResultSetUpdate(Status.AGGREGATE_CANCELLED, null,
                "Source Query execution was cancelled; no partial result was applied.", request);
    }
}
