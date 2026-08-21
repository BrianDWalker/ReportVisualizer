/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/** Compact multi-measure selector for chart values. */
final class ValuesDialog extends TitleAreaDialog {
    private final ResultSetSnapshot snapshot;
    private final List<Integer> initiallySelected;
    private Table table;
    private List<Integer> selected = List.of();

    ValuesDialog(Shell shell, ResultSetSnapshot snapshot, Collection<Integer> initiallySelected) {
        super(shell);
        this.snapshot = snapshot;
        this.initiallySelected = List.copyOf(initiallySelected == null ? List.of() : initiallySelected);
        setHelpAvailable(false);
    }
    @Override public void create() { super.create(); setTitle("Chart Values"); setMessage("Select one or more measures. The selected aggregation applies to each."); }
    @Override protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        table = new Table(area, SWT.BORDER | SWT.CHECK | SWT.V_SCROLL);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true); data.widthHint = 360; data.heightHint = 240; table.setLayoutData(data);
        for (int index = 0; index < snapshot.columns().size(); index++) {
            TableItem item = new TableItem(table, SWT.NONE); item.setText(snapshot.columns().get(index).displayName()); item.setData(index); item.setChecked(initiallySelected.contains(index));
        }
        return area;
    }
    @Override protected void createButtonsForButtonBar(Composite parent) { createButton(parent, IDialogConstants.OK_ID, "Apply", true); createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false); }
    @Override protected void okPressed() {
        List<Integer> values = new ArrayList<>(); for (TableItem item : table.getItems()) if (item.getChecked()) values.add((Integer) item.getData());
        if (values.isEmpty()) { setErrorMessage("Select at least one value."); return; }
        selected = List.copyOf(values); super.okPressed();
    }
    List<Integer> selected() { return selected; }
}
