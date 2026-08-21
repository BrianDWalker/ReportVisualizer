/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetUpdate;
import com.brianwalker.dbeaver.resultsvisualizer.model.VisualizerSession;
import com.brianwalker.dbeaver.resultsvisualizer.model.VisualizerSessionManager;
import java.time.Instant;
import java.util.List;
import org.junit.Test;

public class AggregateRequestCorrelationTest {
    private static final Instant CAPTURED_AT = Instant.parse("2026-08-20T12:00:00Z");

    @Test public void acceptsOnlyTheCurrentRequestForTheLaunchingSourceGeneration() {
        VisualizerSessionManager manager = new VisualizerSessionManager();
        AggregateExecutionRequest current = request("results-a", 2, 7, CAPTURED_AT);
        VisualizerSession session = manager.update("results-a", value -> value
                .withBaseSnapshot(snapshot(CAPTURED_AT))
                .withSourceGeneration(7)
                .withPendingAggregateRequest(current));

        assertTrue(AggregateRequestCorrelation.matches(session, current));
        assertFalse(AggregateRequestCorrelation.matches(session,
                request("results-a", 1, 7, CAPTURED_AT)));
        assertFalse(AggregateRequestCorrelation.matches(session,
                request("results-b", 2, 7, CAPTURED_AT)));
        assertFalse(AggregateRequestCorrelation.matches(session,
                request("results-a", 2, 6, CAPTURED_AT)));
        assertFalse(AggregateRequestCorrelation.matches(session,
                request("results-a", 2, 7, CAPTURED_AT.plusSeconds(1))));
    }

    @Test public void cancellationCarriesContextAndNeverRequiresAPartialSnapshot() {
        AggregateExecutionRequest request = request("results-a", 3, 7, CAPTURED_AT);
        ResultSetUpdate cancelled = ResultSetUpdate.aggregateCancelled(request);
        assertTrue(cancelled.status() == ResultSetUpdate.Status.AGGREGATE_CANCELLED);
        assertTrue(cancelled.snapshot() == null);
        assertTrue(cancelled.aggregateRequest() == request);
    }

    private static AggregateExecutionRequest request(String session, long id,
            long generation, Instant capturedAt) {
        return new AggregateExecutionRequest(session, id, generation, capturedAt,
                new AggregateQuery("SELECT 1", List.of("x"), List.of(), "value"));
    }

    private static ResultSetSnapshot snapshot(Instant capturedAt) {
        return new ResultSetSnapshot("source", List.of(), List.of(), 0, false, capturedAt);
    }
}
