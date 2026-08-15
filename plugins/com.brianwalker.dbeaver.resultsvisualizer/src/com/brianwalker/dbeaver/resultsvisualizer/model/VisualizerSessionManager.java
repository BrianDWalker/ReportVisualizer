/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/** Tracks immutable visualization sessions per result identity. */
public final class VisualizerSessionManager {
    private final Map<String, VisualizerSession> sessions = new ConcurrentHashMap<>();

    public String sessionIdFor(String resultIdentity) {
        String safeIdentity = Objects.requireNonNullElse(resultIdentity, "");
        return safeIdentity.isBlank() ? "anonymous" : safeIdentity;
    }

    public VisualizerSession getOrCreate(String resultIdentity) {
        String sessionId = sessionIdFor(resultIdentity);
        return sessions.computeIfAbsent(sessionId, id -> VisualizerSession.empty(id, resultIdentity));
    }

    public VisualizerSession update(String resultIdentity, UnaryOperator<VisualizerSession> updater) {
        String sessionId = sessionIdFor(resultIdentity);
        VisualizerSession current = sessions.getOrDefault(sessionId, VisualizerSession.empty(sessionId, resultIdentity));
        VisualizerSession updated = updater.apply(current);
        sessions.put(sessionId, updated);
        return updated;
    }

    public Optional<VisualizerSession> get(String resultIdentity) {
        return Optional.ofNullable(sessions.get(sessionIdFor(resultIdentity)));
    }

    public void remove(String resultIdentity) {
        sessions.remove(sessionIdFor(resultIdentity));
    }
}
