/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

/** Observes workbench lifecycle events without polling or instantiating hidden parts. */
final class ViewActivityTracker implements IPartListener2, AutoCloseable {
    private final IWorkbenchPage page;
    private final IWorkbenchPart view;
    private final BooleanSupplier foreground;
    private final Consumer<Boolean> activity;
    private boolean closed;

    ViewActivityTracker(IWorkbenchPage page, IWorkbenchPart view,
            BooleanSupplier foreground, Consumer<Boolean> activity) {
        this.page = page;
        this.view = view;
        this.foreground = foreground;
        this.activity = activity;
        page.addPartListener(this);
        refresh();
    }

    void refresh() {
        if (!closed) activity.accept(page.getActivePart() == view
                && page.isPartVisible(view) && foreground.getAsBoolean());
    }

    void suspend() {
        if (!closed) activity.accept(false);
    }

    private boolean owns(IWorkbenchPartReference reference) {
        return reference != null && reference.getPart(false) == view;
    }

    @Override public void partActivated(IWorkbenchPartReference ref) { refresh(); }
    @Override public void partBroughtToTop(IWorkbenchPartReference ref) { refresh(); }
    @Override public void partVisible(IWorkbenchPartReference ref) { refresh(); }
    @Override public void partOpened(IWorkbenchPartReference ref) { refresh(); }
    @Override public void partInputChanged(IWorkbenchPartReference ref) { }
    @Override public void partDeactivated(IWorkbenchPartReference ref) { if (owns(ref)) suspend(); }
    @Override public void partHidden(IWorkbenchPartReference ref) { if (owns(ref)) suspend(); }
    @Override public void partClosed(IWorkbenchPartReference ref) { if (owns(ref)) close(); }

    @Override
    public void close() {
        if (closed) return;
        suspend();
        closed = true;
        page.removePartListener(this);
    }
}
