/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import com.brianwalker.dbeaver.resultsvisualizer.ResultsVisualizerPlugin;
import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import com.brianwalker.dbeaver.resultsvisualizer.model.Nullability;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultRow;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetUpdate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetListener;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerMain;

/** Event-driven adapter around DBeaver's active result viewer. */
final class DBeaverResultSetService
        implements ResultSetService, IResultSetListener, IPartListener2 {

    private final IWorkbenchPage page;
    private final Display display;
    private final Listener workbenchControlListener = event -> {
        rememberFocusedGroupingController();
        schedule(false);
    };

    private Consumer<ResultSetUpdate> updateConsumer = update -> { };
    private IResultSetController attachedController;
    private IResultSetController groupingController;
    private ResultSource source = ResultSource.RESULTS;
    private boolean scheduled;
    private boolean forceRequested;
    private boolean closed;

    DBeaverResultSetService(IWorkbenchPage page, Display display) {
        this.page = page;
        this.display = display;
    }

    @Override
    public void start(Consumer<ResultSetUpdate> updateConsumer) {
        if (closed) throw new IllegalStateException("Result-set service is closed.");
        this.updateConsumer = updateConsumer;
        page.addPartListener(this);
        display.addFilter(SWT.FocusIn, workbenchControlListener);
        display.addFilter(SWT.Selection, workbenchControlListener);
        schedule(true);
    }

    @Override
    public void refresh() {
        schedule(true);
    }

    @Override
    public void setSource(ResultSource source) {
        this.source = java.util.Objects.requireNonNull(source, "source");
        rememberFocusedGroupingController();
        schedule(true);
    }

    @Override
    public String activeResultIdentity() {
        IResultSetController controller = attachedController;
        if (!isUsable(controller)) return "";
        // Identity tracks the actual controller instance plus the active source mode, so
        // Results and Grouping panels of the same editor never collide, and it stays valid
        // across editor renames/retitles which the editor title text does not survive.
        return Integer.toHexString(System.identityHashCode(controller)) + ":" + source.name();
    }

    @Override
    public String sourceQuery() {
        IResultSetController controller = attachedController;
        if (!isUsable(controller) || controller.getModel().getStatistics() == null) return "";
        return java.util.Objects.requireNonNullElse(
                controller.getModel().getStatistics().getQueryText(), "");
    }

    @Override
    public DBeaverSqlDialectService.QuoteStyle activeIdentifierQuoteStyle() {
        IResultSetController controller = attachedController;
        if (!isUsable(controller)) return DBeaverSqlDialectService.defaultQuoteStyle();
        DBCExecutionContext context = controller.getExecutionContext();
        if (context == null || context.getDataSource() == null) return DBeaverSqlDialectService.defaultQuoteStyle();
        SQLDialect dialect = SQLUtils.getDialectFromDataSource(context.getDataSource());
        return DBeaverSqlDialectService.quoteStyleFromDialect(dialect);
    }

    @Override
    public boolean previewQuery(String title, String sql) {
        IResultSetController controller = attachedController;
        if (!isUsable(controller)) throw new IllegalStateException("No active result is available.");
        UIServiceSQL service = DBWorkbench.getService(UIServiceSQL.class);
        if (service == null) throw new IllegalStateException("DBeaver SQL editor service is unavailable.");
        int action = service.openSQLViewer(controller.getExecutionContext(), title, null, sql, true, true);
        return action == IDialogConstants.PROCEED_ID;
    }

    @Override
    public void executeQuery(String title, String sql) {
        IResultSetController controller = attachedController;
        if (!isUsable(controller)) throw new IllegalStateException("No active result is available.");
        DBCExecutionContext context = controller.getExecutionContext();
        int configuredLimit = controller.getPreferenceStore().getInt(ModelPreferences.RESULT_SET_MAX_ROWS);
        new AbstractJob("Execute Results Visualizer aggregate") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    ResultSetSnapshot result = executeSnapshot(context, title, sql, configuredLimit, monitor);
                    publish(ResultSetUpdate.ready(result));
                    return Status.OK_STATUS;
                } catch (Exception error) {
                    ResultsVisualizerPlugin.logError("Unable to execute the generated aggregate query.", error);
                    publish(ResultSetUpdate.error("Aggregate query failed: "
                            + java.util.Objects.requireNonNullElse(error.getMessage(), error.getClass().getSimpleName())));
                    return Status.OK_STATUS;
                }
            }
        }.schedule();
    }

    /**
     * Runs a generated aggregate/Source Query statement and copies its result, honoring
     * DBeaver's own configured row cap ({@code ModelPreferences.RESULT_SET_MAX_ROWS}) the
     * same way DBeaver's own result viewer does: {@link DBCStatement#setLimit} asks the
     * driver to fetch at most {@code configuredLimit + 1} rows (the extra row is fetched,
     * not kept, purely to detect truncation), and the copy loop independently stops after
     * {@code configuredLimit} rows as a defense-in-depth cap for drivers that ignore
     * {@code setLimit}. A non-positive {@code configuredLimit} means "no limit configured"
     * (DBeaver's own convention), so no cap is applied in that case.
     */
    private ResultSetSnapshot executeSnapshot(DBCExecutionContext context, String title,
            String sql, int configuredLimit, DBRProgressMonitor monitor) throws Exception {
        try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.USER, title);
                DBCStatement statement = session.prepareStatement(
                        DBCStatementType.QUERY, sql, false, false, false)) {
            if (configuredLimit > 0) {
                statement.setLimit(0, configuredLimit + 1L);
            }
            if (!statement.executeStatement()) throw new IllegalStateException("The query returned no result set.");
            try (DBCResultSet resultSet = statement.openResultSet()) {
                List<? extends DBCAttributeMetaData> attributes = resultSet.getMeta().getAttributes();
                List<ResultColumn> columns = new ArrayList<>(attributes.size());
                for (int index = 0; index < attributes.size(); index++) {
                    DBCAttributeMetaData attribute = attributes.get(index);
                    columns.add(new ResultColumn(index, attribute.getName(), attribute.getLabel(),
                            attribute.getTypeID(), attribute.getTypeName(),
                            TypeNormalizer.normalize(attribute.getTypeID(), attribute.getTypeName(),
                                    attribute.getDataKind(), attribute.getScale()),
                            attribute.isRequired() ? Nullability.NOT_NULL : Nullability.NULLABLE));
                }
                List<ResultRow> rows = new ArrayList<>();
                boolean truncated = false;
                while (!monitor.isCanceled() && resultSet.nextRow()) {
                    if (configuredLimit > 0 && rows.size() >= configuredLimit) {
                        // A further row exists beyond the configured cap; stop copying it
                        // but record that the source has more data than we're showing.
                        truncated = true;
                        break;
                    }
                    List<Object> values = new ArrayList<>(attributes.size());
                    for (int index = 0; index < attributes.size(); index++) {
                        values.add(SnapshotValueConverter.convertPortable(
                                columns.get(index).normalizedType(), resultSet.getAttributeValue(index)));
                    }
                    rows.add(new ResultRow(rows.size(), values));
                }
                return new ResultSetSnapshot(title, columns, rows, rows.size(), truncated, Instant.now(),
                        configuredLimit);
            }
        }
    }

    private void publish(ResultSetUpdate update) {
        if (closed || display.isDisposed()) return;
        display.asyncExec(() -> {
            if (!closed && !display.isDisposed()) updateConsumer.accept(update);
        });
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        page.removePartListener(this);
        if (!display.isDisposed()) {
            display.removeFilter(SWT.FocusIn, workbenchControlListener);
            display.removeFilter(SWT.Selection, workbenchControlListener);
        }
        detachController();
        updateConsumer = update -> { };
    }

    private void schedule(boolean force) {
        if (closed || display.isDisposed()) return;
        forceRequested |= force;
        if (scheduled) return;
        scheduled = true;
        display.asyncExec(() -> {
            scheduled = false;
            boolean refreshSnapshot = forceRequested;
            forceRequested = false;
            resolveAndPublish(refreshSnapshot);
        });
    }

    private void resolveAndPublish(boolean force) {
        if (closed || display.isDisposed()) return;
        try {
            IResultSetController activeController = findActiveController();
            if (!isUsable(activeController)) {
                detachController();
                updateConsumer.accept(source == ResultSource.GROUPING
                        ? ResultSetUpdate.error("No grouping results are available. Open the Grouping panel, run or focus its results, then choose Refresh.")
                        : ResultSetUpdate.noActiveResult());
                return;
            }
            boolean controllerChanged = activeController != attachedController;
            if (controllerChanged) {
                detachController();
                attachedController = activeController;
                attachedController.addListener(this);
            }
            if (controllerChanged || force) {
                updateConsumer.accept(ResultSetUpdate.loading());
                updateConsumer.accept(ResultSetUpdate.ready(extract(activeController)));
            }
        } catch (RuntimeException error) {
            ResultsVisualizerPlugin.logError("Unable to read the active DBeaver result set.", error);
            updateConsumer.accept(ResultSetUpdate.error(
                    "Unable to read the active result set. See the DBeaver Error Log for details."));
        }
    }

    private IResultSetController findActiveController() {
        IResultSetController focused = findFocusedController();
        if (source == ResultSource.GROUPING) {
            if (isGroupingController(focused)) groupingController = focused;
            return isUsable(groupingController) ? groupingController : null;
        }
        if (focused != null && !isGroupingController(focused)) return focused;
        IWorkbenchPart activePart = page.getActivePart();
        IResultSetController controller = ResultSetHandlerMain.getActiveResultSet(activePart);
        if (controller != null && !isGroupingController(controller)) return controller;
        IEditorPart activeEditor = page.getActiveEditor();
        controller = activeEditor == activePart ? null
                : ResultSetHandlerMain.getActiveResultSet(activeEditor);
        return isGroupingController(controller) ? null : controller;
    }

    private IResultSetController findFocusedController() {
        IEditorPart activeEditor = page.getActiveEditor();
        return activeEditor == null ? null : ResultSetHandlerMain.getActiveResultSet(activeEditor);
    }

    private void rememberFocusedGroupingController() {
        IResultSetController controller = findFocusedController();
        if (isGroupingController(controller) && isUsable(controller)) groupingController = controller;
    }

    private static boolean isGroupingController(IResultSetController controller) {
        return controller != null && controller.getContainer().getClass().getName()
                .equals("org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.GroupingResultsContainer");
    }

    private static boolean isUsable(IResultSetController controller) {
        return controller != null && controller.getControl() != null
                && !controller.getControl().isDisposed();
    }

    private ResultSetSnapshot extract(IResultSetController controller) {
        ResultSetModel model = controller.getModel();
        List<DBDAttributeBinding> bindings = model.getVisibleLeafAttributes();
        List<ResultColumn> columns = new ArrayList<>(bindings.size());
        for (int index = 0; index < bindings.size(); index++) {
            DBDAttributeBinding binding = bindings.get(index);
            columns.add(new ResultColumn(index, binding.getName(), binding.getLabel(),
                    binding.getTypeID(), binding.getTypeName(), TypeNormalizer.normalize(binding),
                    binding.isRequired() ? Nullability.NOT_NULL : Nullability.NULLABLE));
        }

        int availableRows = model.getRowCount();
        // DBeaver has already applied the connection/editor fetch limit to this model.
        // Copy exactly the rows that DBeaver made available; do not impose a second cap.
        int copiedRowCount = availableRows;
        List<ResultRow> rows = new ArrayList<>(copiedRowCount);
        for (int rowIndex = 0; rowIndex < copiedRowCount; rowIndex++) {
            ResultSetRow sourceRow = model.getRow(rowIndex);
            List<Object> values = new ArrayList<>(bindings.size());
            for (int columnIndex = 0; columnIndex < bindings.size(); columnIndex++) {
                DBDAttributeBinding binding = bindings.get(columnIndex);
                Object value = model.getCellValue(binding, sourceRow);
                values.add(SnapshotValueConverter.convert(
                        binding, columns.get(columnIndex).normalizedType(), value));
            }
            rows.add(new ResultRow(sourceRow.getRowNumber(), values));
        }

        IEditorPart activeEditor = page.getActiveEditor();
        String sourceName = activeEditor == null ? "" : activeEditor.getTitle();
        int configuredLimit = controller.getPreferenceStore().getInt(ModelPreferences.RESULT_SET_MAX_ROWS);
        boolean limitReached = isTruncated(availableRows, configuredLimit, controller.isHasMoreData());
        return new ResultSetSnapshot(sourceName, columns, rows, availableRows,
                limitReached, Instant.now(), configuredLimit);
    }

    /**
     * True when the loaded row set should be treated as truncated relative to DBeaver's
     * configured row cap. {@code hasMoreData} (DBeaver's own "there is definitely more"
     * signal from the controller/driver) always wins. Otherwise, a non-positive
     * {@code configuredLimit} means "no limit configured" and is never truncated; when a
     * limit is configured, only {@code availableRows} strictly greater than the limit
     * counts as truncated — a result that lands exactly on the limit with no other
     * evidence of more data is presented as complete, matching DBeaver's own boundary
     * semantics for {@code ModelPreferences.RESULT_SET_MAX_ROWS}.
     */
    static boolean isTruncated(int availableRows, int configuredLimit, boolean hasMoreData) {
        return hasMoreData || (configuredLimit > 0 && availableRows > configuredLimit);
    }

    private void detachController() {
        if (attachedController == null) return;
        try {
            attachedController.removeListener(this);
        } catch (RuntimeException error) {
            ResultsVisualizerPlugin.logError("Unable to detach the result-set listener.", error);
        } finally {
            attachedController = null;
        }
    }

    @Override public void handleResultSetLoad() { schedule(true); }
    @Override public void handleResultSetChange() { schedule(true); }
    @Override public void handleResultSetSelectionChange(SelectionChangedEvent event) { }
    @Override public void onModelPrepared() { schedule(true); }
    @Override public void partActivated(IWorkbenchPartReference partRef) { schedule(false); }
    @Override public void partBroughtToTop(IWorkbenchPartReference partRef) { schedule(false); }
    @Override public void partClosed(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef == null ? null : partRef.getPart(false);
        if (part == null) return;
        IResultSetController controller = ResultSetHandlerMain.getActiveResultSet(part);
        if (controller == attachedController || controller == groupingController) {
            detachController();
            if (source == ResultSource.GROUPING) groupingController = null;
            updateConsumer.accept(ResultSetUpdate.noActiveResult());
        }
        schedule(false);
    }
    @Override public void partDeactivated(IWorkbenchPartReference partRef) { }
    @Override public void partHidden(IWorkbenchPartReference partRef) { }
    @Override public void partInputChanged(IWorkbenchPartReference partRef) { schedule(true); }
    @Override public void partOpened(IWorkbenchPartReference partRef) { schedule(false); }
    @Override public void partVisible(IWorkbenchPartReference partRef) { schedule(false); }
}
