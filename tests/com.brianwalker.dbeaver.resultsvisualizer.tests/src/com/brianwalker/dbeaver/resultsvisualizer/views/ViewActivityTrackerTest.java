/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.junit.Test;

public class ViewActivityTrackerTest {
    @Test public void restoredHiddenViewDoesNotActivateUntilSelected() {
        Harness h = new Harness();
        assertFalse(h.active);
        h.selected = h.view;
        h.tracker.partActivated(h.reference);
        assertFalse(h.active);
        h.visible = true;
        h.tracker.partVisible(h.reference);
        assertTrue(h.active);
    }

    @Test public void switchingToSqlEditorSuspendsEvenWhenViewRemainsVisible() {
        Harness h = new Harness();
        h.activate();
        // Workbench sends deactivation before replacing its active-part property.
        h.tracker.partDeactivated(h.reference);
        assertFalse(h.active);
        h.selected = h.editor;
        h.tracker.partActivated(null);
        assertFalse(h.active);
        h.selected = h.view;
        h.tracker.partActivated(h.reference);
        assertTrue(h.active);
    }

    @Test public void hiddenOrMinimizedAndBackgroundWindowsStayPaused() {
        Harness h = new Harness();
        h.activate();
        h.tracker.partHidden(h.reference);
        assertFalse(h.active);
        h.visible = false;
        h.tracker.refresh();
        assertFalse(h.active);
        h.visible = true;
        h.foreground = false;
        h.tracker.partVisible(h.reference);
        assertFalse(h.active);
        h.foreground = true;
        h.tracker.refresh();
        assertTrue(h.active);
        h.tracker.suspend(); // immediate shell Deactivate/Iconify/Hide notification
        assertFalse(h.active);
        h.foreground = false;
        h.tracker.refresh();
        assertFalse(h.active);
    }

    @Test public void closingStopsActivityAndLateWindowEventsCannotResumeIt() {
        Harness h = new Harness();
        h.activate();
        h.tracker.partClosed(h.reference);
        assertFalse(h.active);
        assertTrue(h.listeners.isEmpty());
        h.tracker.refresh();
        h.tracker.partActivated(h.reference);
        h.tracker.close();
        assertFalse(h.active);
    }

    private static final class Harness {
        final IWorkbenchPart view = part();
        final IWorkbenchPart editor = part();
        IWorkbenchPart selected = editor;
        boolean visible;
        boolean foreground = true;
        boolean active;
        final List<IPartListener2> listeners = new ArrayList<>();
        final IWorkbenchPartReference reference = (IWorkbenchPartReference) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { IWorkbenchPartReference.class },
                (proxy, method, args) -> {
                    assertEquals("getPart", method.getName());
                    assertEquals(false, args[0]); // never restore/instantiate a hidden part
                    return view;
                });
        final IWorkbenchPage page = (IWorkbenchPage) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { IWorkbenchPage.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getActivePart": return selected;
                        case "isPartVisible": return visible;
                        case "addPartListener": listeners.add((IPartListener2) args[0]); return null;
                        case "removePartListener": listeners.remove(args[0]); return null;
                        default: throw new AssertionError(method.getName());
                    }
                });
        final ViewActivityTracker tracker = new ViewActivityTracker(page, view,
                () -> foreground, value -> active = value);
        void activate() {
            selected = view;
            visible = true;
            tracker.partActivated(reference);
            assertTrue(active);
        }
        private static IWorkbenchPart part() {
            return (IWorkbenchPart) Proxy.newProxyInstance(ViewActivityTrackerTest.class.getClassLoader(),
                    new Class<?>[] { IWorkbenchPart.class }, (proxy, method, args) -> {
                        throw new AssertionError("Part must not be instantiated or inspected");
                    });
        }
    }
}
