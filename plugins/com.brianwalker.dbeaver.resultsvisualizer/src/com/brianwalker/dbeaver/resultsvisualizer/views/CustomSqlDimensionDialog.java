/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.services.CustomSqlDimension;
import com.brianwalker.dbeaver.resultsvisualizer.services.CalculatedFieldSqlTranslator;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/** General database SQL grouping-expression editor. */
final class CustomSqlDimensionDialog extends TitleAreaDialog {
    private Text nameText;
    private Text expressionText;
    private CustomSqlDimension dimension;
    private final CustomSqlDimension existing;
    private final ResultSetSnapshot snapshot;
    private final CalculatedFieldSqlTranslator translator;
    CustomSqlDimensionDialog(Shell shell, ResultSetSnapshot snapshot,
            CalculatedFieldSqlTranslator translator) { this(shell, snapshot, translator, null); }
    CustomSqlDimensionDialog(Shell shell, ResultSetSnapshot snapshot,
            CalculatedFieldSqlTranslator translator, CustomSqlDimension existing) {
        super(shell); this.snapshot = snapshot; this.translator = translator;
        this.existing = existing; setHelpAvailable(false);
    }

    @Override public void create() {
        super.create();
        setTitle(existing == null ? "New SQL Field" : "Edit SQL Field");
        setMessage("Name the field and enter the SQL expression your database should run.");
        if (existing != null) {
            nameText.setText(existing.name());
            expressionText.setText(existing.expression());
        }
        nameText.setFocus();
        ViewTheme.improveContrast((Composite) getDialogArea());
    }

    @Override protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite form = new Composite(area, SWT.NONE);
        form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        form.setLayout(new GridLayout(2, false));
        new Label(form, SWT.NONE).setText("Name:");
        nameText = new Text(form, SWT.BORDER);
        nameText.setMessage("data_dt");
        nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        new Label(form, SWT.NONE).setText("SQL expression:");
        expressionText = new Text(form, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData expressionData = new GridData(SWT.FILL, SWT.FILL, true, true);
        expressionData.widthHint = 380; expressionData.heightHint = 85;
        expressionText.setLayoutData(expressionData);
        expressionText.setMessage("DATE_TRUNC('month', date_time)");
        new Label(form, SWT.NONE).setText("Existing fields:");
        Table fields = new Table(form, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        GridData fieldsData = new GridData(SWT.FILL, SWT.FILL, true, true);
        fieldsData.heightHint = 95;
        fields.setLayoutData(fieldsData);
        fields.setToolTipText("Double-click to insert this source field or expanded formula SQL");
        snapshot.columns().forEach(column -> {
            TableItem item = new TableItem(fields, SWT.NONE);
            item.setText(column.displayName() + "  —  " + column.databaseTypeName());
            item.setData(translator.expressionFor(column.displayName()));
        });
        fields.addListener(SWT.DefaultSelection, event -> {
            int index = fields.getSelectionIndex();
            if (index < 0) return;
            String expression = (String) fields.getItem(index).getData();
            ViewTheme.insertPlainText(expressionText, expression);
        });
        new Label(form, SWT.NONE);
        Label help = new Label(form, SWT.WRAP);
        help.setText("Double-click a field to insert it. Enter a database expression only—omit "
                + "SELECT and AS. After saving, use the field under Available Fields or Aggregations.");
        help.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return area;
    }

    @Override protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, existing == null ? "Create" : "Save", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override protected void okPressed() {
        try {
            dimension = new CustomSqlDimension(nameText.getText(), expressionText.getText());
            super.okPressed();
        } catch (IllegalArgumentException error) { setErrorMessage(error.getMessage()); }
    }
    CustomSqlDimension dimension() { return dimension; }
}
