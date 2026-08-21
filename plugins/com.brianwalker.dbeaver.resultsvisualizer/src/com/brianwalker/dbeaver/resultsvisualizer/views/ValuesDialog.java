/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.Aggregation;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationConfiguration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/** Selects chart measures and their independent aggregations in one compact dialog. */
final class ValuesDialog extends TitleAreaDialog {
    private final ResultSetSnapshot snapshot;
    private final VisualizationConfiguration configuration;
    private final List<Button> checks = new ArrayList<>();
    private final List<Combo> aggregationCombos = new ArrayList<>();
    private List<Integer> selected = List.of();
    private Map<Integer, Aggregation> aggregations = Map.of();

    ValuesDialog(Shell shell, ResultSetSnapshot snapshot, VisualizationConfiguration configuration) {
        super(shell);
        this.snapshot = snapshot;
        this.configuration = configuration;
        setHelpAvailable(false);
    }
    @Override public void create() { super.create(); setTitle("Chart Values"); setMessage("Select one or more measures and choose the aggregation for each one."); }
    @Override protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        ScrolledComposite scroller = new ScrolledComposite(area, SWT.BORDER | SWT.V_SCROLL);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true); data.widthHint = 360; data.heightHint = 180; scroller.setLayoutData(data);
        scroller.setExpandHorizontal(true);
        Composite rows = new Composite(scroller, SWT.NONE);
        GridLayout layout = new GridLayout(2, false); layout.marginWidth = 0; layout.marginHeight = 0; layout.verticalSpacing = 4; rows.setLayout(layout);
        for (int index = 0; index < snapshot.columns().size(); index++) {
            Button check = new Button(rows, SWT.CHECK); check.setText(snapshot.columns().get(index).displayName()); check.setData(index);
            check.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false)); check.setSelection(configuration.valueColumnIndexes().contains(index));
            Combo aggregation = new Combo(rows, SWT.DROP_DOWN | SWT.READ_ONLY); aggregation.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
            List<Aggregation> compatible = Aggregation.compatibleWith(snapshot.columns().get(index).normalizedType());
            for (Aggregation candidate : compatible) aggregation.add(candidate.toString());
            int selectedIndex = compatible.indexOf(configuration.aggregationFor(index)); aggregation.select(selectedIndex < 0 ? 0 : selectedIndex);
            aggregation.setEnabled(check.getSelection()); check.addListener(SWT.Selection, event -> aggregation.setEnabled(check.getSelection()));
            checks.add(check); aggregationCombos.add(aggregation);
        }
        scroller.setContent(rows);
        scroller.setMinSize(rows.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        return area;
    }
    @Override protected void createButtonsForButtonBar(Composite parent) { createButton(parent, IDialogConstants.OK_ID, "Apply", true); createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false); }
    @Override protected void okPressed() {
        List<Integer> values = new ArrayList<>(); Map<Integer, Aggregation> choices = new LinkedHashMap<>();
        for (int index = 0; index < checks.size(); index++) {
            Button check = checks.get(index); if (!check.getSelection()) continue;
            int columnIndex = (Integer) check.getData(); values.add(columnIndex);
            List<Aggregation> compatible = Aggregation.compatibleWith(snapshot.columns().get(columnIndex).normalizedType());
            choices.put(columnIndex, compatible.get(aggregationCombos.get(index).getSelectionIndex()));
        }
        if (values.isEmpty()) { setErrorMessage("Select at least one value."); return; }
        selected = List.copyOf(values); aggregations = Map.copyOf(choices); super.okPressed();
    }
    List<Integer> selected() { return selected; }
    Map<Integer, Aggregation> aggregations() { return aggregations; }
}
