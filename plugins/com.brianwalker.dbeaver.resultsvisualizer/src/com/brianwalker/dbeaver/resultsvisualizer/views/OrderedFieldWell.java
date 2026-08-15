/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/** Inline ordered multi-field selector for Matrix rows and columns. */
final class OrderedFieldWell {
    private final Group root;
    private final Combo availableCombo;
    private final Table selectedTable;
    private final Consumer<List<DimensionChoice>> changeConsumer;
    private List<DimensionChoice> available = List.of();
    private List<DimensionChoice> selected = new ArrayList<>();

    OrderedFieldWell(Composite parent, String title, Consumer<List<DimensionChoice>> changeConsumer) {
        this.changeConsumer = changeConsumer;
        root = new Group(parent, SWT.NONE);
        root.setText(title);
        root.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        GridLayout rootLayout = new GridLayout(3, false);
        rootLayout.marginWidth = 4;
        rootLayout.marginHeight = 3;
        rootLayout.horizontalSpacing = 4;
        rootLayout.verticalSpacing = 2;
        root.setLayout(rootLayout);

        availableCombo = new Combo(root, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData comboData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        comboData.widthHint = 92;
        availableCombo.setLayoutData(comboData);
        Button add = new Button(root, SWT.PUSH);
        add.setText("+");
        add.setToolTipText("Add selected field");
        add.setLayoutData(buttonWidth(28));
        add.addListener(SWT.Selection, event -> addSelected());
        Button remove = new Button(root, SWT.PUSH);
        remove.setText("−");
        remove.setToolTipText("Remove selected field");
        remove.setLayoutData(buttonWidth(28));
        remove.addListener(SWT.Selection, event -> removeSelected());

        selectedTable = new Table(root, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, false);
        tableData.horizontalSpan = 2;
        tableData.heightHint = 46;
        tableData.widthHint = 124;
        selectedTable.setLayoutData(tableData);
        selectedTable.addListener(SWT.DefaultSelection, event -> removeSelected());

        Composite order = new Composite(root, SWT.NONE);
        order.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        GridLayout orderLayout = new GridLayout(1, false);
        orderLayout.marginWidth = 0;
        orderLayout.marginHeight = 0;
        orderLayout.verticalSpacing = 1;
        order.setLayout(orderLayout);
        button(order, "▲", () -> move(-1));
        button(order, "▼", () -> move(1));
    }

    private static void button(Composite parent, String text, Runnable action) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(text);
        GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.widthHint = 34;
        button.setLayoutData(data);
        button.addListener(SWT.Selection, event -> action.run());
    }

    private static GridData buttonWidth(int width) {
        GridData data = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        data.widthHint = width;
        return data;
    }

    Composite control() { return root; }

    void setChoices(List<DimensionChoice> available, List<DimensionChoice> selected) {
        this.available = List.copyOf(available);
        this.selected = new ArrayList<>(selected);
        refresh();
    }

    private void refresh() {
        availableCombo.removeAll();
        available.stream().filter(choice -> selected.stream().noneMatch(value -> same(value, choice)))
                .forEach(choice -> availableCombo.add(choice.displayName()));
        if (availableCombo.getItemCount() > 0) availableCombo.select(0);
        selectedTable.removeAll();
        for (int index = 0; index < selected.size(); index++) {
            TableItem item = new TableItem(selectedTable, SWT.NONE);
            item.setText((index + 1) + ".  " + selected.get(index).displayName());
        }
    }

    private void addSelected() {
        int comboIndex = availableCombo.getSelectionIndex();
        if (comboIndex < 0) return;
        String name = availableCombo.getItem(comboIndex);
        available.stream().filter(choice -> choice.displayName().equals(name))
                .findFirst().ifPresent(selected::add);
        refresh();
        if (!selected.isEmpty()) selectedTable.setSelection(selected.size() - 1);
        changed();
    }

    private void removeSelected() {
        int index = selectedTable.getSelectionIndex();
        if (index < 0) return;
        selected.remove(index);
        refresh();
        if (!selected.isEmpty()) selectedTable.setSelection(Math.min(index, selected.size() - 1));
        changed();
    }

    private void move(int delta) {
        int index = selectedTable.getSelectionIndex();
        int target = index + delta;
        if (index < 0 || target < 0 || target >= selected.size()) return;
        DimensionChoice value = selected.remove(index);
        selected.add(target, value);
        refresh();
        selectedTable.setSelection(target);
        changed();
    }

    private void changed() { changeConsumer.accept(List.copyOf(selected)); }

    private static boolean same(DimensionChoice left, DimensionChoice right) {
        return left.displayName().equalsIgnoreCase(right.displayName())
                && left.isCustom() == right.isCustom();
    }
}
