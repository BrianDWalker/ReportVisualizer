/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/** Checkbox selector for multi-level Matrix/Pivot rows or columns. */
final class MultiDimensionDialog extends TitleAreaDialog {
    private final String roleName;
    private final List<DimensionChoice> available;
    private final List<DimensionChoice> current;
    private Table table;
    private List<DimensionChoice> selected = List.of();

    MultiDimensionDialog(Shell shell, String roleName,
            List<DimensionChoice> available, List<DimensionChoice> current) {
        super(shell);
        this.roleName = roleName;
        this.available = List.copyOf(available);
        this.current = List.copyOf(current);
        setHelpAvailable(false);
    }

    @Override public void create() {
        super.create();
        setTitle("Configure Matrix " + roleName);
        setMessage("Select one or more fields. Their order here defines the pivot hierarchy.");
    }

    @Override protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        table = new Table(area, SWT.BORDER | SWT.CHECK | SWT.V_SCROLL);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.widthHint = 430;
        data.heightHint = 280;
        table.setLayoutData(data);
        for (DimensionChoice choice : available) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(choice.displayName());
            item.setData(choice);
            item.setChecked(current.stream().anyMatch(value -> same(value, choice)));
        }
        return area;
    }

    @Override protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Apply " + roleName, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override protected void okPressed() {
        List<DimensionChoice> choices = new ArrayList<>();
        for (TableItem item : table.getItems()) if (item.getChecked()) choices.add((DimensionChoice) item.getData());
        if (choices.isEmpty()) {
            setErrorMessage("Select at least one " + roleName.toLowerCase() + " field.");
            return;
        }
        selected = List.copyOf(choices);
        super.okPressed();
    }

    List<DimensionChoice> selected() { return selected; }

    static boolean same(DimensionChoice left, DimensionChoice right) {
        return left.displayName().equalsIgnoreCase(right.displayName())
                && left.isCustom() == right.isCustom();
    }
}
