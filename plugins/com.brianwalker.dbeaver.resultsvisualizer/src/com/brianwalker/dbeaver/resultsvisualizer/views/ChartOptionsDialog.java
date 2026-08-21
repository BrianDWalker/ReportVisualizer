/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartDisplayOptions;
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
import org.eclipse.swt.widgets.Text;

/** Advanced chart presentation options kept out of the permanent builder. */
final class ChartOptionsDialog extends TitleAreaDialog {
    private final ChartDisplayOptions initial;
    private Button labels, markers, secondaryAxis;
    private Combo legend, pieLabels;
    private Text topN;
    private ChartDisplayOptions options;

    ChartOptionsDialog(Shell shell, ChartDisplayOptions initial) { super(shell); this.initial = initial; setHelpAvailable(false); }
    @Override public void create() { super.create(); setTitle("Chart Options"); setMessage("Presentation choices apply to the current chart and saved presets."); }
    @Override protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent); Composite form = new Composite(area, SWT.NONE);
        form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true)); form.setLayout(new GridLayout(2, false));
        labels = new Button(form, SWT.CHECK); labels.setText("Show data labels"); labels.setSelection(initial.dataLabels()); GridData span = new GridData(SWT.LEFT, SWT.CENTER, false, false); span.horizontalSpan = 2; labels.setLayoutData(span);
        markers = new Button(form, SWT.CHECK); markers.setText("Show line markers"); markers.setSelection(initial.markers()); GridData markerSpan = new GridData(SWT.LEFT, SWT.CENTER, false, false); markerSpan.horizontalSpan = 2; markers.setLayoutData(markerSpan);
        secondaryAxis = new Button(form, SWT.CHECK); secondaryAxis.setText("Use secondary Y-axis for combo line series"); secondaryAxis.setSelection(initial.secondaryAxis()); GridData axisSpan = new GridData(SWT.LEFT, SWT.CENTER, false, false); axisSpan.horizontalSpan = 2; secondaryAxis.setLayoutData(axisSpan);
        new org.eclipse.swt.widgets.Label(form, SWT.NONE).setText("Legend:"); legend = new Combo(form, SWT.DROP_DOWN | SWT.READ_ONLY); for (ChartDisplayOptions.LegendPosition value : ChartDisplayOptions.LegendPosition.values()) legend.add(value.name()); legend.select(initial.legendPosition().ordinal());
        new org.eclipse.swt.widgets.Label(form, SWT.NONE).setText("Pie labels:"); pieLabels = new Combo(form, SWT.DROP_DOWN | SWT.READ_ONLY); for (ChartDisplayOptions.PieLabelMode value : ChartDisplayOptions.PieLabelMode.values()) pieLabels.add(value.name().replace('_', ' ')); pieLabels.select(initial.pieLabelMode().ordinal());
        new org.eclipse.swt.widgets.Label(form, SWT.NONE).setText("Pie Top N (0 = all):"); topN = new Text(form, SWT.BORDER); topN.setText(Integer.toString(initial.topN())); topN.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return area;
    }
    @Override protected void createButtonsForButtonBar(Composite parent) { createButton(parent, IDialogConstants.OK_ID, "Apply", true); createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false); }
    @Override protected void okPressed() {
        try { options = new ChartDisplayOptions(labels.getSelection(), markers.getSelection(), secondaryAxis.getSelection(), ChartDisplayOptions.LegendPosition.values()[legend.getSelectionIndex()], ChartDisplayOptions.PieLabelMode.values()[pieLabels.getSelectionIndex()], Integer.parseInt(topN.getText().trim())); }
        catch (RuntimeException error) { setErrorMessage("Top N must be a whole number from 0 to 100."); return; }
        super.okPressed();
    }
    ChartDisplayOptions options() { return options; }
}
