/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;

/** Creates result-set services without exposing DBeaver types to the view. */
public final class ResultSetServices {
    private ResultSetServices() {
    }

    public static ResultSetService create(IWorkbenchPage page, Display display) {
        return new DBeaverResultSetService(page, display);
    }
}
