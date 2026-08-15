/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldException;
import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldService;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/** Modal editor for a validated restricted calculated-field expression. */
final class CalculatedFieldDialog extends TitleAreaDialog {
    private final ResultSetSnapshot snapshot;
    private final CalculatedFieldService service;
    private final CalculatedFieldDefinition existing;
    private Text nameText;
    private Text expressionText;
    private CalculatedFieldDefinition definition;

    CalculatedFieldDialog(Shell parentShell, ResultSetSnapshot snapshot,
            CalculatedFieldService service) {
        this(parentShell, snapshot, service, null);
    }

    CalculatedFieldDialog(Shell parentShell, ResultSetSnapshot snapshot,
            CalculatedFieldService service, CalculatedFieldDefinition existing) {
        super(parentShell);
        this.snapshot = snapshot;
        this.service = service;
        this.existing = existing;
        setHelpAvailable(false);
    }

    @Override
    public void create() {
        super.create();
        setTitle(existing == null ? "New Formula" : "Edit Formula");
        setMessage("Build a local numeric formula. Double-click a field below to insert it.");
        if (existing != null) {
            nameText.setText(existing.name());
            expressionText.setText(existing.expression());
        }
        nameText.setFocus();
        ViewTheme.improveContrast((Composite) getDialogArea());
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite form = new Composite(area, SWT.NONE);
        form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        form.setLayout(new GridLayout(2, false));

        new Label(form, SWT.NONE).setText("Name:");
        nameText = new Text(form, SWT.BORDER);
        nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        nameText.setMessage("Profit");

        new Label(form, SWT.NONE).setText("Expression:");
        expressionText = new Text(form, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData expressionData = new GridData(SWT.FILL, SWT.FILL, true, true);
        expressionData.heightHint = 80;
        expressionData.widthHint = 430;
        expressionText.setLayoutData(expressionData);
        expressionText.setMessage("[revenue] - [cost]");

        new Label(form, SWT.NONE).setText("Fields:");
        Table fields = new Table(form, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        GridData fieldsData = new GridData(SWT.FILL, SWT.FILL, true, true);
        fieldsData.heightHint = 100;
        fields.setLayoutData(fieldsData);
        fields.setToolTipText("Double-click a field to insert it into the formula");
        snapshot.columns().forEach(column -> {
            TableItem item = new TableItem(fields, SWT.NONE);
            item.setText(column.displayName() + "  —  " + column.databaseTypeName());
            item.setData("[" + column.displayName() + "]");
        });
        fields.addListener(SWT.DefaultSelection, event -> {
            int index = fields.getSelectionIndex();
            if (index < 0) return;
            String reference = (String) fields.getItem(index).getData();
            ViewTheme.insertPlainText(expressionText, reference);
        });

        new Label(form, SWT.NONE).setText("Formula guide:");
        Label supported = new Label(form, SWT.WRAP);
        supported.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        supported.setText("Put field names in square brackets. Use decimal numbers and +  −  *  /  ( ).\n"
                + "Functions: ABS(value), ROUND(value), ROUND(value, decimals), CEIL(value), "
                + "FLOOR(value), SQRT(value), POWER(value, exponent), MIN(a, b), MAX(a, b).\n"
                + "Examples:\n"
                + "  Profit: [revenue] - [cost]\n"
                + "  Margin %: ROUND(([revenue] - [cost]) * 100 / [revenue], 2)\n"
                + "  Capped amount: MIN([amount], 1000)\n"
                + "Null, non-numeric, invalid square-root, and divide-by-zero results stay blank.");
        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, existing == null ? "Create" : "Save", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        try {
            CalculatedFieldDefinition candidate =
                    new CalculatedFieldDefinition(nameText.getText(), expressionText.getText());
            service.validate(snapshot, candidate);
            definition = candidate;
            setErrorMessage(null);
            super.okPressed();
        } catch (IllegalArgumentException | CalculatedFieldException error) {
            setErrorMessage(error.getMessage());
        }
    }

    CalculatedFieldDefinition definition() {
        return definition;
    }
}
