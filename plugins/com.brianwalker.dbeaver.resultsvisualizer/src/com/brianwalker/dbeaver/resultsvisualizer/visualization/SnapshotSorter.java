/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultRow;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Stable, null-safe ordered sorting for local result snapshots. */
public final class SnapshotSorter {
    private SnapshotSorter() {}

    public static ResultSetSnapshot apply(ResultSetSnapshot snapshot, List<SortRule> rules) {
        if (rules.isEmpty()) return snapshot;
        List<Key> keys = rules.stream().map(rule -> new Key(findColumn(snapshot, rule.fieldName()), rule))
                .filter(key -> key.index() >= 0).toList();
        if (keys.isEmpty()) return snapshot;
        List<ResultRow> rows = new ArrayList<>(snapshot.rows());
        rows.sort((left, right) -> compare(left, right, keys));
        return new ResultSetSnapshot(snapshot.sourceName(), snapshot.columns(), rows,
                snapshot.availableRowCount(), snapshot.truncated(), snapshot.capturedAt());
    }

    private static int compare(ResultRow left, ResultRow right, List<Key> keys) {
        for (Key key : keys) {
            int compared = compareValues(value(left, key.index()), value(right, key.index()));
            if (key.rule().direction() == SortRule.Direction.DESC) compared = -compared;
            if (compared != 0) return compared;
        }
        return 0;
    }

    private static Object value(ResultRow row, int index) {
        return index < row.values().size() ? row.values().get(index) : null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compareValues(Object left, Object right) {
        if (left == right) return 0;
        if (left == null) return -1;
        if (right == null) return 1;
        BigDecimal leftNumber = number(left), rightNumber = number(right);
        if (leftNumber != null && rightNumber != null) return leftNumber.compareTo(rightNumber);
        if (left instanceof Comparable comparable && left.getClass().isInstance(right)
                && !(left instanceof TemporalAccessor)) return comparable.compareTo(right);
        return left.toString().compareToIgnoreCase(right.toString());
    }

    private static BigDecimal number(Object value) {
        if (!(value instanceof Number)) return null;
        try { return new BigDecimal(value.toString()); }
        catch (NumberFormatException error) { return null; }
    }

    private static int findColumn(ResultSetSnapshot snapshot, String name) {
        for (int index = 0; index < snapshot.columns().size(); index++) {
            if (snapshot.columns().get(index).displayName().equalsIgnoreCase(name)) return index;
        }
        return -1;
    }

    private record Key(int index, SortRule rule) {}
}
