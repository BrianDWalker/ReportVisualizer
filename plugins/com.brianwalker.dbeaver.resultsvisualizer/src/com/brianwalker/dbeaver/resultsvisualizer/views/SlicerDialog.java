/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerOperator;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerValue;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/** Compact add/edit slicer dialog with distinct-value selection for every data type. */
final class SlicerDialog extends TitleAreaDialog {
    private final ResultSetSnapshot snapshot;
    private final List<SlicerDefinition> existingSlicers;
    private final Consumer<String> sourceDistinctPreview;
    private Combo fieldCombo;
    private Combo operatorCombo;
    private Table valuesTable;
    private Label valuesLabel;
    private Button selectAllButton;
    private Button clearAllButton;
    private Label firstValueLabel;
    private Text firstValue;
    private Text secondValue;
    private Label secondValueLabel;
    private Composite form;
    private SlicerDefinition definition;

    SlicerDialog(Shell shell, ResultSetSnapshot snapshot, Consumer<String> sourceDistinctPreview) {
        this(shell, snapshot, List.of(), sourceDistinctPreview);
    }

    SlicerDialog(Shell shell, ResultSetSnapshot snapshot, List<SlicerDefinition> existingSlicers,
            Consumer<String> sourceDistinctPreview) {
        super(shell);
        this.snapshot = snapshot;
        this.existingSlicers = List.copyOf(existingSlicers == null ? List.of() : existingSlicers);
        this.sourceDistinctPreview = sourceDistinctPreview;
        setHelpAvailable(false);
    }

    @Override public void create() {
        super.create();
        setTitle("Add/Edit Slicer");
        setMessage(snapshot.truncated()
                ? "The row limit may make Select Values incomplete. Preview the DISTINCT source query to verify all values."
                : "Choose a field and edit its distinct-value selection or typed predicate.");
        ViewTheme.improveContrast((Composite) getDialogArea());
    }

    @Override protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        form = new Composite(area, SWT.NONE);
        form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout formLayout = new GridLayout(2, false);
        formLayout.marginWidth = 8;
        formLayout.marginHeight = 4;
        formLayout.horizontalSpacing = 6;
        formLayout.verticalSpacing = 4;
        form.setLayout(formLayout);

        new Label(form, SWT.NONE).setText("Field:");
        fieldCombo = new Combo(form, SWT.DROP_DOWN | SWT.READ_ONLY);
        snapshot.columns().forEach(column -> fieldCombo.add(column.displayName()));
        fieldCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        fieldCombo.addListener(SWT.Selection, event -> updateEditor());

        new Label(form, SWT.NONE).setText("Mode:");
        operatorCombo = new Combo(form, SWT.DROP_DOWN | SWT.READ_ONLY);
        operatorCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        operatorCombo.addListener(SWT.Selection, event -> updateOperatorVisibility());

        valuesLabel = new Label(form, SWT.NONE);
        valuesLabel.setText("Values:");
        valuesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        valuesTable = new Table(form, SWT.BORDER | SWT.CHECK | SWT.V_SCROLL);
        GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableData.widthHint = 360;
        tableData.heightHint = 170;
        valuesTable.setLayoutData(tableData);

        firstValueLabel = new Label(form, SWT.NONE);
        firstValueLabel.setText("Value:");
        firstValue = new Text(form, SWT.BORDER);
        firstValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        secondValueLabel = new Label(form, SWT.NONE);
        secondValueLabel.setText("To:");
        secondValue = new Text(form, SWT.BORDER);
        secondValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        new Label(form, SWT.NONE);
        Composite actions = new Composite(form, SWT.NONE);
        actions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        GridLayout actionsLayout = new GridLayout(3, false);
        actionsLayout.marginWidth = 0;
        actionsLayout.marginHeight = 0;
        actionsLayout.horizontalSpacing = 4;
        actions.setLayout(actionsLayout);
        selectAllButton = new Button(actions, SWT.PUSH);
        selectAllButton.setText("Select All");
        selectAllButton.addListener(SWT.Selection, event -> setAll(true));
        clearAllButton = new Button(actions, SWT.PUSH);
        clearAllButton.setText("Clear All");
        clearAllButton.addListener(SWT.Selection, event -> setAll(false));
        Button source = new Button(actions, SWT.PUSH);
        source.setText("Preview DISTINCT Source Query…");
        source.setToolTipText("Generate a full-source distinct-value query in DBeaver");
        source.addListener(SWT.Selection, event -> {
            if (fieldCombo.getSelectionIndex() >= 0) sourceDistinctPreview.accept(fieldCombo.getText());
        });
        if (fieldCombo.getItemCount() > 0) {
            fieldCombo.select(initialFieldIndex());
            updateEditor();
        }
        return area;
    }

    private int initialFieldIndex() {
        if (existingSlicers.isEmpty()) return 0;
        String field = existingSlicers.get(0).fieldName();
        for (int index = 0; index < fieldCombo.getItemCount(); index++) {
            if (fieldCombo.getItem(index).equalsIgnoreCase(field)) return index;
        }
        return 0;
    }

    private void updateEditor() {
        SlicerDefinition existing = existingForSelectedField();
        List<SlicerOperator> operators = operatorsForSelectedColumn();
        operatorCombo.removeAll();
        operators.forEach(operator -> operatorCombo.add(operator.toString()));
        int operatorIndex = existing == null ? 0 : operators.indexOf(existing.operator());
        operatorCombo.select(operatorIndex < 0 ? 0 : operatorIndex);
        firstValue.setText(existing == null ? "" : existing.firstValue());
        secondValue.setText(existing == null ? "" : existing.secondValue());
        if (selectedOperator() == SlicerOperator.IN) {
            loadValues(existing != null && existing.operator() == SlicerOperator.IN
                    ? existing.selectedValues() : null);
        } else {
            valuesTable.removeAll();
        }
        updateOperatorVisibility();
    }

    static List<SlicerOperator> operatorsFor(NormalizedDataType type) {
        return switch (type) {
            case INTEGER, DECIMAL, NUMBER -> List.of(SlicerOperator.IN, SlicerOperator.EQUALS,
                    SlicerOperator.NOT_EQUALS, SlicerOperator.GREATER_THAN,
                    SlicerOperator.GREATER_THAN_OR_EQUAL, SlicerOperator.LESS_THAN,
                    SlicerOperator.LESS_THAN_OR_EQUAL, SlicerOperator.BETWEEN,
                    SlicerOperator.NOT_BETWEEN, SlicerOperator.IS_NULL, SlicerOperator.IS_NOT_NULL);
            case DATE, DATETIME -> List.of(SlicerOperator.IN, SlicerOperator.BEFORE,
                    SlicerOperator.AFTER, SlicerOperator.ON_OR_BEFORE, SlicerOperator.ON_OR_AFTER,
                    SlicerOperator.BETWEEN, SlicerOperator.THIS_MONTH, SlicerOperator.THIS_QUARTER,
                    SlicerOperator.THIS_YEAR, SlicerOperator.LAST_N_DAYS,
                    SlicerOperator.LAST_N_MONTHS, SlicerOperator.LAST_N_YEARS,
                    SlicerOperator.IS_NULL, SlicerOperator.IS_NOT_NULL);
            default -> List.of(SlicerOperator.IN);
        };
    }

    private List<SlicerOperator> operatorsForSelectedColumn() {
        if (fieldCombo.getSelectionIndex() < 0) return List.of(SlicerOperator.IN);
        return operatorsFor(snapshot.columns().get(fieldCombo.getSelectionIndex()).normalizedType());
    }

    private SlicerOperator selectedOperator() {
        List<SlicerOperator> options = operatorsForSelectedColumn();
        int index = operatorCombo.getSelectionIndex();
        return index < 0 || index >= options.size() ? options.get(0) : options.get(index);
    }

    private void updateOperatorVisibility() {
        boolean selectValues = selectedOperator() == SlicerOperator.IN;
        if (selectValues && valuesTable.getItemCount() == 0) {
            SlicerDefinition existing = existingForSelectedField();
            loadValues(existing != null && existing.operator() == SlicerOperator.IN
                    ? existing.selectedValues() : null);
        }
        setVisible(valuesLabel, selectValues);
        setVisible(valuesTable, selectValues);
        setVisible(selectAllButton, selectValues);
        setVisible(clearAllButton, selectValues);
        SlicerOperator operator = selectedOperator();
        boolean first = !selectValues && operator.valueCount() >= 1;
        boolean second = !selectValues && operator.valueCount() == 2;
        setVisible(firstValueLabel, first);
        setVisible(firstValue, first);
        setVisible(secondValueLabel, second);
        setVisible(secondValue, second);
        updateValueHints(operator, selectValues);
        layoutIfReady(form);
        if (getShell() != null && !getShell().isDisposed() && getShell().isVisible()) getShell().pack();
    }

    private void updateValueHints(SlicerOperator operator, boolean selectValues) {
        if (selectValues) return;
        if (isDateColumn()) {
            firstValue.setMessage(operator.isRelativeDate() && operator.valueCount() == 1
                    ? "Positive number" : "YYYY-MM-DD");
            secondValue.setMessage("YYYY-MM-DD");
        } else {
            firstValue.setMessage("Enter a number");
            secondValue.setMessage("Enter the end number");
        }
    }

    static void layoutIfReady(Composite composite) {
        if (composite != null && !composite.isDisposed()) composite.layout(true, true);
    }

    private SlicerDefinition existingForSelectedField() {
        if (fieldCombo == null || fieldCombo.getSelectionIndex() < 0) return null;
        return existingFor(existingSlicers, fieldCombo.getText());
    }

    static SlicerDefinition existingFor(List<SlicerDefinition> slicers, String fieldName) {
        if (slicers == null || fieldName == null) return null;
        return slicers.stream().filter(existing -> existing.fieldName().equalsIgnoreCase(fieldName))
                .findFirst().orElse(null);
    }

    private boolean isDateColumn() {
        if (fieldCombo.getSelectionIndex() < 0) return false;
        NormalizedDataType type = snapshot.columns().get(fieldCombo.getSelectionIndex()).normalizedType();
        return type == NormalizedDataType.DATE || type == NormalizedDataType.DATETIME;
    }

    private void loadValues(Set<SlicerValue> selectedValues) {
        valuesTable.removeAll();
        int column = fieldCombo.getSelectionIndex();
        if (column < 0) return;
        Set<SlicerValue> distinct = new LinkedHashSet<>();
        snapshot.rows().forEach(row -> {
            Object value = column < row.values().size() ? row.values().get(column) : null;
            distinct.add(SlicerValue.fromValue(value));
        });
        distinct.stream().sorted(java.util.Comparator.comparing(SlicerValue::displayValue,
                String.CASE_INSENSITIVE_ORDER)).forEach(value -> {
            TableItem item = new TableItem(valuesTable, SWT.NONE);
            item.setText(value.sqlNull() ? "<SQL NULL>" : value.displayValue());
            item.setData(value);
            item.setChecked(selectedValues == null || selectedValues.stream().anyMatch(value::matches));
        });
    }

    private void setAll(boolean checked) {
        for (TableItem item : valuesTable.getItems()) item.setChecked(checked);
    }

    @Override protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Apply Slicer", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override protected void okPressed() {
        if (fieldCombo.getSelectionIndex() < 0) { setErrorMessage("Choose a field."); return; }
        if (selectedOperator() == SlicerOperator.IN) {
            Set<SlicerValue> selected = new LinkedHashSet<>();
            for (TableItem item : valuesTable.getItems()) {
                if (item.getChecked() && item.getData() instanceof SlicerValue value) selected.add(value);
            }
            if (selected.isEmpty()) { setErrorMessage("Select at least one value."); return; }
            definition = SlicerDefinition.typed(fieldCombo.getText(), selected);
        } else {
            try {
                validateTypedInput();
                definition = SlicerDefinition.predicate(fieldCombo.getText(), selectedOperator(),
                        firstValue.getText(), secondValue.getText());
            } catch (IllegalArgumentException error) {
                setErrorMessage(error.getMessage());
                return;
            }
        }
        super.okPressed();
    }

    private void validateTypedInput() {
        SlicerOperator operator = selectedOperator();
        if (operator.valueCount() == 0) return;
        String first = firstValue.getText().trim();
        String second = secondValue.getText().trim();
        if (first.isBlank() || (operator.valueCount() == 2 && second.isBlank())) {
            throw new IllegalArgumentException("Enter the required filter value.");
        }
        if (isDateColumn()) {
            if (operator.isRelativeDate()) {
                try { if (Integer.parseInt(first) < 1) throw new NumberFormatException(); }
                catch (NumberFormatException error) { throw new IllegalArgumentException("Enter a positive whole number."); }
            } else {
                try { java.time.LocalDate.parse(first); if (operator.valueCount() == 2) java.time.LocalDate.parse(second); }
                catch (RuntimeException error) { throw new IllegalArgumentException("Use ISO dates such as 2026-08-20."); }
            }
        } else {
            try { new java.math.BigDecimal(first); if (operator.valueCount() == 2) new java.math.BigDecimal(second); }
            catch (NumberFormatException error) { throw new IllegalArgumentException("Enter a valid number."); }
        }
    }

    private static void setVisible(Control control, boolean visible) {
        control.setVisible(visible);
        if (control.getLayoutData() instanceof GridData data) data.exclude = !visible;
    }

    SlicerDefinition definition() { return definition; }
}
