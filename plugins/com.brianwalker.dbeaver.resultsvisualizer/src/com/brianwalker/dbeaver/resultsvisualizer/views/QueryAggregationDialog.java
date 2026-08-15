/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.services.QueryAggregation;
import com.brianwalker.dbeaver.resultsvisualizer.services.QueryMeasure;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.Aggregation;
import java.util.List;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** Edits one output aggregation in the source-query builder. */
final class QueryAggregationDialog extends TitleAreaDialog {
    private final List<QueryMeasure> measures;
    private final QueryAggregation existing;
    private Combo fieldCombo;
    private Combo aggregationCombo;
    private Text aliasText;
    private QueryAggregation result;
    private boolean generatedAlias = true;

    QueryAggregationDialog(Shell shell, List<QueryMeasure> measures, QueryAggregation existing) {
        super(shell);
        this.measures = List.copyOf(measures);
        this.existing = existing;
        setHelpAvailable(false);
    }

    @Override public void create() {
        super.create();
        setTitle(existing == null ? "Add Aggregation" : "Edit Aggregation");
        setMessage("Choose a field, aggregation, and output name for the generated SQL.");
        if (existing != null) {
            selectField(existing.measure().alias());
            aggregationCombo.select(existing.aggregation().ordinal());
            aliasText.setText(existing.alias());
            generatedAlias = false;
        } else if (!measures.isEmpty()) {
            fieldCombo.select(0);
            aggregationCombo.select(Aggregation.SUM.ordinal());
            updateGeneratedAlias();
        }
        ViewTheme.improveContrast((Composite) getDialogArea());
    }

    @Override protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite form = new Composite(area, SWT.NONE);
        form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        form.setLayout(new GridLayout(2, false));
        new Label(form, SWT.NONE).setText("Field:");
        fieldCombo = new Combo(form, SWT.DROP_DOWN | SWT.READ_ONLY);
        fieldCombo.setLayoutData(width(260));
        measures.forEach(measure -> fieldCombo.add(measure.alias()));
        new Label(form, SWT.NONE).setText("Aggregation:");
        aggregationCombo = new Combo(form, SWT.DROP_DOWN | SWT.READ_ONLY);
        aggregationCombo.setLayoutData(width(180));
        for (Aggregation aggregation : Aggregation.values()) aggregationCombo.add(aggregation.toString());
        new Label(form, SWT.NONE).setText("Output name:");
        aliasText = new Text(form, SWT.BORDER);
        aliasText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        aliasText.setMessage("sum_revenue");
        fieldCombo.addListener(SWT.Selection, event -> updateGeneratedAlias());
        aggregationCombo.addListener(SWT.Selection, event -> updateGeneratedAlias());
        aliasText.addListener(SWT.Modify, event -> {
            if (aliasText.isFocusControl()) generatedAlias = false;
        });
        return area;
    }

    private void selectField(String name) {
        for (int index = 0; index < measures.size(); index++) {
            if (measures.get(index).alias().equalsIgnoreCase(name)) { fieldCombo.select(index); return; }
        }
    }

    private void updateGeneratedAlias() {
        if (!generatedAlias || fieldCombo.getSelectionIndex() < 0 || aggregationCombo.getSelectionIndex() < 0) return;
        String field = measures.get(fieldCombo.getSelectionIndex()).alias().replaceAll("[^A-Za-z0-9_]+", "_");
        aliasText.setText(Aggregation.values()[aggregationCombo.getSelectionIndex()].name().toLowerCase() + "_" + field);
    }

    private static GridData width(int width) {
        GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        data.widthHint = width;
        return data;
    }

    @Override protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, existing == null ? "Add" : "Save", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override protected void okPressed() {
        try {
            if (fieldCombo.getSelectionIndex() < 0) throw new IllegalArgumentException("Choose a field.");
            if (aggregationCombo.getSelectionIndex() < 0) throw new IllegalArgumentException("Choose an aggregation.");
            result = new QueryAggregation(aliasText.getText(), measures.get(fieldCombo.getSelectionIndex()),
                    Aggregation.values()[aggregationCombo.getSelectionIndex()]);
            super.okPressed();
        } catch (IllegalArgumentException error) {
            setErrorMessage(error.getMessage());
        }
    }

    QueryAggregation aggregation() { return result; }
}
