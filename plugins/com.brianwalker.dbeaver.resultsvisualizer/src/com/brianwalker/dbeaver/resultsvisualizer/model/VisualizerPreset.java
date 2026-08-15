/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.*;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

/** Complete, portable visualization state saved against immutable result metadata. */
public record VisualizerPreset(String name, String sourceSignature, ChartType chartType,
        List<Integer> xColumnIndexes, int valueColumnIndex, List<Integer> seriesColumnIndexes,
        Aggregation aggregation, Double yAxisMaximum, MatrixDisplayOptions matrixOptions,
        List<Integer> matrixValueColumnIndexes, List<SlicerDefinition> slicers,
        List<SortRule> sortRules, List<CalculatedFieldDefinition> calculatedFields) {
    private static final int FORMAT_VERSION = 2;

    public VisualizerPreset {
        name = Objects.requireNonNullElse(name, "").trim(); sourceSignature = Objects.requireNonNullElse(sourceSignature, "");
        chartType = Objects.requireNonNull(chartType, "chartType"); xColumnIndexes = indexes(xColumnIndexes);
        seriesColumnIndexes = indexes(seriesColumnIndexes); aggregation = Objects.requireNonNull(aggregation, "aggregation");
        matrixOptions = Objects.requireNonNullElse(matrixOptions, MatrixDisplayOptions.DEFAULT); matrixValueColumnIndexes = indexes(matrixValueColumnIndexes);
        slicers = List.copyOf(Objects.requireNonNullElse(slicers, List.of())); sortRules = List.copyOf(Objects.requireNonNullElse(sortRules, List.of()));
        calculatedFields = List.copyOf(Objects.requireNonNullElse(calculatedFields, List.of()));
    }

    public static String sourceSignature(ResultSetSnapshot snapshot) {
        StringBuilder builder = new StringBuilder(snapshot.sourceName()).append('|');
        for (ResultColumn column : snapshot.columns()) builder.append(column.displayName()).append(':').append(column.normalizedType().name()).append(';');
        return builder.toString();
    }
    public VisualizationConfiguration toConfiguration() { return new VisualizationConfiguration(chartType, xColumnIndexes, valueColumnIndex, seriesColumnIndexes, aggregation, yAxisMaximum); }
    public boolean matches(ResultSetSnapshot snapshot) { return sourceSignature.equals(sourceSignature(snapshot)); }

    /** Versioned binary data avoids delimiter collisions in names, formulas, and slicer values. */
    public String serialize() {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(FORMAT_VERSION); text(out, name); text(out, sourceSignature); text(out, chartType.name()); ints(out, xColumnIndexes); out.writeInt(valueColumnIndex);
            ints(out, seriesColumnIndexes); text(out, aggregation.name()); out.writeBoolean(yAxisMaximum != null); if (yAxisMaximum != null) out.writeDouble(yAxisMaximum);
            out.writeBoolean(matrixOptions.rowTotals()); out.writeBoolean(matrixOptions.columnTotals()); out.writeBoolean(matrixOptions.subtotals()); ints(out, matrixValueColumnIndexes);
            out.writeInt(slicers.size()); for (SlicerDefinition slicer : slicers) { text(out, slicer.fieldName()); out.writeInt(slicer.selectedValues().size()); for (SlicerValue value : slicer.selectedValues()) slicer(out, value); }
            out.writeInt(sortRules.size()); for (SortRule rule : sortRules) { text(out, rule.fieldName()); text(out, rule.direction().name()); }
            out.writeInt(calculatedFields.size()); for (CalculatedFieldDefinition field : calculatedFields) { text(out, field.name()); text(out, field.expression()); }
            out.flush(); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException impossible) { throw new IllegalStateException("Unable to serialize preset.", impossible); }
    }

    /** Returns null for corrupt or unsupported entries; workspace preferences are untrusted input. */
    public static VisualizerPreset deserialize(String fallbackName, String serialized) {
        if (serialized == null || serialized.isBlank()) return null;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(Base64.getUrlDecoder().decode(serialized)))) {
            if (in.readInt() != FORMAT_VERSION) return null;
            VisualizerPreset preset = new VisualizerPreset(read(in), read(in), ChartType.valueOf(read(in)), ints(in), in.readInt(), ints(in), Aggregation.valueOf(read(in)),
                    in.readBoolean() ? in.readDouble() : null, new MatrixDisplayOptions(in.readBoolean(), in.readBoolean(), in.readBoolean()), ints(in), slicers(in), rules(in), fields(in));
            return in.available() == 0 ? preset : null;
        } catch (IllegalArgumentException | IOException error) { return null; }
    }

    private static List<SlicerDefinition> slicers(DataInputStream in) throws IOException {
        List<SlicerDefinition> result = new ArrayList<>(); for (int i = 0, size = count(in); i < size; i++) { String field = read(in); Set<SlicerValue> values = new LinkedHashSet<>(); for (int j = 0, valueCount = count(in); j < valueCount; j++) values.add(slicer(in)); result.add(SlicerDefinition.typed(field, values)); } return result;
    }
    private static List<SortRule> rules(DataInputStream in) throws IOException { List<SortRule> result = new ArrayList<>(); for (int i = 0, size = count(in); i < size; i++) result.add(new SortRule(read(in), SortRule.Direction.valueOf(read(in)))); return result; }
    private static List<CalculatedFieldDefinition> fields(DataInputStream in) throws IOException { List<CalculatedFieldDefinition> result = new ArrayList<>(); for (int i = 0, size = count(in); i < size; i++) result.add(new CalculatedFieldDefinition(read(in), read(in))); return result; }
    private static void slicer(DataOutputStream out, SlicerValue value) throws IOException { out.writeBoolean(value.sqlNull()); text(out, value.type().name()); text(out, value.rawValue() == null ? "" : value.rawValue().toString()); text(out, value.displayValue()); }
    private static SlicerValue slicer(DataInputStream in) throws IOException { boolean nullValue = in.readBoolean(); NormalizedDataType type = NormalizedDataType.valueOf(read(in)); String raw = read(in); String display = read(in); Object typed = switch (type) { case INTEGER, NUMBER -> new BigDecimal(raw); case BOOLEAN -> Boolean.valueOf(raw); default -> raw; }; return new SlicerValue(nullValue ? null : typed, display, type, nullValue); }
    private static void ints(DataOutputStream out, List<Integer> values) throws IOException { out.writeInt(values.size()); for (int value : values) out.writeInt(value); }
    private static List<Integer> ints(DataInputStream in) throws IOException { List<Integer> result = new ArrayList<>(); for (int i = 0, size = count(in); i < size; i++) result.add(in.readInt()); return List.copyOf(result); }
    private static int count(DataInputStream in) throws IOException { int value = in.readInt(); if (value < 0 || value > 10_000) throw new IOException("Invalid preset collection size."); return value; }
    private static void text(DataOutputStream out, String value) throws IOException { out.writeUTF(value == null ? "" : value); }
    private static String read(DataInputStream in) throws IOException { return in.readUTF(); }
    private static List<Integer> indexes(List<Integer> values) { List<Integer> result = List.copyOf(Objects.requireNonNullElse(values, List.of())); if (result.stream().anyMatch(value -> value == null || value < 0)) throw new IllegalArgumentException("Invalid column index."); return result; }
}
