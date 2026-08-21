/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import com.brianwalker.dbeaver.resultsvisualizer.model.VisualizerSession;

/** Pure correlation checks used before an asynchronous aggregate result may mutate a session. */
public final class AggregateRequestCorrelation {
    private AggregateRequestCorrelation() {
    }

    public static boolean sameRequest(AggregateExecutionRequest left,
            AggregateExecutionRequest right) {
        return left != null && right != null
                && left.requestId() == right.requestId()
                && left.sessionIdentity().equals(right.sessionIdentity());
    }

    public static boolean matches(VisualizerSession session, AggregateExecutionRequest request) {
        return session != null && request != null
                && sameRequest(session.pendingAggregateRequest(), request)
                && session.resultIdentity().equals(request.sessionIdentity())
                && session.baseSnapshot() != null
                && session.sourceGeneration() == request.sourceGeneration()
                && session.baseSnapshot().capturedAt().equals(request.sourceCapturedAt());
    }
}
