/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import static org.junit.Assert.*;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetUpdate;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.junit.Test;

/** Exercises the actual adapter with a deterministic UI queue and no native SWT display. */
public class InactiveResultSetServiceTest {
    @Test public void startsPausedAndIgnoresResultEventStormsUntilActivated() {
        Harness h = new Harness();
        h.changes(10_000);
        assertTrue(h.ui.queue.isEmpty());
        assertTrue(h.ui.filters.isEmpty());
        assertEquals(0, h.discoveries);
        assertTrue(h.updates.isEmpty());

        h.service.setActive(true);
        h.changes(100);
        assertEquals(1, h.ui.queue.size());
        h.ui.drain();
        assertEquals(1, h.updates.size());
        assertTrue(h.discoveries > 0);
    }

    @Test public void hidingBeforeQueuedRefreshPreventsAllDiscoveryAndPublication() {
        Harness h = new Harness();
        h.service.setActive(true);
        h.service.setActive(false);
        h.changes(10_000);
        h.ui.drain();
        assertEquals(0, h.discoveries);
        assertTrue(h.updates.isEmpty());
        assertTrue(h.ui.filters.isEmpty());
    }

    @Test public void rapidHideShowDoesNotReviveStaleWorkOrDuplicateRefreshes() {
        Harness h = new Harness();
        h.service.setActive(true);
        h.service.setActive(false);
        h.service.setActive(true);
        h.service.setActive(true);
        h.ui.drain();
        assertEquals(1, h.updates.size());
        assertEquals(2, h.ui.filters.size());
        int discoveries = h.discoveries;
        h.service.setActive(false);
        h.changes(100);
        h.ui.drain();
        assertEquals(discoveries, h.discoveries);
        h.service.setActive(true);
        h.ui.drain();
        assertEquals(2, h.updates.size());
    }

    @Test public void inactivePartCallbacksDoNotInspectOrInstantiateParts() {
        Harness h = new Harness();
        IWorkbenchPartReference reference = (IWorkbenchPartReference) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { IWorkbenchPartReference.class },
                (proxy, method, args) -> { throw new AssertionError("Inactive part was inspected"); });
        h.service.partActivated(reference);
        h.service.partBroughtToTop(reference);
        h.service.partClosed(reference);
        h.service.partInputChanged(reference);
        assertTrue(h.ui.queue.isEmpty());
        assertEquals(0, h.discoveries);
    }

    @Test public void aggregateCompletionWaitsForResumeAndFollowsSourceRevalidation() {
        Harness h = new Harness();
        h.service.publish(completion("result-a", 1));
        h.ui.drain();
        assertTrue(h.updates.isEmpty());
        h.service.setActive(true);
        h.ui.drain();
        assertEquals(ResultSetUpdate.Status.NO_ACTIVE_RESULT, h.updates.get(0).status());
        assertEquals(ResultSetUpdate.Status.AGGREGATE_CANCELLED, h.updates.get(1).status());
        h.service.setActive(false);
        h.service.setActive(true);
        h.ui.drain();
        assertEquals(3, h.updates.size()); // completion delivered only once
    }

    @Test public void aggregateQueuedWhileActiveCannotRenderAfterHiding() {
        Harness h = new Harness();
        h.service.setActive(true);
        h.ui.drain();
        h.updates.clear();
        h.service.publish(completion("result-a", 1));
        h.service.setActive(false);
        h.ui.drain();
        assertTrue(h.updates.isEmpty());
        h.service.setActive(true);
        h.ui.drain();
        assertEquals(2, h.updates.size());
    }

    @Test public void lateOlderAggregateDoesNotReplaceLatestDeferredCompletion() {
        Harness h = new Harness();
        h.service.publish(completion("result-a", 2));
        h.service.publish(completion("result-a", 1));
        h.ui.drain();
        h.service.setActive(true);
        h.ui.drain();
        assertEquals(2, h.updates.size());
        assertEquals(2, h.updates.get(1).aggregateRequest().requestId());
    }

    @Test public void deferredCompletionsAreBoundedAcrossManyResultSessions() {
        Harness h = new Harness();
        for (int i = 0; i < 100; i++) h.service.publish(completion("result-" + i, 1));
        h.ui.drain();
        h.service.setActive(true);
        h.ui.drain();
        assertEquals(21, h.updates.size()); // one source update plus 20 retained completions
    }

    @Test public void closingRemovesListenersAndDiscardsPendingAndLateWork() {
        Harness h = new Harness();
        h.service.publish(completion("result-a", 1));
        h.ui.drain();
        h.service.setActive(true);
        h.service.close();
        h.service.close();
        h.service.setActive(true);
        h.service.publish(completion("result-b", 1));
        h.changes(100);
        h.ui.drain();
        assertTrue(h.updates.isEmpty());
        assertEquals(0, h.discoveries);
        assertTrue(h.partListeners.isEmpty());
        assertTrue(h.ui.filters.isEmpty());
    }

    private static ResultSetUpdate completion(String identity, long id) {
        return ResultSetUpdate.aggregateCancelled(new AggregateExecutionRequest(identity, id, 1,
                Instant.EPOCH, new AggregateQuery("SELECT 1", List.of("x"), List.of(), "value")));
    }

    private static final class Harness {
        final FakeUi ui = new FakeUi();
        final List<ResultSetUpdate> updates = new ArrayList<>();
        final List<IPartListener2> partListeners = new ArrayList<>();
        int discoveries;
        final IWorkbenchPage page = (IWorkbenchPage) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { IWorkbenchPage.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "addPartListener" -> partListeners.add((IPartListener2) args[0]);
                        case "removePartListener" -> partListeners.remove(args[0]);
                        case "getActivePart", "getActiveEditor" -> discoveries++;
                        default -> throw new AssertionError("Unexpected page call: " + method.getName());
                    }
                    return null;
                });
        final DBeaverResultSetService service = new DBeaverResultSetService(page, ui);
        Harness() { service.start(updates::add); }
        void changes(int count) {
            for (int i = 0; i < count; i++) {
                service.handleResultSetLoad();
                service.handleResultSetChange();
                service.onModelPrepared();
                service.refresh();
            }
        }
    }

    private static final class FakeUi implements DBeaverResultSetService.UiAccess {
        final Queue<Runnable> queue = new ArrayDeque<>();
        final Set<Integer> filters = new HashSet<>();
        @Override public boolean isDisposed() { return false; }
        @Override public void asyncExec(Runnable runnable) { queue.add(runnable); }
        @Override public void addFilter(int type, Listener listener) { assertTrue(filters.add(type)); }
        @Override public void removeFilter(int type, Listener listener) { filters.remove(type); }
        @Override public Control getFocusControl() { return null; }
        void drain() { while (!queue.isEmpty()) queue.remove().run(); }
    }
}
