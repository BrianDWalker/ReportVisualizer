/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultRow;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Derives date hierarchy labels locally, without changing DBeaver's source result. */
public final class DateHierarchyProjector {
    private DateHierarchyProjector() { }

    public static ResultSetSnapshot apply(ResultSetSnapshot snapshot,
            List<DateHierarchySelection> selections) {
        if (selections == null || selections.isEmpty()) return snapshot;
        Map<Integer, DateHierarchyLevel> levels = selections.stream().collect(Collectors.toMap(
                DateHierarchySelection::fieldIndex, DateHierarchySelection::level, (left, right) -> right));
        List<ResultColumn> columns = new ArrayList<>(snapshot.columns());
        levels.forEach((index, level) -> {
            if (index >= 0 && index < columns.size()) {
                ResultColumn current = columns.get(index);
                columns.set(index, new ResultColumn(current.index(),
                        current.name(), current.displayName() + " [" + level + "]",
                        current.databaseTypeId(), current.databaseTypeName(),
                        current.normalizedType(), current.nullability()));
            }
        });
        List<ResultRow> rows = snapshot.rows().stream().map(row -> project(row, levels)).toList();
        return new ResultSetSnapshot(snapshot.sourceName(), columns, rows, snapshot.availableRowCount(),
                snapshot.truncated(), snapshot.capturedAt(), snapshot.configuredRowLimit());
    }

    private static ResultRow project(ResultRow row, Map<Integer, DateHierarchyLevel> levels) {
        List<Object> values = new ArrayList<>(row.values());
        levels.forEach((index, level) -> {
            if (index < values.size()) values.set(index, label(values.get(index), level));
        });
        return new ResultRow(row.sourceIndex(), values);
    }

    public static String label(Object value, DateHierarchyLevel level) {
        LocalDate date = date(value);
        if (date == null) return value == null ? "(null)" : value.toString();
        return switch (level) {
            case YEAR -> Integer.toString(date.getYear());
            case QUARTER -> date.getYear() + " Q" + ((date.getMonthValue() - 1) / 3 + 1);
            case MONTH -> String.format("%04d-%02d", date.getYear(), date.getMonthValue());
            case DAY -> date.toString();
        };
    }

    public static LocalDate date(Object value) {
        if (value instanceof LocalDate date) return date;
        if (value instanceof LocalDateTime dateTime) return dateTime.toLocalDate();
        if (value instanceof OffsetDateTime dateTime) return dateTime.toLocalDate();
        if (value instanceof ZonedDateTime dateTime) return dateTime.toLocalDate();
        if (value instanceof Instant instant) return instant.atZone(ZoneId.systemDefault()).toLocalDate();
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        if (value instanceof java.util.Date date) return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (value instanceof TemporalAccessor temporal) {
            try { return LocalDate.from(temporal); } catch (RuntimeException ignored) { return null; }
        }
        if (value instanceof CharSequence text) {
            try { return LocalDate.parse(text); }
            catch (RuntimeException ignored) {
                try { return LocalDateTime.parse(text).toLocalDate(); }
                catch (RuntimeException ignoredAgain) { return null; }
            }
        }
        return null;
    }
}
