/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultRow;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Applies slicers to an immutable snapshot without touching DBeaver's result. */
public final class SnapshotSlicer {
    private SnapshotSlicer() {}

    public static ResultSetSnapshot apply(ResultSetSnapshot snapshot, List<SlicerDefinition> slicers) {
        if (slicers.isEmpty()) return snapshot;
        List<ResultRow> rows = snapshot.rows().stream().filter(row -> slicers.stream()
                .allMatch(slicer -> matches(snapshot, row, slicer))).toList();
        return new ResultSetSnapshot(snapshot.sourceName(), snapshot.columns(), rows,
                rows.size(), false, snapshot.capturedAt(), snapshot.configuredRowLimit());
    }

    private static boolean matches(ResultSetSnapshot snapshot, ResultRow row, SlicerDefinition slicer) {
        int index = -1;
        for (int i = 0; i < snapshot.columns().size(); i++) {
            ResultColumn column = snapshot.columns().get(i);
            if (column.displayName().equalsIgnoreCase(slicer.fieldName())) { index = i; break; }
        }
        if (index < 0 || index >= row.values().size()) return false;
        Object value = row.values().get(index);
        if (!slicer.isCategorySelection()) return predicateMatches(value, slicer);
        SlicerValue candidate = SlicerValue.fromValue(value);
        return slicer.selectedValues().stream().anyMatch(item -> item.matches(candidate));
    }

    private static boolean predicateMatches(Object rawValue, SlicerDefinition slicer) {
        SlicerOperator operator = slicer.operator();
        if (operator == SlicerOperator.IS_NULL) return rawValue == null;
        if (operator == SlicerOperator.IS_NOT_NULL) return rawValue != null;
        if (rawValue == null) return false;
        LocalDate date = DateHierarchyProjector.date(rawValue);
        if (operator.isRelativeDate() || isDateOperator(operator)
                || ((operator == SlicerOperator.BETWEEN || operator == SlicerOperator.NOT_BETWEEN)
                && date != null && date(slicer.firstValue()) != null)) {
            if (date == null) return false;
            return dateMatches(date, slicer);
        }
        BigDecimal candidate = number(rawValue);
        BigDecimal first = number(slicer.firstValue());
        BigDecimal second = number(slicer.secondValue());
        if (candidate == null || first == null || (operator.valueCount() == 2 && second == null)) return false;
        int firstComparison = candidate.compareTo(first);
        return switch (operator) {
            case EQUALS -> firstComparison == 0;
            case NOT_EQUALS -> firstComparison != 0;
            case GREATER_THAN -> firstComparison > 0;
            case GREATER_THAN_OR_EQUAL -> firstComparison >= 0;
            case LESS_THAN -> firstComparison < 0;
            case LESS_THAN_OR_EQUAL -> firstComparison <= 0;
            case BETWEEN -> candidate.compareTo(first) >= 0 && candidate.compareTo(second) <= 0;
            case NOT_BETWEEN -> candidate.compareTo(first) < 0 || candidate.compareTo(second) > 0;
            default -> false;
        };
    }

    private static boolean isDateOperator(SlicerOperator operator) {
        return switch (operator) {
            case BEFORE, AFTER, ON_OR_BEFORE, ON_OR_AFTER, BETWEEN, NOT_BETWEEN -> true;
            default -> false;
        };
    }

    private static boolean dateMatches(LocalDate candidate, SlicerDefinition slicer) {
        SlicerOperator operator = slicer.operator();
        LocalDate today = LocalDate.now();
        if (operator.isRelativeDate()) {
            int count = operator.valueCount() == 0 ? 0 : positiveCount(slicer.firstValue());
            return switch (operator) {
                case THIS_MONTH -> candidate.getYear() == today.getYear()
                        && candidate.getMonth() == today.getMonth();
                case THIS_QUARTER -> candidate.getYear() == today.getYear()
                        && quarter(candidate) == quarter(today);
                case THIS_YEAR -> candidate.getYear() == today.getYear();
                case LAST_N_DAYS -> !candidate.isBefore(today.minusDays(count)) && !candidate.isAfter(today);
                case LAST_N_MONTHS -> !candidate.isBefore(today.minusMonths(count)) && !candidate.isAfter(today);
                case LAST_N_YEARS -> !candidate.isBefore(today.minusYears(count)) && !candidate.isAfter(today);
                default -> false;
            };
        }
        LocalDate first = date(slicer.firstValue());
        LocalDate second = date(slicer.secondValue());
        if (first == null || (operator.valueCount() == 2 && second == null)) return false;
        int comparison = candidate.compareTo(first);
        return switch (operator) {
            case BEFORE -> comparison < 0;
            case AFTER -> comparison > 0;
            case ON_OR_BEFORE -> comparison <= 0;
            case ON_OR_AFTER -> comparison >= 0;
            case BETWEEN -> !candidate.isBefore(first) && !candidate.isAfter(second);
            case NOT_BETWEEN -> candidate.isBefore(first) || candidate.isAfter(second);
            default -> false;
        };
    }

    private static int quarter(LocalDate date) { return (date.getMonthValue() - 1) / 3; }
    private static int positiveCount(String value) {
        try { int count = Integer.parseInt(value); return count > 0 ? count : -1; }
        catch (NumberFormatException ignored) { return -1; }
    }
    private static BigDecimal number(Object value) {
        try { return new BigDecimal(value.toString().trim()); }
        catch (RuntimeException ignored) { return null; }
    }
    private static LocalDate date(String value) {
        try { return LocalDate.parse(value); }
        catch (RuntimeException ignored) {
            try { return java.time.LocalDateTime.parse(value).toLocalDate(); }
            catch (RuntimeException ignoredAgain) { return null; }
        }
    }
}
