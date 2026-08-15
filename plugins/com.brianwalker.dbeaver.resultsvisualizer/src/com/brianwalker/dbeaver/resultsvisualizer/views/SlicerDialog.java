/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerDefinition;
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

/** Distinct-value slicer editor based on the current result snapshot. */
final class SlicerDialog extends TitleAreaDialog {
    private final ResultSetSnapshot snapshot;
    private final Consumer<String> sourceDistinctPreview;
    private Combo fieldCombo;
    private Table valuesTable;
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
        Composite form = new Composite(area, SWT.NONE);
        form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        form.setLayout(new GridLayout(2, false));
        new Label(form, SWT.NONE).setText("Field:");
        fieldCombo = new Combo(form, SWT.DROP_DOWN | SWT.READ_ONLY);
        snapshot.columns().forEach(c -> fieldCombo.add(c.displayName()));
        fieldCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        fieldCombo.addListener(SWT.Selection, e -> loadValues());

        new Label(form, SWT.NONE).setText("Values:");
        valuesTable = new Table(form, SWT.BORDER | SWT.CHECK | SWT.V_SCROLL);
        GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableData.widthHint = 420;
        tableData.heightHint = 260;
        valuesTable.setLayoutData(tableData);

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
        if (fieldCombo.getItemCount() > 0) { fieldCombo.select(0); loadValues(); }
        return area;
    }

    private void loadValues() {
        valuesTable.removeAll();
        int column = fieldCombo.getSelectionIndex();
        if (column < 0) return;
        Set<String> distinct = new LinkedHashSet<>();
        snapshot.rows().forEach(row -> {
            Object value = column < row.values().size() ? row.values().get(column) : null;
            distinct.add(value == null ? "(null)" : value.toString());
        });
        distinct.stream().sorted(String.CASE_INSENSITIVE_ORDER).forEach(value -> {
            TableItem item = new TableItem(valuesTable, SWT.NONE);
            item.setText(value);
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
        Set<String> selected = new LinkedHashSet<>();
        for (TableItem item : valuesTable.getItems()) if (item.getChecked()) selected.add(item.getText());
        if (fieldCombo.getSelectionIndex() < 0) { setErrorMessage("Choose a field."); return; }
        if (selected.isEmpty()) { setErrorMessage("Select at least one value."); return; }
        definition = new SlicerDefinition(fieldCombo.getText(), selected);
        super.okPressed();
    }

    SlicerDefinition definition() { return definition; }
}
