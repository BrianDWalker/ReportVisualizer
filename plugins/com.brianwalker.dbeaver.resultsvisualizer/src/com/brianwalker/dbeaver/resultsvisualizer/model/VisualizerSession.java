/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.services.AggregateQuery;
import com.brianwalker.dbeaver.resultsvisualizer.services.CustomSqlDimension;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.MatrixDisplayOptions;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SortRule;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationConfiguration;
import java.util.List;
import java.util.Objects;

/** Immutable state captured for one DBeaver result/controller session. */
public record VisualizerSession(
        String id,
        String resultIdentity,
        ResultSetSnapshot baseSnapshot,
        ResultSetSnapshot aggregateSnapshot,
        VisualizationConfiguration configuration,
        MatrixDisplayOptions matrixOptions,
        List<CalculatedFieldDefinition> calculatedFields,
        List<SlicerDefinition> slicers,
        List<SortRule> sortRules,
        List<CustomSqlDimension> customSqlDimensions,
        AggregateQuery aggregateQuery,
        DisplayMode displayMode) {

    public enum DisplayMode {
        SOURCE,
        AGGREGATE
    }

    public VisualizerSession {
        id = Objects.requireNonNull(id, "id");
        resultIdentity = Objects.requireNonNullElse(resultIdentity, "");
        calculatedFields = calculatedFields == null ? List.of() : List.copyOf(calculatedFields);
        slicers = slicers == null ? List.of() : List.copyOf(slicers);
        sortRules = sortRules == null ? List.of() : List.copyOf(sortRules);
        customSqlDimensions = customSqlDimensions == null ? List.of() : List.copyOf(customSqlDimensions);
        matrixOptions = matrixOptions == null ? MatrixDisplayOptions.DEFAULT : matrixOptions;
        displayMode = displayMode == null ? DisplayMode.SOURCE : displayMode;
    }

    public static VisualizerSession empty(String id, String resultIdentity) {
        return new VisualizerSession(id, resultIdentity, null, null, null,
                MatrixDisplayOptions.DEFAULT, List.of(), List.of(), List.of(), List.of(), null,
                DisplayMode.SOURCE);
    }

    public VisualizerSession withBaseSnapshot(ResultSetSnapshot snapshot) {
        return new VisualizerSession(id, resultIdentity, snapshot, aggregateSnapshot,
                configuration, matrixOptions, calculatedFields, slicers, sortRules,
                customSqlDimensions, aggregateQuery, displayMode);
    }

    public VisualizerSession withAggregateSnapshot(ResultSetSnapshot snapshot) {
        return new VisualizerSession(id, resultIdentity, baseSnapshot, snapshot,
                configuration, matrixOptions, calculatedFields, slicers, sortRules,
                customSqlDimensions, aggregateQuery, displayMode);
    }

    public VisualizerSession withConfiguration(VisualizationConfiguration configuration) {
        return new VisualizerSession(id, resultIdentity, baseSnapshot, aggregateSnapshot,
                configuration, matrixOptions, calculatedFields, slicers, sortRules,
                customSqlDimensions, aggregateQuery, displayMode);
    }

    public VisualizerSession withMatrixOptions(MatrixDisplayOptions matrixOptions) {
        return new VisualizerSession(id, resultIdentity, baseSnapshot, aggregateSnapshot,
                configuration, matrixOptions, calculatedFields, slicers, sortRules,
                customSqlDimensions, aggregateQuery, displayMode);
    }

    public VisualizerSession withCalculatedFields(List<CalculatedFieldDefinition> value) {
        return new VisualizerSession(id, resultIdentity, baseSnapshot, aggregateSnapshot,
                configuration, matrixOptions, value, slicers, sortRules,
                customSqlDimensions, aggregateQuery, displayMode);
    }

    public VisualizerSession withSlicers(List<SlicerDefinition> value) {
        return new VisualizerSession(id, resultIdentity, baseSnapshot, aggregateSnapshot,
                configuration, matrixOptions, calculatedFields, value, sortRules,
                customSqlDimensions, aggregateQuery, displayMode);
    }

    public VisualizerSession withSortRules(List<SortRule> value) {
        return new VisualizerSession(id, resultIdentity, baseSnapshot, aggregateSnapshot,
                configuration, matrixOptions, calculatedFields, slicers, value,
                customSqlDimensions, aggregateQuery, displayMode);
    }

    public VisualizerSession withCustomSqlDimensions(List<CustomSqlDimension> value) {
        return new VisualizerSession(id, resultIdentity, baseSnapshot, aggregateSnapshot,
                configuration, matrixOptions, calculatedFields, slicers, sortRules,
                value, aggregateQuery, displayMode);
    }

    public VisualizerSession withAggregateQuery(AggregateQuery value) {
        return new VisualizerSession(id, resultIdentity, baseSnapshot, aggregateSnapshot,
                configuration, matrixOptions, calculatedFields, slicers, sortRules,
                customSqlDimensions, value, displayMode);
    }

    public VisualizerSession withDisplayMode(DisplayMode value) {
        return new VisualizerSession(id, resultIdentity, baseSnapshot, aggregateSnapshot,
                configuration, matrixOptions, calculatedFields, slicers, sortRules,
                customSqlDimensions, aggregateQuery, value);
    }
}
