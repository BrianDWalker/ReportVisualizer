/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetUpdate;
import java.util.function.Consumer;

/** Lifecycle boundary between the UI and the active DBeaver result set. */
public interface ResultSetService extends AutoCloseable {
    void start(Consumer<ResultSetUpdate> updateConsumer);
    void setSource(ResultSource source);
    String sourceQuery();
    boolean previewQuery(String title, String sql);
    void executeQuery(String title, String sql);
    void refresh();
    @Override void close();
}
