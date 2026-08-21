/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.visualization.MatrixDisplayOptions;
import java.util.LinkedHashSet;
import java.util.Set;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/** Advanced Matrix controls kept behind one compact action. */
final class MatrixOptionsDialog extends TitleAreaDialog {
    private final MatrixDisplayOptions initial; private final int rowLevelCount;
    private Button rowTotals, columnTotals, subtotals, grandTotals, percentage, separators, dataBars;
    private Combo layout, conditional; private Text decimals, topN, width, subtotalLevels;
    private MatrixDisplayOptions options;
    MatrixOptionsDialog(Shell shell, MatrixDisplayOptions initial, int rowLevelCount) { super(shell); this.initial = initial; this.rowLevelCount = rowLevelCount; setHelpAvailable(false); }
    @Override public void create() { super.create(); setTitle("Matrix Options"); setMessage("Configure hierarchy layout, totals, formatting, and rendering limits."); }
    @Override protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent), form = new Composite(area, SWT.NONE); form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true)); form.setLayout(new GridLayout(2, false));
        layout = combo(form, "Layout:", MatrixDisplayOptions.Layout.values(), initial.layout().ordinal());
        rowTotals = check(form, "Row totals", initial.rowTotals()); columnTotals = check(form, "Column totals", initial.columnTotals());
        grandTotals = check(form, "Grand totals", initial.grandTotals()); subtotals = check(form, "Subtotals", initial.subtotals());
        subtotalLevels = text(form, "Subtotal levels (1-" + Math.max(1, rowLevelCount) + "):", initial.subtotalLevels().stream().sorted().map(value -> Integer.toString(value + 1)).reduce((a,b) -> a + "," + b).orElse(""));
        decimals = text(form, "Decimal places:", Integer.toString(initial.decimalPlaces())); percentage = check(form, "Percentage values", initial.percentage()); separators = check(form, "Thousands separators", initial.thousandsSeparator());
        conditional = combo(form, "Conditional formatting:", MatrixDisplayOptions.ConditionalFormat.values(), initial.conditionalFormat().ordinal()); dataBars = check(form, "Show data bars", initial.dataBars());
        topN = text(form, "Top N rows (0 = all):", Integer.toString(initial.topN())); width = text(form, "Value column width:", Integer.toString(initial.columnWidth())); return area;
    }
    private static Button check(Composite form, String label, boolean selected) { Button b = new Button(form, SWT.CHECK); b.setText(label); b.setSelection(selected); GridData d = new GridData(); d.horizontalSpan = 2; b.setLayoutData(d); return b; }
    private static Text text(Composite form, String label, String value) { new Label(form, SWT.NONE).setText(label); Text t = new Text(form, SWT.BORDER); t.setText(value); t.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false)); return t; }
    private static Combo combo(Composite form, String label, Object[] values, int selected) { new Label(form, SWT.NONE).setText(label); Combo c = new Combo(form, SWT.DROP_DOWN | SWT.READ_ONLY); for (Object value : values) c.add(value.toString().replace('_', ' ')); c.select(selected); return c; }
    @Override protected void createButtonsForButtonBar(Composite parent) { createButton(parent, IDialogConstants.OK_ID, "Apply", true); createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false); }
    @Override protected void okPressed() {
        try {
            Set<Integer> levels = new LinkedHashSet<>(); if (!subtotalLevels.getText().isBlank()) for (String value : subtotalLevels.getText().split(",")) { int level = Integer.parseInt(value.trim()) - 1; if (level < 0 || level >= rowLevelCount) throw new IllegalArgumentException(); levels.add(level); }
            options = new MatrixDisplayOptions(rowTotals.getSelection(), columnTotals.getSelection(), subtotals.getSelection(), grandTotals.getSelection(), MatrixDisplayOptions.Layout.values()[layout.getSelectionIndex()], Integer.parseInt(decimals.getText().trim()), percentage.getSelection(), separators.getSelection(), MatrixDisplayOptions.ConditionalFormat.values()[conditional.getSelectionIndex()], dataBars.getSelection(), Integer.parseInt(topN.getText().trim()), Integer.parseInt(width.getText().trim()), levels, initial.collapsedRowPaths());
        } catch (RuntimeException error) { setErrorMessage("Check levels, decimal places (0-8), Top N, and width (72-320)."); return; }
        super.okPressed();
    }
    MatrixDisplayOptions options() { return options; }
}
