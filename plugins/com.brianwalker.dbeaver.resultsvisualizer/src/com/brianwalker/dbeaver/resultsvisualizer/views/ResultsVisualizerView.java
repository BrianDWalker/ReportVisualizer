/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldProjection;
import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldService;
import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetUpdate;
import com.brianwalker.dbeaver.resultsvisualizer.model.VisualizerPreset;
import com.brianwalker.dbeaver.resultsvisualizer.model.VisualizerPresetStore;
import com.brianwalker.dbeaver.resultsvisualizer.model.VisualizerSession;
import com.brianwalker.dbeaver.resultsvisualizer.model.VisualizerSessionManager;
import com.brianwalker.dbeaver.resultsvisualizer.services.DBeaverSqlDialectService;
import com.brianwalker.dbeaver.resultsvisualizer.services.ResultSetService;
import com.brianwalker.dbeaver.resultsvisualizer.services.ResultSetServices;
import com.brianwalker.dbeaver.resultsvisualizer.services.ResultSource;
import com.brianwalker.dbeaver.resultsvisualizer.services.AggregateQueryBuilder;
import com.brianwalker.dbeaver.resultsvisualizer.services.AggregateQuery;
import com.brianwalker.dbeaver.resultsvisualizer.services.CustomSqlDimension;
import com.brianwalker.dbeaver.resultsvisualizer.services.QueryDimension;
import com.brianwalker.dbeaver.resultsvisualizer.services.QueryMeasure;
import com.brianwalker.dbeaver.resultsvisualizer.services.CalculatedFieldSqlTranslator;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.Aggregation;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartCanvas;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartDataBuilder;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartRendererRegistry;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartType;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationConfiguration;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SnapshotSlicer;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SnapshotSorter;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SortRule;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.MatrixDisplayOptions;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationExportService;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationExportService.ExportFormat;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.part.ViewPart;

/** Dockable interactive visualization builder driven by the active DBeaver result set. */
public final class ResultsVisualizerView extends ViewPart {

    public static final String ID =
            "com.brianwalker.dbeaver.resultsvisualizer.views.resultsVisualizer";

    private enum FieldRole { X, VALUE, SERIES }

    private final ChartRendererRegistry rendererRegistry = ChartRendererRegistry.defaults();
    private final List<ChartType> chartTypes = rendererRegistry.availableTypes();
    private final CalculatedFieldService calculatedFieldService = new CalculatedFieldService();
    private final VisualizerSessionManager visualizerSessionManager = new VisualizerSessionManager();
    private final VisualizerPresetStore presetStore = new VisualizerPresetStore();
    private final List<CalculatedFieldDefinition> calculatedFields = new ArrayList<>();
    private final List<SlicerDefinition> slicers = new ArrayList<>();
    private final List<CustomSqlDimension> customSqlDimensions = new ArrayList<>();
    private List<SortRule> sortRules = new ArrayList<>();

    private Composite content;
    private Label summaryLabel;
    private Combo sourceCombo;
    private Label messageLabel;
    private Label rowLimitWarning;
    private Group body;
    private ScrolledComposite configurationScroller;
    private Group configurationGroup;
    private Button addCalculatedFieldButton;
    private Label calculatedFieldStatus;
    private Button slicersButton;
    private Button sourceQueryButton;
    private Button sortButton;
    private Button backToOriginalButton;
    private ChartCanvas chartCanvas;
    private Combo chartTypeCombo;
    private Combo aggregationCombo;
    private Combo xWell;
    private Combo valueWell;
    private Combo seriesWell;
    private Combo yMaximumCombo;
    private Label xWellLabel;
    private Label seriesWellLabel;
    private Composite matrixConfiguration;
    private OrderedFieldWell matrixRowsWell;
    private OrderedFieldWell matrixColumnsWell;
    private OrderedFieldWell matrixValuesWell;
    private Button rowTotalsButton;
    private Button columnTotalsButton;
    private Button subtotalsButton;
    private List<DimensionChoice> xWellChoices = List.of();
    private List<DimensionChoice> seriesWellChoices = List.of();
    private List<Integer> valueWellIndexes = List.of();
    private List<Aggregation> availableAggregations = List.of(Aggregation.values());
    private List<DimensionChoice> matrixRows = new ArrayList<>();
    private List<DimensionChoice> matrixColumns = new ArrayList<>();
    private List<DimensionChoice> matrixValues = new ArrayList<>();
    private DimensionChoice activeXChoice;
    private DimensionChoice activeSeriesChoice;
    private MatrixDisplayOptions matrixOptions = MatrixDisplayOptions.DEFAULT;
    private AggregateQuery pendingAggregateQuery;
    private ResultSetSnapshot snapshot;
    private ResultSetSnapshot baseSnapshot;
    private ResultSetSnapshot aggregateSnapshot;
    private VisualizationConfiguration configuration;
    private boolean configurationInitialized;
    private ResultSetService resultSetService;
    private String activeSessionIdentity = "";
    private VisualizerSession.DisplayMode displayMode = VisualizerSession.DisplayMode.SOURCE;

    @Override
    public void createPartControl(Composite parent) {
        content = new Composite(parent, SWT.NONE);
        content.setLayout(new GridLayout(1, false));
        createHeader(content);

        messageLabel = new Label(content, SWT.WRAP);
        messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        messageLabel.setText("No active result set available.");

        rowLimitWarning = new Label(content, SWT.WRAP);
        rowLimitWarning.setText(formatRowLimitWarning());
        rowLimitWarning.setToolTipText(formatRowLimitTooltip(0));
        rowLimitWarning.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        setVisible(rowLimitWarning, false);

        createChartPanel(content);

        createConfigurationPanel(content);
        setVisible(body, false);
        setVisible(configurationScroller, false);

        resultSetService = ResultSetServices.create(getSite().getPage(), parent.getDisplay());
        resultSetService.start(this::applyUpdate);
    }

    private void createHeader(Composite parent) {
        Composite header = new Composite(parent, SWT.NONE);
        header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        GridLayout layout = new GridLayout(5, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        header.setLayout(layout);

        summaryLabel = new Label(header, SWT.NONE);
        summaryLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        summaryLabel.setText("Results Visualizer");
        new Label(header, SWT.NONE).setText("Source:");
        sourceCombo = new Combo(header, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (ResultSource source : ResultSource.values()) sourceCombo.add(source.displayName());
        sourceCombo.select(ResultSource.RESULTS.ordinal());
        sourceCombo.setToolTipText("Visualize the normal results or DBeaver Grouping panel output");
        sourceCombo.addListener(SWT.Selection, event -> {
            if (resultSetService != null && sourceCombo.getSelectionIndex() >= 0) {
                resultSetService.setSource(ResultSource.values()[sourceCombo.getSelectionIndex()]);
            }
        });
        Button refresh = new Button(header, SWT.PUSH);
        refresh.setText("Refresh");
        refresh.setToolTipText("Read the active result set again");
        refresh.addListener(SWT.Selection, event -> resultSetService.refresh());
        backToOriginalButton = new Button(header, SWT.PUSH);
        backToOriginalButton.setText("Back to Original");
        backToOriginalButton.setVisible(false);
        backToOriginalButton.addListener(SWT.Selection, event -> switchToSourceView());
    }

    private void createChartPanel(Composite parent) {
        body = new Group(parent, SWT.NONE);
        body.setText("Visualization");
        body.setLayout(new GridLayout(1, false));
        body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite chartHeader = new Composite(body, SWT.NONE);
        chartHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        GridLayout chartHeaderLayout = new GridLayout(2, false);
        chartHeaderLayout.marginWidth = 0;
        chartHeaderLayout.marginHeight = 0;
        chartHeader.setLayout(chartHeaderLayout);
        Label chartHeaderSpacer = new Label(chartHeader, SWT.NONE);
        chartHeaderSpacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        Button exportButton = new Button(chartHeader, SWT.PUSH);
        exportButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        exportButton.setText("Export ▾");
        exportButton.setToolTipText("Save or copy the current chart as an image or document");
        exportButton.addListener(SWT.Selection, event -> showDropdownMenu(exportButton, menu -> {
            addMenuItem(menu, "Copy Image", "Copy the current chart to the system clipboard",
                    this::copyChartToClipboard);
            addMenuItem(menu, "Save PNG…", "Export the current chart as a PNG file",
                    () -> exportChartToFile(ExportFormat.PNG));
            addMenuItem(menu, "Save JPEG…", "Export the current chart as a JPEG file",
                    () -> exportChartToFile(ExportFormat.JPEG));
            addMenuItem(menu, "Save SVG…", "Export the current chart as a vector SVG file",
                    () -> exportChartToFile(ExportFormat.SVG));
            addMenuItem(menu, "Save PDF…", "Export the current chart as a PDF document",
                    () -> exportChartToFile(ExportFormat.PDF));
        }));

        chartCanvas = new ChartCanvas(body, SWT.BORDER, rendererRegistry);
        chartCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    private void createConfigurationPanel(Composite parent) {
        configurationScroller = new ScrolledComposite(parent, SWT.V_SCROLL);
        GridData scrollerData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        configurationScroller.setLayoutData(scrollerData);
        configurationScroller.setExpandHorizontal(true);
        configurationScroller.setExpandVertical(true);
        configurationGroup = new Group(configurationScroller, SWT.NONE);
        configurationGroup.setText("Visualization Builder");
        GridLayout builderLayout = new GridLayout(1, false);
        builderLayout.marginWidth = 6;
        builderLayout.marginHeight = 4;
        builderLayout.verticalSpacing = 3;
        configurationGroup.setLayout(builderLayout);
        configurationGroup.setBackgroundMode(SWT.INHERIT_FORCE);
        configurationScroller.setContent(configurationGroup);

        Composite wells = new Composite(configurationGroup, SWT.NONE);
        wells.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        GridLayout wellsLayout = new GridLayout(3, false);
        wellsLayout.marginWidth = 0;
        wellsLayout.marginHeight = 0;
        wellsLayout.horizontalSpacing = 6;
        wells.setLayout(wellsLayout);
        xWell = createWell(wells, "X-Axis", FieldRole.X);
        valueWell = createWell(wells, "Values", FieldRole.VALUE);
        seriesWell = createWell(wells, "Series", FieldRole.SERIES);

        matrixConfiguration = new Composite(configurationGroup, SWT.NONE);
        matrixConfiguration.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        GridLayout matrixLayout = new GridLayout(3, true);
        matrixLayout.marginWidth = 0;
        matrixLayout.marginHeight = 0;
        matrixLayout.horizontalSpacing = 6;
        matrixLayout.verticalSpacing = 2;
        matrixConfiguration.setLayout(matrixLayout);
        matrixRowsWell = new OrderedFieldWell(matrixConfiguration, "Rows (ordered)", this::setMatrixRows);
        matrixColumnsWell = new OrderedFieldWell(matrixConfiguration, "Columns (ordered)", this::setMatrixColumns);
        matrixValuesWell = new OrderedFieldWell(matrixConfiguration, "Values (ordered)", this::setMatrixValues);
        Composite matrixOptionsBar = new Composite(matrixConfiguration, SWT.NONE);
        GridData matrixOptionsData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        matrixOptionsData.horizontalSpan = 3;
        matrixOptionsBar.setLayoutData(matrixOptionsData);
        GridLayout matrixOptionsLayout = new GridLayout(3, false);
        matrixOptionsLayout.marginWidth = 0;
        matrixOptionsLayout.marginHeight = 0;
        matrixOptionsBar.setLayout(matrixOptionsLayout);
        rowTotalsButton = matrixOption(matrixOptionsBar, "Row totals", matrixOptions.rowTotals());
        columnTotalsButton = matrixOption(matrixOptionsBar, "Column totals", matrixOptions.columnTotals());
        subtotalsButton = matrixOption(matrixOptionsBar, "Subtotals", matrixOptions.subtotals());
        setVisible(matrixConfiguration, false);

        Composite actionBand = new Composite(configurationGroup, SWT.NONE);
        actionBand.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        GridLayout actionBandLayout = new GridLayout(2, false);
        actionBandLayout.marginWidth = 0;
        actionBandLayout.marginHeight = 0;
        actionBandLayout.horizontalSpacing = 6;
        actionBand.setLayout(actionBandLayout);

        Composite localActions = new Composite(actionBand, SWT.NONE);
        localActions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        GridLayout localLayout = new GridLayout(3, false);
        localLayout.marginWidth = 0;
        localLayout.marginHeight = 0;
        localLayout.horizontalSpacing = 4;
        localActions.setLayout(localLayout);
        addCalculatedFieldButton = new Button(localActions, SWT.PUSH);
        addCalculatedFieldButton.setText("Formulas…");
        addCalculatedFieldButton.setToolTipText("Create, edit, or delete local calculated fields");
        addCalculatedFieldButton.setEnabled(false);
        setAccessibleName(addCalculatedFieldButton, "Manage Local Calculated Fields");
        addCalculatedFieldButton.addListener(SWT.Selection, event -> openCalculatedFieldManager());
        slicersButton = new Button(localActions, SWT.PUSH);
        slicersButton.setText("Slicer ▾");
        slicersButton.setToolTipText("Add or edit a slicer, or clear all active slicers");
        slicersButton.addListener(SWT.Selection, event -> showDropdownMenu(slicersButton, menu -> {
            addMenuItem(menu, "Add/Edit Slicer…", "Choose a field and values to filter the current chart",
                    () -> openSlicerDialog());
            addMenuItem(menu, "Clear Slicers", "Remove all active slicers",
                    () -> { slicers.clear(); updateSlicerLabel(); updateChart(); });
        }));
        Button savePreset = new Button(localActions, SWT.PUSH);
        savePreset.setText("Presets ▾");
        savePreset.setToolTipText("Save, load, or delete a saved chart layout preset for this result shape");
        savePreset.addListener(SWT.Selection, event -> showDropdownMenu(savePreset, menu -> {
            addMenuItem(menu, "Save Preset…", "Save the current chart layout for this result shape",
                    () -> saveVisualizationPreset());
            addMenuItem(menu, "Load Preset…", "Restore a saved preset for this result shape",
                    () -> loadVisualizationPreset());
            addMenuItem(menu, "Delete Preset…", "Remove a saved preset by name",
                    () -> deleteVisualizationPreset());
        }));
        Composite sourceActions = new Composite(actionBand, SWT.NONE);
        sourceActions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        GridLayout sourceLayout = new GridLayout(2, false);
        sourceLayout.marginWidth = 0;
        sourceLayout.marginHeight = 0;
        sourceLayout.horizontalSpacing = 4;
        sourceActions.setLayout(sourceLayout);
        sourceQueryButton = new Button(sourceActions, SWT.PUSH);
        sourceQueryButton.setText("Source Query…");
        sourceQueryButton.setToolTipText("Add existing fields and aggregations, manage custom SQL fields, preview SQL, and execute it here");
        sourceQueryButton.addListener(SWT.Selection, event -> openSourceQueryBuilder());
        sortButton = new Button(sourceActions, SWT.PUSH);
        sortButton.setText("Sort…");
        sortButton.setToolTipText("Set one or more ASC/DESC sort keys in priority order");
        sortButton.addListener(SWT.Selection, event -> openSortDialog());
        calculatedFieldStatus = new Label(configurationGroup, SWT.NONE);
        calculatedFieldStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        setVisible(calculatedFieldStatus, false);

        Composite controls = new Composite(configurationGroup, SWT.NONE);
        controls.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        GridLayout controlsLayout = new GridLayout(4, false);
        controlsLayout.marginWidth = 0;
        controlsLayout.marginHeight = 0;
        controlsLayout.horizontalSpacing = 6;
        controls.setLayout(controlsLayout);

        Composite chartOptions = createOptionCard(controls, "Chart");
        chartTypeCombo = new Combo(chartOptions, SWT.DROP_DOWN | SWT.READ_ONLY);
        chartTypeCombo.setLayoutData(compactComboData(90));
        setAccessibleName(chartTypeCombo, "Chart Type");
        for (ChartType type : chartTypes) chartTypeCombo.add(type.displayName());
        chartTypeCombo.addListener(SWT.Selection, event -> {
            if (configuration == null) return;
            configuration = configuration.withChartType(selectedChartType());
            initializeDimensionSelections(snapshot);
            updateRoleLabels();
            selectNumericXForScatter();
            updateChart();
        });

        Composite aggregationOptions = createOptionCard(controls, "Agg");
        aggregationCombo = new Combo(aggregationOptions, SWT.DROP_DOWN | SWT.READ_ONLY);
        aggregationCombo.setLayoutData(compactComboData(80));
        setAccessibleName(aggregationCombo, "Aggregation");
        aggregationCombo.addListener(SWT.Selection, event -> {
            if (configuration == null || aggregationCombo.getSelectionIndex() < 0) return;
            if (aggregationCombo.getSelectionIndex() >= availableAggregations.size()) return;
            configuration = configuration.withAggregation(
                    availableAggregations.get(aggregationCombo.getSelectionIndex()));
            updateChart();
        });

        Composite yMaximumOptions = createOptionCard(controls, "Y Max");
        yMaximumCombo = new Combo(yMaximumOptions, SWT.DROP_DOWN);
        yMaximumCombo.setLayoutData(compactComboData(85));
        yMaximumCombo.add("Auto (rounded)");
        yMaximumCombo.add("100");
        yMaximumCombo.add("1,000");
        yMaximumCombo.add("10,000");
        yMaximumCombo.select(0);
        yMaximumCombo.setToolTipText("Use Auto for a rounded upper bound, or type a number and press Enter");
        yMaximumCombo.addListener(SWT.Selection, event -> applyYMaximum());
        yMaximumCombo.addListener(SWT.DefaultSelection, event -> applyYMaximum());
        yMaximumCombo.addListener(SWT.FocusOut, event -> applyYMaximum());

        Button reset = new Button(controls, SWT.PUSH);
        reset.setText("Reset");
        reset.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        setAccessibleName(reset, "Reset Visualization");
        reset.setToolTipText("Clear field assignments without changing SQL or results");
        reset.addListener(SWT.Selection, event -> resetVisualization());

        ViewTheme.compact(configurationGroup);
        ViewTheme.improveContrast(configurationGroup);
        updateConfigurationViewport(false);
    }

    private static Composite createOptionCard(Composite parent, String title) {
        Composite card = new Composite(parent, SWT.NONE);
        card.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 3;
        card.setLayout(layout);
        new Label(card, SWT.NONE).setText(title + ":");
        return card;
    }

    /**
     * Opens a drop-down {@link Menu} anchored below the given button, populated via
     * {@code populate}. Used to consolidate related actions (Presets, Export) behind a single
     * button instead of one button per action.
     */
    private static void showDropdownMenu(Button anchor, java.util.function.Consumer<Menu> populate) {
        Menu menu = new Menu(anchor.getShell(), SWT.POP_UP);
        populate.accept(menu);
        org.eclipse.swt.graphics.Point location =
                anchor.toDisplay(0, anchor.getSize().y);
        menu.setLocation(location);
        menu.setVisible(true);
    }

    private static void addMenuItem(Menu menu, String text, String toolTip, Runnable action) {
        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText(text);
        if (toolTip != null) item.setToolTipText(toolTip);
        item.addListener(SWT.Selection, event -> action.run());
    }

    private static GridData compactComboData(int width) {
        GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        data.widthHint = width;
        return data;
    }

    private Button matrixOption(Composite parent, String text, boolean selected) {
        Button button = new Button(parent, SWT.CHECK);
        button.setText(text);
        button.setSelection(selected);
        button.addListener(SWT.Selection, event -> {
            matrixOptions = new MatrixDisplayOptions(rowTotalsButton.getSelection(),
                    columnTotalsButton.getSelection(), subtotalsButton.getSelection());
            updateChart();
        });
        return button;
    }

    private Combo createWell(Composite parent, String title, FieldRole role) {
        Composite card = new Composite(parent, SWT.NONE);
        card.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 3;
        card.setLayout(layout);
        Label label = new Label(card, SWT.NONE);
        label.setText(title + ":");
        if (role == FieldRole.X) xWellLabel = label;
        if (role == FieldRole.SERIES) seriesWellLabel = label;
        Combo well = new Combo(card, SWT.DROP_DOWN | SWT.READ_ONLY);
        setAccessibleName(well, title + " field well");
        well.setLayoutData(compactComboData(100));
        well.setToolTipText("Select a field from the list");
        well.addListener(SWT.Selection, event -> assignRole(role, well.getSelectionIndex() - 1));
        return well;
    }

    private void updateRoleLabels() {
        boolean matrix = selectedChartType() == ChartType.MATRIX || selectedChartType() == ChartType.HEATMAP;
        xWellLabel.setText("X-Axis:");
        seriesWellLabel.setText("Series:");
        setVisible(xWell.getParent(), !matrix);
        setVisible(valueWell.getParent(), !matrix);
        setVisible(seriesWell.getParent(), !matrix);
        setVisible(matrixConfiguration, matrix);
        if (matrix && snapshot != null) updateMatrixWells();
        configurationGroup.layout(true, true);
        updateConfigurationViewport(matrix);
    }

    /**
     * Sizes the builder viewport to its actual content instead of a fixed pixel guess.
     * The normal (non-matrix) builder must show its entire control set with no vertical
     * scrolling at ordinary docked widths, so its {@link GridData#heightHint} is left at
     * {@link SWT#DEFAULT}: SWT then asks {@link Group#computeSize} for the real preferred
     * height of whatever is currently visible (the fixed set of wells/action rows, since
     * the taller matrix wells are hidden), and the chart area above simply receives the
     * remaining space. Matrix/Heatmap legitimately has more controls (three ordered wells
     * plus totals checkboxes); it keeps the same {@code SWT.DEFAULT} sizing so nothing is
     * pre-emptively clipped, and only falls back to the {@link ScrolledComposite}'s normal
     * scrollbar if the panel is ever docked narrower/shorter than the matrix controls'
     * genuine preferred size — never because of an arbitrary hardcoded cap.
     */
    private void updateConfigurationViewport(boolean matrix) {
        if (configurationScroller == null || configurationScroller.isDisposed()) return;
        GridData data = (GridData) configurationScroller.getLayoutData();
        data.heightHint = SWT.DEFAULT;
        configurationScroller.setMinSize(configurationGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        configurationScroller.getParent().layout(true, true);
    }

    private void applyUpdate(ResultSetUpdate update) {
        if (content == null || content.isDisposed()) return;
        switch (update.status()) {
            case LOADING -> showMessage("Reading the active result set...");
            case NO_ACTIVE_RESULT -> {
                pendingAggregateQuery = null;
                aggregateSnapshot = null;
                displayMode = VisualizerSession.DisplayMode.SOURCE;
                if (!activeSessionIdentity.isBlank()) {
                    persistCurrentSessionState(activeSessionIdentity);
                    activeSessionIdentity = "";
                }
                DBeaverSqlDialectService.clearQuoteString();
                showMessage(update.message());
            }
            case ERROR -> {
                pendingAggregateQuery = null;
                showMessage(update.message());
            }
            case READY -> {
                if (pendingAggregateQuery != null) {
                    aggregateSnapshot = update.snapshot();
                    displayMode = VisualizerSession.DisplayMode.AGGREGATE;
                    pendingAggregateQuery = null;
                    showAggregateSnapshot(aggregateSnapshot);
                    return;
                }
                showBaseSnapshot(update.snapshot());
            }
        }
    }

    private void showMessage(String message) {
        summaryLabel.setText("Results Visualizer");
        messageLabel.setText(message);
        setVisible(messageLabel, true);
        setVisible(rowLimitWarning, false);
        setVisible(body, false);
        setVisible(configurationScroller, false);
        if (addCalculatedFieldButton != null) addCalculatedFieldButton.setEnabled(false);
        content.layout(true, true);
    }

    private void updateQuoteContext() {
        if (resultSetService == null) {
            DBeaverSqlDialectService.clearQuoteString();
            return;
        }
        DBeaverSqlDialectService.installQuoteStyle(resultSetService.activeIdentifierQuoteStyle());
    }

    /**
     * Applies a freshly extracted base snapshot from DBeaver. Because DBeaver's result-set
     * listener fires a fresh READY event on effectively every focus/tab change — not only
     * on a genuine query rerun — this only invalidates a previously computed aggregate
     * when the new base data actually differs in content from what we last knew (see
     * {@link ResultSetSnapshot#sameData}); a mere refocus that re-delivers identical data
     * must leave an active aggregate/{@code displayMode} exactly as the user left it.
     */
    private void showBaseSnapshot(ResultSetSnapshot newBaseSnapshot) {
        switchSessionIfNeeded(currentSessionIdentity());
        boolean genuineRerun = baseSnapshot == null || !baseSnapshot.sameData(newBaseSnapshot);
        baseSnapshot = newBaseSnapshot;
        if (genuineRerun) {
            // The source query actually reran with different data: any previously computed
            // aggregate no longer matches it, so drop it and fall back to the source view.
            displayMode = VisualizerSession.DisplayMode.SOURCE;
            aggregateSnapshot = null;
        }
        CalculatedFieldProjection projection =
                calculatedFieldService.project(newBaseSnapshot, calculatedFields);
        updateFieldStatus(projection.errors());
        if (!genuineRerun && displayMode == VisualizerSession.DisplayMode.AGGREGATE && aggregateSnapshot != null) {
            // Keep showing the still-valid aggregate; nothing about the source data changed.
            showAggregateSnapshot(aggregateSnapshot);
        } else {
            showSnapshot(projection.snapshot());
        }
    }

    private void renderSourceView(ResultSetSnapshot sourceSnapshot) {
        if (sourceSnapshot == null) return;
        displayMode = VisualizerSession.DisplayMode.SOURCE;
        CalculatedFieldProjection projection =
                calculatedFieldService.project(sourceSnapshot, calculatedFields);
        updateFieldStatus(projection.errors());
        showSnapshot(projection.snapshot());
    }

    private void showAggregateSnapshot(ResultSetSnapshot aggregateResult) {
        if (aggregateResult == null) return;
        displayMode = VisualizerSession.DisplayMode.AGGREGATE;
        aggregateSnapshot = aggregateResult;
        snapshot = aggregateResult;
        String source = aggregateResult.sourceName().isBlank() ? "Aggregate result" : aggregateResult.sourceName();
        summaryLabel.setText(source + " — " + aggregateResult.columns().size() + " fields, "
                + aggregateResult.availableRowCount() + " rows");
        if (aggregateResult.truncated()) {
            rowLimitWarning.setText(formatRowLimitWarning());
            rowLimitWarning.setToolTipText(formatRowLimitTooltip(aggregateResult.configuredRowLimit()));
        }
        setVisible(rowLimitWarning, aggregateResult.truncated());
        setVisible(messageLabel, false);
        setVisible(body, true);
        setVisible(configurationScroller, true);
        updateDisplayModeControls();
        updateChart();
        content.layout(true, true);
        persistCurrentSessionState(activeSessionIdentity);
    }

    private void switchToSourceView() {
        if (baseSnapshot == null) return;
        displayMode = VisualizerSession.DisplayMode.SOURCE;
        renderSourceView(baseSnapshot);
    }

    private void updateDisplayModeControls() {
        if (backToOriginalButton == null || backToOriginalButton.isDisposed()) return;
        boolean shouldShow = aggregateSnapshot != null && displayMode == VisualizerSession.DisplayMode.AGGREGATE;
        backToOriginalButton.setVisible(shouldShow);
        if (shouldShow) {
            summaryLabel.setText((summaryLabel.getText().contains("Aggregate") ? summaryLabel.getText() : "Viewing: Aggregate Result")
                    + "   [Back to Original]");
        }
    }

    /** Identity of the DBeaver result/controller currently backing the view, or "" if none. */
    private String currentSessionIdentity() {
        return resultSetService == null ? "" : resultSetService.activeResultIdentity();
    }

    /**
     * Switches the live per-result fields (slicers, calculated fields, sort rules, custom
     * fields, matrix options, chart configuration, pending aggregate query) to match
     * {@code newIdentity}, persisting the outgoing identity's state first so it can be
     * restored exactly when the user returns to it later.
     */
    private void switchSessionIfNeeded(String newIdentity) {
        String normalizedNew = visualizerSessionManager.sessionIdFor(newIdentity);
        String normalizedOld = visualizerSessionManager.sessionIdFor(activeSessionIdentity);
        if (normalizedNew.equals(normalizedOld) && baseSnapshot != null) {
            updateQuoteContext();
            return;
        }
        if (baseSnapshot != null && !activeSessionIdentity.isBlank()) {
            persistCurrentSessionState(activeSessionIdentity);
        }
        activeSessionIdentity = newIdentity;
        restoreSessionState(newIdentity);
        updateQuoteContext();
    }

    private void persistCurrentSessionState(String identity) {
        String sessionId = visualizerSessionManager.sessionIdFor(identity);
        visualizerSessionManager.update(sessionId, session -> session
                .withBaseSnapshot(baseSnapshot)
                .withAggregateSnapshot(aggregateSnapshot)
                .withConfiguration(configuration)
                .withMatrixOptions(matrixOptions)
                .withAggregateQuery(pendingAggregateQuery)
                .withCalculatedFields(new ArrayList<>(calculatedFields))
                .withSlicers(new ArrayList<>(slicers))
                .withSortRules(new ArrayList<>(sortRules))
                .withCustomSqlDimensions(new ArrayList<>(customSqlDimensions))
                .withDisplayMode(displayMode));
    }

    private void restoreSessionState(String identity) {
        VisualizerSession session = visualizerSessionManager.getOrCreate(identity);
        baseSnapshot = session.baseSnapshot();
        aggregateSnapshot = session.aggregateSnapshot();
        snapshot = session.aggregateSnapshot() != null ? session.aggregateSnapshot() : session.baseSnapshot();
        configuration = session.configuration();
        configurationInitialized = configuration != null;
        matrixOptions = session.matrixOptions();
        pendingAggregateQuery = session.aggregateQuery();
        displayMode = session.displayMode() == null ? VisualizerSession.DisplayMode.SOURCE : session.displayMode();
        calculatedFields.clear();
        calculatedFields.addAll(session.calculatedFields());
        slicers.clear();
        slicers.addAll(session.slicers());
        sortRules = new ArrayList<>(session.sortRules());
        customSqlDimensions.clear();
        customSqlDimensions.addAll(session.customSqlDimensions());
        activeXChoice = null;
        activeSeriesChoice = null;
        matrixRows.clear();
        matrixColumns.clear();
        matrixValues.clear();
    }

    private void showSnapshot(ResultSetSnapshot newSnapshot) {
        ResultSetSnapshot previous = snapshot;
        snapshot = newSnapshot;
        slicers.removeIf(slicer -> newSnapshot.columns().stream().noneMatch(column ->
                column.displayName().equalsIgnoreCase(slicer.fieldName())));
        updateSlicerLabel();
        addCalculatedFieldButton.setEnabled(true);
        if (newSnapshot.columns().isEmpty()) {
            showMessage("The active result set has no columns.");
            return;
        }
        if (applyPendingAggregateResult(newSnapshot)) {
            configurationInitialized = true;
        } else if (!configurationInitialized) {
            configuration = ChartDataBuilder.defaultVisualization(newSnapshot);
            configurationInitialized = true;
        } else if (!isCompatible(previous, newSnapshot, configuration)) {
            configuration = ChartDataBuilder.defaultVisualization(newSnapshot);
            activeXChoice = null;
            activeSeriesChoice = null;
            matrixRows.clear();
            matrixColumns.clear();
        }
        initializeDimensionSelections(newSnapshot);
        populateControls(newSnapshot);

        String source = newSnapshot.sourceName().isBlank() ? "Active result set" : newSnapshot.sourceName();
        summaryLabel.setText(source + " — " + newSnapshot.columns().size() + " fields, "
                + newSnapshot.availableRowCount() + " rows");
        if (newSnapshot.truncated()) {
            rowLimitWarning.setText(formatRowLimitWarning());
            rowLimitWarning.setToolTipText(formatRowLimitTooltip(newSnapshot.configuredRowLimit()));
        }
        setVisible(rowLimitWarning, newSnapshot.truncated());
        setVisible(messageLabel, false);
        setVisible(body, true);
        setVisible(configurationScroller, true);
        updateDisplayModeControls();
        updateChart();
        content.layout(true, true);
        persistCurrentSessionState(activeSessionIdentity);
    }


    private void saveVisualizationPreset() {
        if (snapshot == null || configuration == null) {
            return;
        }
        InputDialog dialog = new InputDialog(content.getShell(), "Save visualization preset",
                "Choose a name for this chart layout for the current result shape.",
                "", value -> value != null && !value.trim().isEmpty() ? null : "Preset name is required.");
        if (dialog.open() != Window.OK) {
            return;
        }
        String name = dialog.getValue().trim();
        if (name.isEmpty()) {
            MessageDialog.openError(content.getShell(), "Preset not saved", "Preset name is required.");
            return;
        }
        presetStore.save(name, snapshot, configuration, matrixOptions);
        MessageDialog.openInformation(content.getShell(), "Preset saved",
                "Saved '" + name + "' for '" + snapshot.sourceName() + "'.");
    }

    private void loadVisualizationPreset() {
        if (snapshot == null) {
            return;
        }
        List<VisualizerPreset> available = presetStore.listFor(snapshot);
        String prompt = available.isEmpty()
                ? "No saved presets match this result shape yet. Enter a preset name anyway."
                : "Enter the preset name to restore for the current result shape.\nSaved for this result: "
                        + available.stream().map(VisualizerPreset::name)
                                .collect(java.util.stream.Collectors.joining(", "));
        InputDialog dialog = new InputDialog(content.getShell(), "Load visualization preset", prompt,
                "", value -> value != null && !value.trim().isEmpty() ? null : "Preset name is required.");
        if (dialog.open() != Window.OK) {
            return;
        }
        String name = dialog.getValue().trim();
        var preset = presetStore.load(name, snapshot);
        if (preset.isEmpty()) {
            MessageDialog.openInformation(content.getShell(), "Preset not found",
                    "No saved preset named '" + name + "' matches this result set.");
            return;
        }
        VisualizerPreset loaded = preset.get();
        configuration = loaded.toConfiguration();
        matrixOptions = loaded.matrixOptions();
        initializeDimensionSelections(snapshot);
        populateControls(snapshot);
        updateRoleLabels();
        updateChart();
        MessageDialog.openInformation(content.getShell(), "Preset loaded",
                "Restored preset '" + name + "'.");
    }

    private void deleteVisualizationPreset() {
        if (snapshot == null) {
            return;
        }
        List<VisualizerPreset> available = presetStore.listFor(snapshot);
        String prompt = available.isEmpty()
                ? "No saved presets match this result shape."
                : "Enter the preset name to delete.\nSaved for this result: "
                        + available.stream().map(VisualizerPreset::name)
                                .collect(java.util.stream.Collectors.joining(", "));
        if (available.isEmpty()) {
            MessageDialog.openInformation(content.getShell(), "No presets", prompt);
            return;
        }
        InputDialog dialog = new InputDialog(content.getShell(), "Delete visualization preset", prompt,
                "", value -> value != null && !value.trim().isEmpty() ? null : "Preset name is required.");
        if (dialog.open() != Window.OK) {
            return;
        }
        String name = dialog.getValue().trim();
        boolean removed = presetStore.delete(name);
        if (removed) {
            MessageDialog.openInformation(content.getShell(), "Preset deleted", "Deleted preset '" + name + "'.");
        } else {
            MessageDialog.openInformation(content.getShell(), "Preset not found",
                    "No saved preset named '" + name + "' was found.");
        }
    }

    private void openCalculatedFieldManager() {
        if (snapshot == null || baseSnapshot == null) return;
        CalculatedFieldManagerDialog dialog = new CalculatedFieldManagerDialog(
                content.getShell(), snapshot, calculatedFieldService, calculatedFields);
        if (dialog.open() != Window.OK) return;
        calculatedFields.clear();
        calculatedFields.addAll(dialog.definitions());
        showBaseSnapshot(baseSnapshot);
    }

    private void populateControls(ResultSetSnapshot value) {
        xWellChoices = dimensionChoices(value);
        seriesWellChoices = dimensionChoices(value);
        populateDimensionWell(xWell, xWellChoices, selectedRows().stream().findFirst().orElse(null));
        valueWell.removeAll();
        valueWell.add("(none)");
        valueWellIndexes = java.util.stream.IntStream.range(0, value.columns().size())
                .boxed().toList();
        for (int index : valueWellIndexes) valueWell.add(value.columns().get(index).displayName());
        populateDimensionWell(seriesWell, seriesWellChoices, selectedColumns().stream().findFirst().orElse(null));
        chartTypeCombo.select(chartTypes.indexOf(configuration.chartType()));
        updateRoleLabels();
        updateAggregationOptions(value);
        int valueSelection = valueWellIndexes.indexOf(configuration.valueColumnIndex());
        valueWell.select(valueSelection < 0 ? 0 : valueSelection + 1);
        if (configuration.yAxisMaximum() == null) {
            yMaximumCombo.select(0);
        } else {
            yMaximumCombo.setText(formatAxisMaximum(configuration.yAxisMaximum()));
        }
    }

    /**
     * Repopulates the Aggregation combo with only the aggregations that are meaningful for the
     * currently selected Values column's type (see {@link Aggregation#compatibleWith}), and, if
     * the configuration's current aggregation is no longer compatible (e.g. the Values column
     * changed from numeric to a string column), automatically switches it to COUNT so the chart
     * keeps rendering instead of silently keeping an invalid aggregation.
     */
    private void updateAggregationOptions(ResultSetSnapshot value) {
        int columnIndex = configuration.valueColumnIndex();
        NormalizedDataType type = columnIndex >= 0 && columnIndex < value.columns().size()
                ? value.columns().get(columnIndex).normalizedType() : NormalizedDataType.OTHER;
        availableAggregations = Aggregation.compatibleWith(type);
        aggregationCombo.removeAll();
        for (Aggregation aggregation : availableAggregations) aggregationCombo.add(aggregation.toString());
        if (!availableAggregations.contains(configuration.aggregation())) {
            configuration = configuration.withAggregation(Aggregation.COUNT);
        }
        int selection = availableAggregations.indexOf(configuration.aggregation());
        aggregationCombo.select(selection < 0 ? 0 : selection);
    }

    private void assignRole(FieldRole role, int index) {
        if (configuration == null || snapshot == null) return;
        if (role == FieldRole.VALUE) {
            if (index < 0) {
                configuration = configuration.withValue(VisualizationConfiguration.UNASSIGNED);
                matrixValues.clear();
            } else if (index < valueWellIndexes.size()) {
                configuration = configuration.withValue(valueWellIndexes.get(index));
                matrixValues = new ArrayList<>(List.of(
                        DimensionChoice.result(snapshot, valueWellIndexes.get(index))));
            } else {
                return;
            }
        } else {
            List<DimensionChoice> choices = role == FieldRole.X ? xWellChoices : seriesWellChoices;
            DimensionChoice choice = index < 0 || index >= choices.size() ? null : choices.get(index);
            if (role == FieldRole.X) {
                activeXChoice = choice;
                matrixRows = choice == null ? new ArrayList<>() : new ArrayList<>(List.of(choice));
                configuration = configuration.withXColumns(resultIndexes(matrixRows));
            } else {
                activeSeriesChoice = choice;
                matrixColumns = choice == null ? new ArrayList<>() : new ArrayList<>(List.of(choice));
                configuration = configuration.withSeriesColumns(resultIndexes(matrixColumns));
            }
        }
        populateControls(snapshot);
        selectNumericXForScatter();
        updateChart();
    }

    private void selectNumericXForScatter() {
        if (snapshot == null || configuration.chartType() != ChartType.SCATTER) return;
        int x = configuration.xColumnIndex();
        if (x >= 0 && ChartDataBuilder.isNumeric(snapshot.columns().get(x))) return;
        int numericX = ChartDataBuilder.firstNumericColumn(
                snapshot.columns(), configuration.valueColumnIndex());
        if (numericX >= 0) {
            configuration = configuration.withX(numericX);
            activeXChoice = DimensionChoice.result(snapshot, numericX);
            xWell.select(numericX + 1);
        }
    }

    private void resetVisualization() {
        if (configuration == null) return;
        configuration = VisualizationConfiguration.empty(configuration.chartType());
        activeXChoice = null;
        activeSeriesChoice = null;
        matrixRows.clear();
        matrixColumns.clear();
        matrixValues.clear();
        sortRules = new ArrayList<>();
        updateSortButton();
        populateControls(snapshot);
        chartCanvas.setPrompt("Choose X and Values below to build a chart.");
    }

    private void setMatrixRows(List<DimensionChoice> choices) {
        matrixRows = new ArrayList<>(choices);
        activeXChoice = matrixRows.isEmpty() ? null : matrixRows.get(0);
        configuration = configuration.withXColumns(resultIndexes(matrixRows));
        updateChart();
    }

    private void setMatrixColumns(List<DimensionChoice> choices) {
        matrixColumns = new ArrayList<>(choices);
        activeSeriesChoice = matrixColumns.isEmpty() ? null : matrixColumns.get(0);
        configuration = configuration.withSeriesColumns(resultIndexes(matrixColumns));
        updateChart();
    }

    private void setMatrixValues(List<DimensionChoice> choices) {
        matrixValues = new ArrayList<>(choices);
        int primary = matrixValues.isEmpty() ? VisualizationConfiguration.UNASSIGNED
                : matrixValues.get(0).resultIndex();
        configuration = configuration.withValue(primary);
        updateChart();
    }

    private void updateMatrixWells() {
        matrixRowsWell.setChoices(dimensionChoices(snapshot), matrixRows);
        matrixColumnsWell.setChoices(dimensionChoices(snapshot), matrixColumns);
        List<DimensionChoice> values = valueWellIndexes.stream()
                .map(index -> DimensionChoice.result(snapshot, index)).toList();
        matrixValuesWell.setChoices(values, matrixValues);
    }

    private void initializeDimensionSelections(ResultSetSnapshot value) {
        matrixValues.removeIf(choice -> choice.isCustom()
                || choice.resultIndex() >= value.columns().size());
        if (activeXChoice == null || (!activeXChoice.isCustom()
                && activeXChoice.resultIndex() >= value.columns().size())) {
            activeXChoice = configuration.xColumnIndex() < 0 ? null
                    : DimensionChoice.result(value, configuration.xColumnIndex());
        }
        if (activeSeriesChoice == null || (!activeSeriesChoice.isCustom()
                && activeSeriesChoice.resultIndex() >= value.columns().size())) {
            activeSeriesChoice = configuration.seriesColumnIndex() < 0 ? null
                    : DimensionChoice.result(value, configuration.seriesColumnIndex());
        }
        if (matrixRows.isEmpty()) {
            matrixRows = configuration.xColumnIndexes().stream()
                    .map(index -> DimensionChoice.result(value, index)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }
        if (matrixColumns.isEmpty()) {
            matrixColumns = configuration.seriesColumnIndexes().stream()
                    .map(index -> DimensionChoice.result(value, index)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }
        if (matrixValues.isEmpty() && configuration.valueColumnIndex() >= 0) {
            matrixValues.add(DimensionChoice.result(value, configuration.valueColumnIndex()));
        }
    }

    private boolean applyPendingAggregateResult(ResultSetSnapshot value) {
        if (pendingAggregateQuery == null) return false;
        List<Integer> rows = findColumns(value, pendingAggregateQuery.rowAliases());
        List<Integer> columns = findColumns(value, pendingAggregateQuery.columnAliases());
        int resultValue = findColumn(value, pendingAggregateQuery.valueAlias());
        if (rows.size() != pendingAggregateQuery.rowAliases().size()
                || columns.size() != pendingAggregateQuery.columnAliases().size() || resultValue < 0) return false;
        configuration = new VisualizationConfiguration(configuration.chartType(), rows, resultValue,
                columns, Aggregation.SUM, configuration.yAxisMaximum());
        activeXChoice = DimensionChoice.result(value, rows.get(0));
        activeSeriesChoice = columns.isEmpty() ? null : DimensionChoice.result(value, columns.get(0));
        matrixRows = rows.stream().map(index -> DimensionChoice.result(value, index))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        matrixColumns = columns.stream().map(index -> DimensionChoice.result(value, index))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        matrixValues = java.util.stream.IntStream.range(0, value.columns().size())
                .filter(index -> !rows.contains(index) && !columns.contains(index)
                        && ChartDataBuilder.isNumeric(value.columns().get(index)))
                .mapToObj(index -> DimensionChoice.result(value, index))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (matrixValues.isEmpty()) matrixValues.add(DimensionChoice.result(value, resultValue));
        pendingAggregateQuery = null;
        return true;
    }

    private List<DimensionChoice> selectedRows() {
        return isMatrix() ? List.copyOf(matrixRows)
                : activeXChoice == null ? List.of() : List.of(activeXChoice);
    }

    private List<DimensionChoice> selectedColumns() {
        return isMatrix() ? List.copyOf(matrixColumns)
                : activeSeriesChoice == null ? List.of() : List.of(activeSeriesChoice);
    }

    private boolean isMatrix() {
        return configuration != null
                && (configuration.chartType() == ChartType.MATRIX || configuration.chartType() == ChartType.HEATMAP);
    }

    private List<DimensionChoice> dimensionChoices(ResultSetSnapshot value) {
        List<DimensionChoice> choices = new ArrayList<>();
        for (int index = 0; index < value.columns().size(); index++) choices.add(DimensionChoice.result(value, index));
        customSqlDimensions.stream()
                .filter(dimension -> value.columns().stream().noneMatch(column ->
                        column.displayName().equalsIgnoreCase(dimension.name())))
                .map(DimensionChoice::custom).forEach(choices::add);
        return List.copyOf(choices);
    }

    private static void populateDimensionWell(Combo well, List<DimensionChoice> choices,
            DimensionChoice selected) {
        well.removeAll();
        well.add("(none)");
        choices.forEach(choice -> well.add(choice.displayName()));
        int index = -1;
        if (selected != null) {
            for (int choiceIndex = 0; choiceIndex < choices.size(); choiceIndex++) {
                if (sameChoice(selected, choices.get(choiceIndex))) { index = choiceIndex; break; }
            }
        }
        well.select(index + 1);
    }

    private static boolean sameChoice(DimensionChoice left, DimensionChoice right) {
        return left.displayName().equalsIgnoreCase(right.displayName())
                && left.isCustom() == right.isCustom();
    }

    private static List<Integer> resultIndexes(List<DimensionChoice> choices) {
        return choices.stream().filter(choice -> !choice.isCustom())
                .map(DimensionChoice::resultIndex).toList();
    }

    private static List<Integer> findColumns(ResultSetSnapshot value, List<String> names) {
        List<Integer> indexes = new ArrayList<>();
        for (String name : names) {
            int index = findColumn(value, name);
            if (index >= 0) indexes.add(index);
        }
        return indexes;
    }

    private static int findColumn(ResultSetSnapshot value, String name) {
        for (int index = 0; index < value.columns().size(); index++) {
            if (value.columns().get(index).displayName().equalsIgnoreCase(name)) return index;
        }
        return -1;
    }

    private void openSlicerDialog() {
        if (snapshot == null) return;
        SlicerDialog dialog = new SlicerDialog(content.getShell(), snapshot, this::previewDistinctSourceQuery);
        if (dialog.open() != Window.OK || dialog.definition() == null) return;
        slicers.removeIf(existing -> existing.fieldName().equalsIgnoreCase(dialog.definition().fieldName()));
        slicers.add(dialog.definition());
        updateSlicerLabel();
        updateChart();
    }

    private DimensionChoice retainChoice(DimensionChoice choice) {
        if (choice == null || !choice.isCustom()) return choice;
        return customSqlDimensions.stream().filter(field -> field.name().equalsIgnoreCase(choice.displayName()))
                .map(DimensionChoice::custom).findFirst().orElse(null);
    }

    private List<DimensionChoice> refreshChoices(List<DimensionChoice> choices) {
        return choices.stream().map(this::retainChoice)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private void updateFieldStatus(List<String> errors) {
        configurationGroup.setText("Visualization Builder");
        configurationGroup.setToolTipText(calculatedFields.size() + " local field(s), "
                + customSqlDimensions.size() + " SQL field(s)");
        if (addCalculatedFieldButton != null && !addCalculatedFieldButton.isDisposed()) {
            addCalculatedFieldButton.setText(calculatedFields.isEmpty()
                    ? "Formulas…" : "Formulas (" + calculatedFields.size() + ")…");
        }
        if (sourceQueryButton != null && !sourceQueryButton.isDisposed()) {
            sourceQueryButton.setText(customSqlDimensions.isEmpty()
                    ? "Source Query…" : "Source Query (" + customSqlDimensions.size() + ")…");
        }
        calculatedFieldStatus.setText(String.join("  ", errors));
        setVisible(calculatedFieldStatus, !errors.isEmpty());
        configurationGroup.layout(true, true);
        updateConfigurationViewport(configuration != null && isMatrix());
    }

    private void openSourceQueryBuilder() {
        if (snapshot == null || configuration == null) return;
        try {
            List<QueryDimension> dimensions = baseQueryDimensions();
            List<QueryMeasure> measures = baseQueryMeasures();
            String selectedMeasure = configuration.valueColumnIndex() < 0 ? ""
                    : snapshot.columns().get(configuration.valueColumnIndex()).displayName();
            FullSqlConfigurationDialog dialog = new FullSqlConfigurationDialog(content.getShell(),
                    resultSetService.sourceQuery(), dimensions, measures, customSqlDimensions,
                    snapshot, sqlTranslator(),
                    selectedRows().stream().map(DimensionChoice::displayName).toList(),
                    selectedColumns().stream().map(DimensionChoice::displayName).toList(),
                    selectedMeasure, configuration.aggregation(), slicers, sortRules);
            if (dialog.open() == Window.OK) {
                applyCustomSqlFields(dialog.customFields());
            }
            if (dialog.executeRequested() && dialog.query() != null) {
                pendingAggregateQuery = dialog.query();
                summaryLabel.setText("Executing source aggregate query…");
                resultSetService.executeQuery("Results Visualizer Source Query", dialog.query().sql());
            }
        } catch (RuntimeException error) {
            MessageDialog.openError(content.getShell(), "Cannot Build Aggregate Query", error.getMessage());
        }
    }

    private void applyCustomSqlFields(List<CustomSqlDimension> definitions) {
        if (definitions.stream().anyMatch(field -> snapshot.columns().stream()
                .anyMatch(column -> column.displayName().equalsIgnoreCase(field.name())))) {
            MessageDialog.openError(content.getShell(), "Duplicate Field Name",
                    "SQL field names must be different from existing result fields.");
            return;
        }
        customSqlDimensions.clear();
        customSqlDimensions.addAll(definitions);
        activeXChoice = retainChoice(activeXChoice);
        activeSeriesChoice = retainChoice(activeSeriesChoice);
        matrixRows = refreshChoices(matrixRows);
        matrixColumns = refreshChoices(matrixColumns);
        populateControls(snapshot);
        updateFieldStatus(List.of());
        updateChart();
    }

    private List<QueryDimension> baseQueryDimensions() {
        CalculatedFieldSqlTranslator translator = sqlTranslator();
        List<QueryDimension> dimensions = new ArrayList<>();
        for (ResultColumn column : snapshot.columns()) {
            dimensions.add(new QueryDimension(column.displayName(),
                    translator.expressionFor(column.displayName())));
        }
        return List.copyOf(dimensions);
    }

    private List<QueryMeasure> baseQueryMeasures() {
        CalculatedFieldSqlTranslator translator = sqlTranslator();
        List<QueryMeasure> measures = new ArrayList<>();
        for (ResultColumn column : snapshot.columns()) {
            if (ChartDataBuilder.isNumeric(column)) {
                measures.add(new QueryMeasure(column.displayName(),
                        translator.expressionFor(column.displayName())));
            }
        }
        return List.copyOf(measures);
    }

    private CalculatedFieldSqlTranslator sqlTranslator() {
        ResultSetSnapshot source = baseSnapshot == null ? snapshot : baseSnapshot;
        return new CalculatedFieldSqlTranslator(source.columns(), calculatedFields);
    }

    private void previewDistinctSourceQuery(String fieldName) {
        try {
            resultSetService.previewQuery("Distinct Source Values",
                    AggregateQueryBuilder.distinct(resultSetService.sourceQuery(), fieldName));
        } catch (RuntimeException error) {
            MessageDialog.openError(content.getShell(), "Cannot Build DISTINCT Query", error.getMessage());
        }
    }

    private void updateSlicerLabel() {
        if (slicersButton != null && !slicersButton.isDisposed()) {
            slicersButton.setText(slicers.isEmpty() ? "Slicer ▾" : "Slicers (" + slicers.size() + ") ▾");
            slicersButton.getParent().layout(true, true);
        }
    }

    private void openSortDialog() {
        if (snapshot == null) return;
        SortDialog dialog = new SortDialog(content.getShell(), snapshot, sortRules);
        if (dialog.open() != Window.OK) return;
        sortRules = new ArrayList<>(dialog.rules());
        updateSortButton();
        updateChart();
    }

    private void updateSortButton() {
        if (sortButton == null || sortButton.isDisposed()) return;
        sortButton.setText(sortRules.isEmpty() ? "Sort…" : "Sort (" + sortRules.size() + ")…");
        sortButton.getParent().layout(true, true);
    }

    private void applyYMaximum() {
        if (configuration == null) return;
        String text = yMaximumCombo.getText().trim();
        if (text.isEmpty() || text.equalsIgnoreCase("Auto (rounded)")) {
            configuration = configuration.withYAxisMaximum(null);
            yMaximumCombo.select(0);
            updateChart();
            return;
        }
        try {
            double maximum = Double.parseDouble(text.replace(",", ""));
            if (!Double.isFinite(maximum)) throw new NumberFormatException();
            configuration = configuration.withYAxisMaximum(maximum);
            yMaximumCombo.setText(formatAxisMaximum(maximum));
            updateChart();
        } catch (NumberFormatException error) {
            yMaximumCombo.setToolTipText("Enter a valid number, or choose Auto (rounded)");
            yMaximumCombo.select(0);
            configuration = configuration.withYAxisMaximum(null);
            updateChart();
        }
    }

    private static String formatAxisMaximum(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    /**
     * Short, static row-limit warning shown under the summary line. The actual configured
     * DBeaver row cap ({@code ModelPreferences.RESULT_SET_MAX_ROWS}) and the suggestion to use
     * Source Query for a full, source-level aggregate are surfaced via the label's tooltip
     * (see {@link #formatRowLimitTooltip(int)}) rather than in the permanent on-screen text.
     */
    private static String formatRowLimitWarning() {
        return "⚠ Row limit reached — visualization may be partial.";
    }

    /**
     * Tooltip text for the row-limit warning, naming the actual configured DBeaver row cap when
     * known and pointing to Source Query for a full, source-level aggregate.
     */
    private static String formatRowLimitTooltip(int configuredRowLimit) {
        String limitPhrase = configuredRowLimit > 0
                ? "Showing only the first " + configuredRowLimit + " rows (DBeaver's row limit). "
                : "Not all source rows are shown. ";
        return limitPhrase + "Use Source Query for a full, source-level aggregate.";
    }

    private void copyChartToClipboard() {
        if (chartCanvas == null || chartCanvas.isDisposed() || snapshot == null) {
            return;
        }
        Image image = chartCanvas.captureImage();
        try {
            Clipboard clipboard = new Clipboard(getSite().getShell().getDisplay());
            try {
                clipboard.setContents(new Object[] { image.getImageData() },
                        new Transfer[] { ImageTransfer.getInstance() });
            } finally {
                clipboard.dispose();
            }
            MessageDialog.openInformation(content.getShell(), "Chart copied",
                    "The current chart image was copied to the clipboard.");
        } finally {
            if (!image.isDisposed()) {
                image.dispose();
            }
        }
    }

    private void exportChartToFile(ExportFormat format) {
        if (chartCanvas == null || chartCanvas.isDisposed() || snapshot == null) {
            return;
        }
        FileDialog dialog = new FileDialog(content.getShell(), SWT.SAVE);
        dialog.setText("Save chart as " + format.name());
        dialog.setFilterNames(new String[] { format.description() });
        dialog.setFilterExtensions(format.filterExtensions());
        dialog.setFileName(format.defaultFileName());
        String filePath = dialog.open();
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            byte[] content = switch (format) {
                case PNG -> VisualizationExportService.pngBytes(chartCanvas);
                case JPEG -> VisualizationExportService.jpegBytes(chartCanvas);
                case SVG -> VisualizationExportService.svgBytes(chartCanvas);
                case PDF -> VisualizationExportService.pdfBytes(chartCanvas);
            };
            java.nio.file.Files.write(java.nio.file.Path.of(filePath), content);
        } catch (Exception e) {
            MessageDialog.openError(this.content.getShell(), "Export failed",
                    "The chart could not be exported: " + e.getMessage());
        }
    }

    private void updateChart() {
        if (snapshot == null || configuration == null) return;
        List<DimensionChoice> rows = selectedRows();
        if (rows.isEmpty() || (isMatrix() ? matrixValues.isEmpty() : configuration.valueColumnIndex() < 0)) {
            chartCanvas.setPrompt("Choose X and Values below to build a chart.");
            return;
        }
        if (rows.stream().anyMatch(DimensionChoice::isCustom)
                || selectedColumns().stream().anyMatch(DimensionChoice::isCustom)) {
            chartCanvas.setPrompt("Custom SQL fields run at the database. Choose Source Query, then Execute.");
            return;
        }
        configuration = configuration.withXColumns(resultIndexes(rows))
                .withSeriesColumns(resultIndexes(selectedColumns()));
        ResultSetSnapshot filtered = SnapshotSorter.apply(
                SnapshotSlicer.apply(snapshot, slicers), sortRules);
        var dataset = isMatrix()
                ? ChartDataBuilder.buildMatrixValues(filtered, configuration, resultIndexes(matrixValues))
                : ChartDataBuilder.build(filtered, configuration);
        if (isMatrix()) dataset = dataset.withMatrixOptions(matrixOptions);
        chartCanvas.setChart(configuration.chartType(), dataset);
    }

    private ChartType selectedChartType() {
        int selection = chartTypeCombo.getSelectionIndex();
        return selection < 0 ? ChartType.BAR : chartTypes.get(selection);
    }

    private static boolean isCompatible(ResultSetSnapshot oldSnapshot,
            ResultSetSnapshot newSnapshot, VisualizationConfiguration value) {
        if (oldSnapshot == null || value == null) return false;
        return value.xColumnIndexes().stream().allMatch(index -> sameColumn(oldSnapshot, newSnapshot, index))
                && sameColumn(oldSnapshot, newSnapshot, value.valueColumnIndex())
                && value.seriesColumnIndexes().stream().allMatch(index -> sameColumn(oldSnapshot, newSnapshot, index));
    }

    private static boolean sameColumn(ResultSetSnapshot oldSnapshot,
            ResultSetSnapshot newSnapshot, int index) {
        if (index < 0) return true;
        if (index >= oldSnapshot.columns().size() || index >= newSnapshot.columns().size()) return false;
        ResultColumn oldColumn = oldSnapshot.columns().get(index);
        ResultColumn newColumn = newSnapshot.columns().get(index);
        return oldColumn.displayName().equals(newColumn.displayName())
                && oldColumn.normalizedType() == newColumn.normalizedType();
    }

    private static void setVisible(org.eclipse.swt.widgets.Control control, boolean visible) {
        control.setVisible(visible);
        if (control.getLayoutData() instanceof GridData data) data.exclude = !visible;
    }

    private static void setAccessibleName(Control control, String name) {
        control.getAccessible().addAccessibleListener(new AccessibleAdapter() {
            @Override
            public void getName(AccessibleEvent event) {
                event.result = name;
            }
        });
    }

    @Override
    public void setFocus() {
        if (xWell != null && !xWell.isDisposed() && xWell.isVisible()) {
            xWell.setFocus();
        } else if (content != null && !content.isDisposed()) {
            content.setFocus();
        }
    }

    @Override
    public void dispose() {
        if (resultSetService != null) resultSetService.close();
        visualizerSessionManager.clear();
        DBeaverSqlDialectService.clearQuoteString();
        super.dispose();
    }
}
