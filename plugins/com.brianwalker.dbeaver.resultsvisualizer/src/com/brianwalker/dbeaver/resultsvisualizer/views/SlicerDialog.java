/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerOperator;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerValue;
import java.util.LinkedHashSet;
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

/** Distinct-value slicer editor based on the current result snapshot. */
final class SlicerDialog extends TitleAreaDialog {
    private final ResultSetSnapshot snapshot;
    private final Consumer<String> sourceDistinctPreview;
    private Combo fieldCombo;
    private Combo operatorCombo;
    private Table valuesTable;
    private Label valuesLabel;
    private Label firstValueLabel;
    private Text firstValue;
    private Text secondValue;
    private Label secondValueLabel;
    private Composite form;
    private SlicerDefinition definition;

    SlicerDialog(Shell shell, ResultSetSnapshot snapshot, Consumer<String> sourceDistinctPreview) {
        super(shell);
        this.snapshot = snapshot;
        this.sourceDistinctPreview = sourceDistinctPreview;
        setHelpAvailable(false);
    }

    @Override public void create() {
        super.create();
        setTitle("Add Slicer");
        setMessage(snapshot.truncated()
                ? "The panel reached its row limit, so this distinct list may be incomplete. Use the source query option to verify all values."
                : "Choose a field, then select the distinct result values to keep.");
        ViewTheme.improveContrast((Composite) getDialogArea());
    }

    @Override protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        form = new Composite(area, SWT.NONE);
        form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        form.setLayout(new GridLayout(2, false));
        new Label(form, SWT.NONE).setText("Field:");
        fieldCombo = new Combo(form, SWT.DROP_DOWN | SWT.READ_ONLY);
        snapshot.columns().forEach(c -> fieldCombo.add(c.displayName()));
        fieldCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        fieldCombo.addListener(SWT.Selection, e -> updateEditor());

        new Label(form, SWT.NONE).setText("Operator:");
        operatorCombo = new Combo(form, SWT.DROP_DOWN | SWT.READ_ONLY);
        operatorCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        operatorCombo.addListener(SWT.Selection, e -> updateOperatorVisibility());

        valuesLabel = new Label(form, SWT.NONE);
        valuesLabel.setText("Values:");
        valuesTable = new Table(form, SWT.BORDER | SWT.CHECK | SWT.V_SCROLL);
        GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableData.widthHint = 420;
        tableData.heightHint = 260;
        valuesTable.setLayoutData(tableData);

        firstValueLabel = new Label(form, SWT.NONE);
        firstValueLabel.setText("Value:");
        firstValue = new Text(form, SWT.BORDER);
        firstValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        firstValue.setMessage("Enter a value");
        secondValueLabel = new Label(form, SWT.NONE);
        secondValueLabel.setText("To:");
        secondValue = new Text(form, SWT.BORDER);
        secondValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        secondValue.setMessage("Enter the end value");

        new Label(form, SWT.NONE);
        Composite actions = new Composite(form, SWT.NONE);
        actions.setLayout(new GridLayout(3, false));
        Button all = new Button(actions, SWT.PUSH);
        all.setText("Select All");
        all.addListener(SWT.Selection, e -> setAll(true));
        Button none = new Button(actions, SWT.PUSH);
        none.setText("Clear All");
        none.addListener(SWT.Selection, e -> setAll(false));
        Button source = new Button(actions, SWT.PUSH);
        source.setText("Preview DISTINCT Source Query…");
        source.setToolTipText("Generate a full-source distinct-value query in DBeaver");
        source.addListener(SWT.Selection, e -> {
            if (fieldCombo.getSelectionIndex() >= 0) sourceDistinctPreview.accept(fieldCombo.getText());
        });
        if (fieldCombo.getItemCount() > 0) { fieldCombo.select(0); updateEditor(); }
        return area;
    }

    private void updateEditor() {
        operatorCombo.removeAll();
        for (SlicerOperator operator : operatorsForSelectedColumn()) operatorCombo.add(operator.toString());
        operatorCombo.select(0);
        if (isCategory()) loadValues();
        updateOperatorVisibility();
    }

    private java.util.List<SlicerOperator> operatorsForSelectedColumn() {
        if (fieldCombo.getSelectionIndex() < 0) return java.util.List.of(SlicerOperator.IN);
        return switch (snapshot.columns().get(fieldCombo.getSelectionIndex()).normalizedType()) {
            case INTEGER, DECIMAL, NUMBER -> java.util.List.of(SlicerOperator.EQUALS, SlicerOperator.NOT_EQUALS,
                    SlicerOperator.GREATER_THAN, SlicerOperator.GREATER_THAN_OR_EQUAL,
                    SlicerOperator.LESS_THAN, SlicerOperator.LESS_THAN_OR_EQUAL, SlicerOperator.BETWEEN,
                    SlicerOperator.NOT_BETWEEN, SlicerOperator.IS_NULL, SlicerOperator.IS_NOT_NULL);
            case DATE, DATETIME -> java.util.List.of(SlicerOperator.BEFORE, SlicerOperator.AFTER,
                    SlicerOperator.ON_OR_BEFORE, SlicerOperator.ON_OR_AFTER, SlicerOperator.BETWEEN,
                    SlicerOperator.IS_NULL, SlicerOperator.IS_NOT_NULL, SlicerOperator.THIS_MONTH,
                    SlicerOperator.THIS_QUARTER, SlicerOperator.THIS_YEAR, SlicerOperator.LAST_N_DAYS,
                    SlicerOperator.LAST_N_MONTHS, SlicerOperator.LAST_N_YEARS);
            default -> java.util.List.of(SlicerOperator.IN);
        };
    }

    private boolean isCategory() { return selectedOperator() == SlicerOperator.IN; }
    private SlicerOperator selectedOperator() {
        int index = operatorCombo.getSelectionIndex();
        java.util.List<SlicerOperator> options = operatorsForSelectedColumn();
        return index < 0 ? options.get(0) : options.get(index);
    }

    private void updateOperatorVisibility() {
        boolean category = isCategory();
        SlicerOperator operator = selectedOperator();
        setVisible(valuesLabel, category); setVisible(valuesTable, category);
        boolean first = !category && operator.valueCount() >= 1;
        boolean second = !category && operator.valueCount() == 2;
        setVisible(firstValueLabel, first);
        setVisible(firstValue, first);
        setVisible(secondValueLabel, second); setVisible(secondValue, second);
        if (!category && isDateColumn()) {
            firstValue.setMessage(operator.isRelativeDate() && operator.valueCount() == 1
                    ? "Positive number" : "YYYY-MM-DD");
            secondValue.setMessage("YYYY-MM-DD");
        } else if (!category) {
            firstValue.setMessage("Enter a number");
            secondValue.setMessage("Enter the end number");
        }
        // createDialogArea invokes this before TitleAreaDialog has assigned its
        // dialog-area field, so laying out getDialogArea() here can be null.
        layoutIfReady(form);
    }

    static void layoutIfReady(Composite composite) {
        if (composite != null && !composite.isDisposed()) composite.layout(true, true);
    }

    private boolean isDateColumn() {
        if (fieldCombo.getSelectionIndex() < 0) return false;
        var type = snapshot.columns().get(fieldCombo.getSelectionIndex()).normalizedType();
        return type == com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType.DATE
                || type == com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType.DATETIME;
    }

    private void loadValues() {
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
            item.setChecked(true);
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
        Set<SlicerValue> selected = new LinkedHashSet<>();
        for (TableItem item : valuesTable.getItems()) if (item.getChecked() && item.getData() instanceof SlicerValue value) selected.add(value);
        if (fieldCombo.getSelectionIndex() < 0) { setErrorMessage("Choose a field."); return; }
        if (isCategory()) {
            if (selected.isEmpty()) { setErrorMessage("Select at least one value."); return; }
            definition = SlicerDefinition.typed(fieldCombo.getText(), selected);
        }
        else {
            try {
                validateTypedInput();
                definition = SlicerDefinition.predicate(fieldCombo.getText(), selectedOperator(),
                        firstValue.getText(), secondValue.getText());
            } catch (IllegalArgumentException error) { setErrorMessage(error.getMessage()); return; }
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
