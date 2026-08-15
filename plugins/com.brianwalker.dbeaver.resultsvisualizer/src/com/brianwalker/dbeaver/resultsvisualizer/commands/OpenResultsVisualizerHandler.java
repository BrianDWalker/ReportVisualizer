/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.commands;

import com.brianwalker.dbeaver.resultsvisualizer.ResultsVisualizerPlugin;
import com.brianwalker.dbeaver.resultsvisualizer.views.ResultsVisualizerView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

/** Opens the dockable Results Visualizer workbench view. */
public final class OpenResultsVisualizerHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        try {
            window.getActivePage().showView(ResultsVisualizerView.ID).setFocus();
        } catch (PartInitException error) {
            ResultsVisualizerPlugin.logError("Unable to open the Results Visualizer view.", error);
            showError(window.getShell());
        }
        return null;
    }

    private static void showError(Shell shell) {
        MessageDialog.openError(
                shell,
                "Results Visualizer",
                "The Results Visualizer could not be opened. See the DBeaver Error Log for details.");
    }
}
