/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import com.brianwalker.dbeaver.resultsvisualizer.services.AggregateQuery;
import com.brianwalker.dbeaver.resultsvisualizer.services.AggregateQueryBuilder;
import com.brianwalker.dbeaver.resultsvisualizer.services.CalculatedFieldSqlTranslator;
import com.brianwalker.dbeaver.resultsvisualizer.services.CustomSqlDimension;
import com.brianwalker.dbeaver.resultsvisualizer.services.QueryAggregation;
import com.brianwalker.dbeaver.resultsvisualizer.services.QueryDimension;
import com.brianwalker.dbeaver.resultsvisualizer.services.QueryMeasure;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.Aggregation;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SortRule;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/** Unified field, aggregation, SQL-preview, and execution workflow. */
final class FullSqlConfigurationDialog extends TitleAreaDialog {
    private static final int SAVE_FIELDS_ID = IDialogConstants.CLIENT_ID + 1;
    private final String sourceSql;
    private final List<QueryDimension> baseDimensions;
    private final List<QueryMeasure> baseMeasures;
    private final List<String> selectedRows;
    private final List<String> selectedColumns;
    private final List<SlicerDefinition> slicers;
    private final List<SortRule> sortRules;
    private final ResultSetSnapshot snapshot;
    private final CalculatedFieldSqlTranslator translator;
    private final List<CustomSqlDimension> customFields;
    private final List<QueryAggregation> aggregations = new ArrayList<>();
    private Table availableFieldsTable;
    private Table customTable;
    private Table aggregationTable;
    private Text sqlText;
    private Label sqlLabel;
    private AggregateQuery query;
    private boolean executeRequested;

    FullSqlConfigurationDialog(Shell shell, String sourceSql,
            List<QueryDimension> baseDimensions, List<QueryMeasure> baseMeasures,
            List<CustomSqlDimension> customFields, ResultSetSnapshot snapshot,
            CalculatedFieldSqlTranslator translator, List<String> selectedRows,
            List<String> selectedColumns, String selectedMeasure, Aggregation selectedAggregation,
            List<SlicerDefinition> slicers, List<SortRule> sortRules) {
        super(shell);
        this.sourceSql = sourceSql;
        this.baseDimensions = List.copyOf(baseDimensions);
        this.baseMeasures = List.copyOf(baseMeasures);
        this.customFields = new ArrayList<>(customFields);
        this.snapshot = snapshot;
        this.translator = translator;
        this.selectedRows = List.copyOf(selectedRows);
        this.selectedColumns = List.copyOf(selectedColumns);
        this.slicers = List.copyOf(slicers);
        this.sortRules = List.copyOf(sortRules);
        QueryMeasure initial = measures().stream()
                .filter(value -> value.alias().equalsIgnoreCase(selectedMeasure)).findFirst().orElse(null);
        if (initial != null) aggregations.add(new QueryAggregation(
                selectedAggregation.name().toLowerCase() + "_" + initial.alias(), initial, selectedAggregation));
        setHelpAvailable(false);
    }

    @Override public void create() {
        super.create();
        setTitle("Source Query Builder");
        setMessage("Choose fields and aggregations, then review and execute the query.");
        refreshAll();
        ViewTheme.improveContrast((Composite) getDialogArea());
    }

    @Override protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite content = new Composite(area, SWT.NONE);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        content.setLayout(new GridLayout(3, true));

        List<String> initiallySelected = new ArrayList<>(selectedRows);
        selectedColumns.stream().filter(name -> initiallySelected.stream().noneMatch(name::equalsIgnoreCase))
                .forEach(initiallySelected::add);
        availableFieldsTable = fieldTable(content, "Available Fields", initiallySelected);
        availableFieldsTable.addListener(SWT.Selection, event -> refreshQuery());

        Composite customSection = section(content, "Custom Fields");
        customTable = dataTable(customSection, List.of("Name"), new int[] {180});
        Composite customActions = actionRow(customSection);
        button(customActions, "Add…", this::addCustom);
        button(customActions, "Edit…", this::editCustom);
        button(customActions, "Delete", this::deleteCustom);
        customTable.addListener(SWT.DefaultSelection, event -> editCustom());

        Composite aggregationSection = section(content, "Aggregations");
        aggregationTable = dataTable(aggregationSection, List.of("Output", "Agg", "Field"), new int[] {108, 62, 96});
        Composite aggregationActions = actionRow(aggregationSection);
        button(aggregationActions, "Add…", this::addAggregation);
        button(aggregationActions, "Edit…", this::editAggregation);
        button(aggregationActions, "Delete", this::deleteAggregation);
        aggregationTable.addListener(SWT.DefaultSelection, event -> editAggregation());

        Label sqlLabel = new Label(content, SWT.NONE);
        sqlLabel.setText("Generated SQL — executes in this visualization:");
        GridData labelData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        labelData.horizontalSpan = 3;
        sqlLabel.setLayoutData(labelData);
        this.sqlLabel = sqlLabel;
        sqlText = new Text(content, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
        GridData sqlData = new GridData(SWT.FILL, SWT.FILL, true, true);
        sqlData.horizontalSpan = 3;
        sqlData.widthHint = 720;
        sqlData.heightHint = 175;
        sqlText.setLayoutData(sqlData);
        return area;
    }

    private Table fieldTable(Composite parent, String title, List<String> checked) {
        Composite section = section(parent, title);
        Table table = new Table(section, SWT.BORDER | SWT.CHECK | SWT.V_SCROLL);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.heightHint = 105;
        table.setLayoutData(data);
        populateFields(table, checked);
        return table;
    }

    private static Composite section(Composite parent, String title) {
        Composite section = new Composite(parent, SWT.NONE);
        section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        section.setLayout(new GridLayout(1, false));
        new Label(section, SWT.NONE).setText(title);
        return section;
    }

    private static Table dataTable(Composite parent, List<String> titles, int[] widths) {
        Table table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE | SWT.V_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.heightHint = 105;
        table.setLayoutData(data);
        for (int index = 0; index < titles.size(); index++) {
            TableColumn column = new TableColumn(table, SWT.LEFT);
            column.setText(titles.get(index));
            column.setWidth(widths[index]);
        }
        return table;
    }

    private static Composite actionRow(Composite parent) {
        Composite actions = new Composite(parent, SWT.NONE);
        actions.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        actions.setLayout(layout);
        return actions;
    }

    private static void button(Composite parent, String text, Runnable action) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(text);
        button.addListener(SWT.Selection, event -> action.run());
    }

    private List<QueryDimension> dimensions() {
        List<QueryDimension> result = new ArrayList<>(baseDimensions);
        customFields.stream().map(AggregateQueryBuilder::customDimension).forEach(result::add);
        return result;
    }

    private List<QueryMeasure> measures() {
        List<QueryMeasure> result = new ArrayList<>(baseMeasures);
        customFields.stream().map(AggregateQueryBuilder::customMeasure).forEach(result::add);
        return result;
    }

    private void populateFields(Table table, List<String> checked) {
        table.removeAll();
        for (QueryDimension dimension : dimensions()) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(dimension.alias());
            item.setData(dimension);
            item.setChecked(checked.stream().anyMatch(name -> name.equalsIgnoreCase(dimension.alias())));
        }
    }

    private void refreshAll() {
        if (availableFieldsTable == null || availableFieldsTable.isDisposed()) return;
        List<String> selected = checkedNames(availableFieldsTable);
        populateFields(availableFieldsTable, selected);
        customTable.removeAll();
        for (CustomSqlDimension field : customFields) {
            TableItem item = new TableItem(customTable, SWT.NONE);
            item.setText(field.name());
        }
        aggregationTable.removeAll();
        for (QueryAggregation value : aggregations) {
            TableItem item = new TableItem(aggregationTable, SWT.NONE);
            item.setText(new String[] {value.alias(), value.aggregation().toString(), value.measure().alias()});
        }
        refreshQuery();
    }

    private void addCustom() {
        CustomSqlDimensionDialog dialog = new CustomSqlDimensionDialog(getShell(), snapshot, translator);
        if (dialog.open() != Window.OK || dialog.dimension() == null || duplicateCustom(dialog.dimension().name(), -1)) return;
        customFields.add(dialog.dimension());
        refreshAll();
    }

    private void editCustom() {
        int index = customTable.getSelectionIndex();
        if (index < 0) return;
        CustomSqlDimension old = customFields.get(index);
        CustomSqlDimensionDialog dialog = new CustomSqlDimensionDialog(getShell(), snapshot, translator, old);
        if (dialog.open() != Window.OK || dialog.dimension() == null || duplicateCustom(dialog.dimension().name(), index)) return;
        CustomSqlDimension updated = dialog.dimension();
        customFields.set(index, updated);
        for (int aggregationIndex = 0; aggregationIndex < aggregations.size(); aggregationIndex++) {
            QueryAggregation value = aggregations.get(aggregationIndex);
            if (value.measure().alias().equalsIgnoreCase(old.name())) {
                aggregations.set(aggregationIndex, new QueryAggregation(value.alias(),
                        AggregateQueryBuilder.customMeasure(updated), value.aggregation()));
            }
        }
        refreshAll();
        customTable.setSelection(index);
    }

    private boolean duplicateCustom(String name, int except) {
        boolean duplicate = baseDimensions.stream().anyMatch(value -> value.alias().equalsIgnoreCase(name));
        for (int index = 0; index < customFields.size(); index++) {
            if (index != except && customFields.get(index).name().equalsIgnoreCase(name)) duplicate = true;
        }
        if (duplicate) MessageDialog.openError(getShell(), "Duplicate Field", "Custom field names must be unique and different from result fields.");
        return duplicate;
    }

    private void deleteCustom() {
        int index = customTable.getSelectionIndex();
        if (index < 0) return;
        String name = customFields.remove(index).name();
        aggregations.removeIf(value -> value.measure().alias().equalsIgnoreCase(name));
        refreshAll();
    }

    private void addAggregation() {
        QueryAggregationDialog dialog = new QueryAggregationDialog(getShell(), measures(), null);
        if (dialog.open() != Window.OK || dialog.aggregation() == null || duplicateAggregation(dialog.aggregation().alias(), -1)) return;
        aggregations.add(dialog.aggregation());
        refreshAll();
    }

    private void editAggregation() {
        int index = aggregationTable.getSelectionIndex();
        if (index < 0) return;
        QueryAggregationDialog dialog = new QueryAggregationDialog(getShell(), measures(), aggregations.get(index));
        if (dialog.open() != Window.OK || dialog.aggregation() == null || duplicateAggregation(dialog.aggregation().alias(), index)) return;
        aggregations.set(index, dialog.aggregation());
        refreshAll();
        aggregationTable.setSelection(index);
    }

    private boolean duplicateAggregation(String alias, int except) {
        for (int index = 0; index < aggregations.size(); index++) {
            if (index != except && aggregations.get(index).alias().equalsIgnoreCase(alias)) {
                MessageDialog.openError(getShell(), "Duplicate Output", "Aggregation output names must be unique.");
                return true;
            }
        }
        return false;
    }

    private void deleteAggregation() {
        int index = aggregationTable.getSelectionIndex();
        if (index < 0) return;
        aggregations.remove(index);
        refreshAll();
    }

    private void refreshQuery() {
        if (sqlText == null || sqlText.isDisposed()) return;
        try {
            query = AggregateQueryBuilder.buildQuery(sourceSql, checked(availableFieldsTable), List.of(),
                    slicers, sortRules, aggregations);
            sqlLabel.setText("Generated SQL — executes in this visualization ("
                    + strategyLabel(query.strategy()) + "):");
            sqlText.setText(query.sql());
            setErrorMessage(null);
            if (getButton(IDialogConstants.OK_ID) != null) getButton(IDialogConstants.OK_ID).setEnabled(true);
        } catch (RuntimeException error) {
            query = null;
            sqlLabel.setText("Generated SQL — executes in this visualization:");
            sqlText.setText("-- " + error.getMessage());
            setErrorMessage(error.getMessage());
            if (getButton(IDialogConstants.OK_ID) != null) getButton(IDialogConstants.OK_ID).setEnabled(false);
        }
    }

    private static String strategyLabel(com.brianwalker.dbeaver.resultsvisualizer.services.DBeaverSqlDialectService.QueryStrategy strategy) {
        return strategy == com.brianwalker.dbeaver.resultsvisualizer.services.DBeaverSqlDialectService.QueryStrategy.DIRECT_REWRITE
                ? "optimized source GROUP BY" : "derived-query GROUP BY";
    }


    private static List<QueryDimension> checked(Table table) {
        return java.util.Arrays.stream(table.getItems()).filter(TableItem::getChecked)
                .map(item -> (QueryDimension) item.getData()).toList();
    }

    private static List<String> checkedNames(Table table) {
        return java.util.Arrays.stream(table.getItems()).filter(TableItem::getChecked).map(TableItem::getText).toList();
    }

    @Override protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, SAVE_FIELDS_ID, "Save Fields", false);
        createButton(parent, IDialogConstants.OK_ID, "Execute", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override protected void buttonPressed(int buttonId) {
        if (buttonId == SAVE_FIELDS_ID) {
            executeRequested = false;
            setReturnCode(Window.OK);
            close();
            return;
        }
        super.buttonPressed(buttonId);
    }

    @Override protected void okPressed() {
        refreshQuery();
        if (query != null) {
            executeRequested = true;
            super.okPressed();
        }
    }

    AggregateQuery query() { return query; }
    List<CustomSqlDimension> customFields() { return List.copyOf(customFields); }
    boolean executeRequested() { return executeRequested; }
}
