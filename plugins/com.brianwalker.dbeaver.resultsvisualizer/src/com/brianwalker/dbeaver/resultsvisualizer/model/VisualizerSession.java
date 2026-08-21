/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.services.AggregateExecutionRequest;
import com.brianwalker.dbeaver.resultsvisualizer.services.AggregateQuery;
import com.brianwalker.dbeaver.resultsvisualizer.services.CustomSqlDimension;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.MatrixDisplayOptions;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.DateHierarchySelection;
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
        VisualizationConfiguration sourceConfigurationBeforeAggregate,
        MatrixDisplayOptions matrixOptions,
        List<CalculatedFieldDefinition> calculatedFields,
        List<SlicerDefinition> slicers,
        List<DateHierarchySelection> dateHierarchies,
        List<SortRule> sortRules,
        List<CustomSqlDimension> customSqlDimensions,
        AggregateQuery aggregateQuery,
        AggregateExecutionRequest pendingAggregateRequest,
        long sourceGeneration,
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
        dateHierarchies = dateHierarchies == null ? List.of() : List.copyOf(dateHierarchies);
        sortRules = sortRules == null ? List.of() : List.copyOf(sortRules);
        customSqlDimensions = customSqlDimensions == null ? List.of() : List.copyOf(customSqlDimensions);
        matrixOptions = matrixOptions == null ? MatrixDisplayOptions.DEFAULT : matrixOptions;
        displayMode = displayMode == null ? DisplayMode.SOURCE : displayMode;
        if (sourceGeneration < 0) throw new IllegalArgumentException("Source generation cannot be negative.");
    }

    public static VisualizerSession empty(String id, String resultIdentity) {
        return new VisualizerSession(id, resultIdentity, null, null, null, null,
                MatrixDisplayOptions.DEFAULT, List.of(), List.of(), List.of(), List.of(), List.of(), null,
                null, 0, DisplayMode.SOURCE);
    }

    public VisualizerSession withBaseSnapshot(ResultSetSnapshot snapshot) {
        return copy(snapshot, aggregateSnapshot, configuration, sourceConfigurationBeforeAggregate,
                matrixOptions, calculatedFields, slicers, dateHierarchies, sortRules, customSqlDimensions,
                aggregateQuery, pendingAggregateRequest, sourceGeneration, displayMode);
    }

    public VisualizerSession withAggregateSnapshot(ResultSetSnapshot snapshot) {
        return copy(baseSnapshot, snapshot, configuration, sourceConfigurationBeforeAggregate,
                matrixOptions, calculatedFields, slicers, dateHierarchies, sortRules, customSqlDimensions,
                aggregateQuery, pendingAggregateRequest, sourceGeneration, displayMode);
    }

    public VisualizerSession withConfiguration(VisualizationConfiguration configuration) {
        return copy(baseSnapshot, aggregateSnapshot, configuration, sourceConfigurationBeforeAggregate,
                matrixOptions, calculatedFields, slicers, dateHierarchies, sortRules, customSqlDimensions,
                aggregateQuery, pendingAggregateRequest, sourceGeneration, displayMode);
    }

    public VisualizerSession withSourceConfigurationBeforeAggregate(VisualizationConfiguration value) {
        return copy(baseSnapshot, aggregateSnapshot, configuration, value, matrixOptions,
                calculatedFields, slicers, sortRules, customSqlDimensions, aggregateQuery,
                pendingAggregateRequest, sourceGeneration, displayMode);
    }

    public VisualizerSession withMatrixOptions(MatrixDisplayOptions matrixOptions) {
        return copy(baseSnapshot, aggregateSnapshot, configuration, sourceConfigurationBeforeAggregate,
                matrixOptions, calculatedFields, slicers, sortRules, customSqlDimensions,
                aggregateQuery, pendingAggregateRequest, sourceGeneration, displayMode);
    }

    public VisualizerSession withCalculatedFields(List<CalculatedFieldDefinition> value) {
        return copy(baseSnapshot, aggregateSnapshot, configuration, sourceConfigurationBeforeAggregate,
                matrixOptions, value, slicers, sortRules, customSqlDimensions, aggregateQuery,
                pendingAggregateRequest, sourceGeneration, displayMode);
    }

    public VisualizerSession withSlicers(List<SlicerDefinition> value) {
        return copy(baseSnapshot, aggregateSnapshot, configuration, sourceConfigurationBeforeAggregate,
                matrixOptions, calculatedFields, value, sortRules, customSqlDimensions,
                aggregateQuery, pendingAggregateRequest, sourceGeneration, displayMode);
    }

    public VisualizerSession withDateHierarchies(List<DateHierarchySelection> value) {
        return new VisualizerSession(id, resultIdentity, baseSnapshot, aggregateSnapshot,
                configuration, sourceConfigurationBeforeAggregate, matrixOptions, calculatedFields,
                slicers, value, sortRules, customSqlDimensions, aggregateQuery,
                pendingAggregateRequest, sourceGeneration, displayMode);
    }

    public VisualizerSession withSortRules(List<SortRule> value) {
        return copy(baseSnapshot, aggregateSnapshot, configuration, sourceConfigurationBeforeAggregate,
                matrixOptions, calculatedFields, slicers, value, customSqlDimensions,
                aggregateQuery, pendingAggregateRequest, sourceGeneration, displayMode);
    }

    public VisualizerSession withCustomSqlDimensions(List<CustomSqlDimension> value) {
        return copy(baseSnapshot, aggregateSnapshot, configuration, sourceConfigurationBeforeAggregate,
                matrixOptions, calculatedFields, slicers, sortRules, value, aggregateQuery,
                pendingAggregateRequest, sourceGeneration, displayMode);
    }

    public VisualizerSession withAggregateQuery(AggregateQuery value) {
        return copy(baseSnapshot, aggregateSnapshot, configuration, sourceConfigurationBeforeAggregate,
                matrixOptions, calculatedFields, slicers, sortRules, customSqlDimensions, value,
                pendingAggregateRequest, sourceGeneration, displayMode);
    }

    public VisualizerSession withPendingAggregateRequest(AggregateExecutionRequest value) {
        return copy(baseSnapshot, aggregateSnapshot, configuration, sourceConfigurationBeforeAggregate,
                matrixOptions, calculatedFields, slicers, sortRules, customSqlDimensions,
                aggregateQuery, value, sourceGeneration, displayMode);
    }

    public VisualizerSession withSourceGeneration(long value) {
        return copy(baseSnapshot, aggregateSnapshot, configuration, sourceConfigurationBeforeAggregate,
                matrixOptions, calculatedFields, slicers, sortRules, customSqlDimensions,
                aggregateQuery, pendingAggregateRequest, value, displayMode);
    }

    public VisualizerSession withDisplayMode(DisplayMode value) {
        return copy(baseSnapshot, aggregateSnapshot, configuration, sourceConfigurationBeforeAggregate,
                matrixOptions, calculatedFields, slicers, sortRules, customSqlDimensions,
                aggregateQuery, pendingAggregateRequest, sourceGeneration, value);
    }

    private VisualizerSession copy(ResultSetSnapshot base, ResultSetSnapshot aggregate,
            VisualizationConfiguration currentConfiguration,
            VisualizationConfiguration preAggregateConfiguration, MatrixDisplayOptions options,
            List<CalculatedFieldDefinition> fields, List<SlicerDefinition> currentSlicers,
            List<DateHierarchySelection> currentHierarchies, List<SortRule> currentSortRules,
            List<CustomSqlDimension> sqlDimensions, AggregateQuery currentAggregateQuery,
            AggregateExecutionRequest pendingRequest, long generation, DisplayMode mode) {
        return new VisualizerSession(id, resultIdentity, base, aggregate, currentConfiguration,
                preAggregateConfiguration, options, fields, currentSlicers, currentHierarchies,
                currentSortRules, sqlDimensions, currentAggregateQuery, pendingRequest, generation, mode);
    }

    private VisualizerSession copy(ResultSetSnapshot base, ResultSetSnapshot aggregate,
            VisualizationConfiguration currentConfiguration,
            VisualizationConfiguration preAggregateConfiguration, MatrixDisplayOptions options,
            List<CalculatedFieldDefinition> fields, List<SlicerDefinition> currentSlicers,
            List<SortRule> currentSortRules, List<CustomSqlDimension> sqlDimensions,
            AggregateQuery currentAggregateQuery, AggregateExecutionRequest pendingRequest,
            long generation, DisplayMode mode) {
        return new VisualizerSession(id, resultIdentity, base, aggregate, currentConfiguration,
                preAggregateConfiguration, options, fields, currentSlicers, dateHierarchies, currentSortRules,
                sqlDimensions, currentAggregateQuery, pendingRequest, generation, mode);
    }
}
