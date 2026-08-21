/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.Aggregation;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartDataBuilder;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationConfiguration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

/** Sets an independent local aggregation for each selected chart measure. */
final class ValueAggregationsDialog extends TitleAreaDialog {
    private final ResultSetSnapshot snapshot;
    private final VisualizationConfiguration configuration;
    private final Map<Integer, Combo> controls = new LinkedHashMap<>();
    private Map<Integer, Aggregation> result = Map.of();

    ValueAggregationsDialog(Shell shell, ResultSetSnapshot snapshot,
            VisualizationConfiguration configuration) {
        super(shell);
        this.snapshot = snapshot;
        this.configuration = configuration;
        setHelpAvailable(false);
    }

    @Override public void create() {
        super.create();
        setTitle("Value Aggregations");
        setMessage("Choose how each selected measure is grouped locally.");
    }

    @Override protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite form = new Composite(area, SWT.NONE);
        form.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 10;
        layout.verticalSpacing = 7;
        form.setLayout(layout);
        for (int index : configuration.valueColumnIndexes()) {
            Label name = new Label(form, SWT.NONE);
            name.setText(snapshot.columns().get(index).displayName() + ":");
            Combo aggregation = new Combo(form, SWT.DROP_DOWN | SWT.READ_ONLY);
            List<Aggregation> choices = Aggregation.compatibleWith(snapshot.columns().get(index).normalizedType());
            choices.forEach(value -> aggregation.add(value.toString()));
            int selected = choices.indexOf(configuration.aggregationFor(index));
            aggregation.select(selected < 0 ? 0 : selected);
            aggregation.setData(choices);
            aggregation.setLayoutData(new GridData(150, SWT.DEFAULT));
            controls.put(index, aggregation);
        }
        return area;
    }

    @Override protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Apply", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override protected void okPressed() {
        Map<Integer, Aggregation> selected = new LinkedHashMap<>();
        for (Map.Entry<Integer, Combo> entry : controls.entrySet()) {
            @SuppressWarnings("unchecked")
            List<Aggregation> choices = (List<Aggregation>) entry.getValue().getData();
            int index = entry.getValue().getSelectionIndex();
            if (index >= 0 && index < choices.size()) selected.put(entry.getKey(), choices.get(index));
        }
        result = Map.copyOf(selected);
        super.okPressed();
    }

    Map<Integer, Aggregation> aggregations() { return result; }
}
