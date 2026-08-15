/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SortRule;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/** Ordered multi-field sort editor used by every visualization type. */
final class SortDialog extends TitleAreaDialog {
    private final ResultSetSnapshot snapshot;
    private final List<SortRule> current;
    private Table table;
    private Combo direction;
    private List<SortRule> rules = List.of();

    SortDialog(Shell shell, ResultSetSnapshot snapshot, List<SortRule> current) {
        super(shell);
        this.snapshot = snapshot;
        this.current = List.copyOf(current);
        setHelpAvailable(false);
    }

    @Override public void create() {
        super.create();
        setTitle("Sort Visualization");
        setMessage("Check fields in priority order, choose ASC or DESC, and move keys up or down.");
    }

    @Override protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite content = new Composite(area, SWT.NONE);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        content.setLayout(new GridLayout(2, false));
        table = new Table(content, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION | SWT.V_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableData.widthHint = 420;
        tableData.heightHint = 260;
        table.setLayoutData(tableData);
        TableColumn fieldColumn = new TableColumn(table, SWT.LEFT);
        fieldColumn.setText("Field");
        fieldColumn.setWidth(300);
        TableColumn directionColumn = new TableColumn(table, SWT.LEFT);
        directionColumn.setText("Direction");
        directionColumn.setWidth(90);
        populate();

        Composite actions = new Composite(content, SWT.NONE);
        actions.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        actions.setLayout(new GridLayout(1, false));
        direction = new Combo(actions, SWT.DROP_DOWN | SWT.READ_ONLY);
        direction.add("ASC");
        direction.add("DESC");
        direction.select(0);
        direction.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        direction.addListener(SWT.Selection, event -> setSelectedDirection());
        Button up = new Button(actions, SWT.PUSH);
        up.setText("Move Up");
        up.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        up.addListener(SWT.Selection, event -> move(-1));
        Button down = new Button(actions, SWT.PUSH);
        down.setText("Move Down");
        down.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        down.addListener(SWT.Selection, event -> move(1));
        table.addListener(SWT.Selection, event -> syncDirection());
        return area;
    }

    private void populate() {
        List<String> names = new ArrayList<>();
        current.forEach(rule -> names.add(rule.fieldName()));
        snapshot.columns().forEach(column -> {
            if (names.stream().noneMatch(name -> name.equalsIgnoreCase(column.displayName())))
                names.add(column.displayName());
        });
        for (String name : names) {
            SortRule existing = current.stream().filter(rule -> rule.fieldName().equalsIgnoreCase(name))
                    .findFirst().orElse(new SortRule(name, SortRule.Direction.ASC));
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(new String[] {name, existing.direction().toString()});
            item.setData(existing);
            item.setChecked(current.contains(existing));
        }
    }

    private void syncDirection() {
        int index = table.getSelectionIndex();
        if (index < 0) return;
        SortRule rule = (SortRule) table.getItem(index).getData();
        direction.select(rule.direction().ordinal());
    }

    private void setSelectedDirection() {
        int index = table.getSelectionIndex();
        if (index < 0 || direction.getSelectionIndex() < 0) return;
        TableItem item = table.getItem(index);
        SortRule rule = new SortRule(item.getText(0),
                SortRule.Direction.values()[direction.getSelectionIndex()]);
        item.setData(rule);
        item.setText(1, rule.direction().toString());
    }

    private void move(int delta) {
        int index = table.getSelectionIndex();
        int target = index + delta;
        if (index < 0 || target < 0 || target >= table.getItemCount()) return;
        ItemState selected = state(table.getItem(index));
        ItemState adjacent = state(table.getItem(target));
        restore(table.getItem(index), adjacent);
        restore(table.getItem(target), selected);
        table.setSelection(target);
        syncDirection();
    }

    private static ItemState state(TableItem item) {
        return new ItemState(item.getText(0), (SortRule) item.getData(), item.getChecked());
    }

    private static void restore(TableItem item, ItemState state) {
        item.setText(new String[] {state.name(), state.rule().direction().toString()});
        item.setData(state.rule());
        item.setChecked(state.checked());
    }

    @Override protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Apply Sort", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override protected void okPressed() {
        List<SortRule> selected = new ArrayList<>();
        for (TableItem item : table.getItems()) {
            if (item.getChecked()) selected.add((SortRule) item.getData());
        }
        rules = List.copyOf(selected);
        super.okPressed();
    }

    List<SortRule> rules() { return rules; }
    private record ItemState(String name, SortRule rule, boolean checked) {}
}
