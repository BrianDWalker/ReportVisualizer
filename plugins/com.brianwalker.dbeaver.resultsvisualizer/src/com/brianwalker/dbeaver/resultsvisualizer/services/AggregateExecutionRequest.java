/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import java.time.Instant;
import java.util.Objects;

/** Immutable context that correlates a Source Query execution with its launching session. */
public record AggregateExecutionRequest(
        String sessionIdentity,
        long requestId,
        long sourceGeneration,
        Instant sourceCapturedAt,
        AggregateQuery query) {

    public AggregateExecutionRequest {
        sessionIdentity = Objects.requireNonNullElse(sessionIdentity, "");
        if (sessionIdentity.isBlank()) throw new IllegalArgumentException("A result session is required.");
        if (requestId < 1) throw new IllegalArgumentException("A positive request id is required.");
        if (sourceGeneration < 1) throw new IllegalArgumentException("A positive source generation is required.");
        sourceCapturedAt = Objects.requireNonNull(sourceCapturedAt, "sourceCapturedAt");
        query = Objects.requireNonNull(query, "query");
    }
}
