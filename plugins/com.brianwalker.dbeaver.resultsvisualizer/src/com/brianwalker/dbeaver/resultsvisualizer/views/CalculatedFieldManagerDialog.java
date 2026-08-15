/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldService;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/** Create, edit, and remove multiple local formulas as one committed change. */
final class CalculatedFieldManagerDialog extends TitleAreaDialog {
    private final ResultSetSnapshot snapshot;
    private final CalculatedFieldService service;
    private final List<CalculatedFieldDefinition> working;
    private Table table;

    CalculatedFieldManagerDialog(Shell shell, ResultSetSnapshot snapshot,
            CalculatedFieldService service, List<CalculatedFieldDefinition> definitions) {
        super(shell);
        this.snapshot = snapshot;
        this.service = service;
        this.working = new ArrayList<>(definitions);
        setHelpAvailable(false);
    }

    @Override public void create() {
        super.create();
        setTitle("Manage Formulas");
        setMessage("Create multiple local formulas, or select one to edit or delete.");
        ViewTheme.improveContrast((Composite) getDialogArea());
    }

    @Override protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite content = new Composite(area, SWT.NONE);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        content.setLayout(new GridLayout(2, false));
        table = new Table(content, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE | SWT.V_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.widthHint = 560;
        data.heightHint = 240;
        table.setLayoutData(data);
        TableColumn name = new TableColumn(table, SWT.LEFT);
        name.setText("Name"); name.setWidth(180);
        TableColumn expression = new TableColumn(table, SWT.LEFT);
        expression.setText("Expression"); expression.setWidth(360);

        Composite actions = new Composite(content, SWT.NONE);
        actions.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        actions.setLayout(new GridLayout(1, false));
        button(actions, "Add…", this::add);
        button(actions, "Edit…", this::edit);
        button(actions, "Delete", this::delete);
        table.addListener(SWT.DefaultSelection, event -> edit());
        refresh();
        return area;
    }

    private static void button(Composite parent, String text, Runnable action) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(text);
        button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        button.addListener(SWT.Selection, event -> action.run());
    }

    private void add() {
        CalculatedFieldDialog dialog = new CalculatedFieldDialog(getShell(), snapshot, service);
        if (dialog.open() != Window.OK || dialog.definition() == null) return;
        if (duplicate(dialog.definition().name(), -1)) return;
        working.add(dialog.definition());
        refresh();
        table.setSelection(table.getItemCount() - 1);
    }

    private void edit() {
        int index = table.getSelectionIndex();
        if (index < 0) return;
        CalculatedFieldDialog dialog = new CalculatedFieldDialog(
                getShell(), snapshot, service, working.get(index));
        if (dialog.open() != Window.OK || dialog.definition() == null) return;
        if (duplicate(dialog.definition().name(), index)) return;
        working.set(index, dialog.definition());
        refresh();
        table.setSelection(index);
    }

    private boolean duplicate(String name, int except) {
        for (int index = 0; index < working.size(); index++) {
            if (index != except && working.get(index).name().equalsIgnoreCase(name)) {
                MessageDialog.openError(getShell(), "Duplicate Formula", "Formula names must be unique.");
                return true;
            }
        }
        return false;
    }

    private void delete() {
        int index = table.getSelectionIndex();
        if (index < 0) return;
        working.remove(index);
        refresh();
        if (!working.isEmpty()) table.setSelection(Math.min(index, working.size() - 1));
    }

    private void refresh() {
        table.removeAll();
        for (CalculatedFieldDefinition definition : working) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(new String[] {definition.name(), definition.expression()});
        }
    }

    @Override protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Apply", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    List<CalculatedFieldDefinition> definitions() { return List.copyOf(working); }
}
