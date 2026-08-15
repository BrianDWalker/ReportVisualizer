/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Tracks immutable visualization sessions per result identity.
 *
 * <p>Sessions are retained only for the lifetime of the owning view: {@link #clear()} is
 * expected to be called when the view is disposed. Within that lifetime, the manager is
 * additionally bounded to {@value #MAX_SESSIONS} entries using least-recently-used eviction,
 * so switching between many distinct DBeaver results/editors over a long session cannot grow
 * this map without bound while a dedicated per-controller disposal hook is not available.</p>
 */
public final class VisualizerSessionManager {
    private static final int MAX_SESSIONS = 20;

    private final Map<String, VisualizerSession> sessions =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, VisualizerSession> eldest) {
                    return size() > MAX_SESSIONS;
                }
            };

    public String sessionIdFor(String resultIdentity) {
        String safeIdentity = Objects.requireNonNullElse(resultIdentity, "");
        return safeIdentity.isBlank() ? "anonymous" : safeIdentity;
    }

    public synchronized VisualizerSession getOrCreate(String resultIdentity) {
        String sessionId = sessionIdFor(resultIdentity);
        return sessions.computeIfAbsent(sessionId, id -> VisualizerSession.empty(id, resultIdentity));
    }

    public synchronized VisualizerSession update(String resultIdentity, UnaryOperator<VisualizerSession> updater) {
        String sessionId = sessionIdFor(resultIdentity);
        VisualizerSession current = sessions.getOrDefault(sessionId, VisualizerSession.empty(sessionId, resultIdentity));
        VisualizerSession updated = updater.apply(current);
        sessions.put(sessionId, updated);
        return updated;
    }

    public synchronized boolean contains(String resultIdentity) {
        return sessions.containsKey(sessionIdFor(resultIdentity));
    }

    public synchronized Optional<VisualizerSession> get(String resultIdentity) {
        return Optional.ofNullable(sessions.get(sessionIdFor(resultIdentity)));
    }

    public synchronized void remove(String resultIdentity) {
        sessions.remove(sessionIdFor(resultIdentity));
    }

    /** Discards every tracked session. Call when the owning view is disposed. */
    public synchronized void clear() {
        sessions.clear();
    }

    public synchronized int size() {
        return sessions.size();
    }
}

