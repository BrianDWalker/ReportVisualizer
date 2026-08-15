/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/** Plug-in lifecycle and centralized Eclipse log access. */
public final class ResultsVisualizerPlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.brianwalker.dbeaver.resultsvisualizer";
    private static ResultsVisualizerPlugin instance;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        instance = null;
        super.stop(context);
    }

    public static ResultsVisualizerPlugin getDefault() {
        return instance;
    }

    public static void logError(String message, Throwable error) {
        IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, message, error);
        ResultsVisualizerPlugin plugin = instance;
        if (plugin != null) {
            plugin.getLog().log(status);
        }
    }

}
