/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import java.math.BigDecimal;
import java.util.Objects;

/** Typed, immutable slicer value that preserves SQL NULL semantics and numeric equivalence. */
public record SlicerValue(Object rawValue, String displayValue, NormalizedDataType type, boolean sqlNull) {
    public SlicerValue {
        displayValue = Objects.requireNonNullElse(displayValue, "");
        type = Objects.requireNonNullElse(type, NormalizedDataType.STRING);
    }

    public static SlicerValue nullValue() {
        return new SlicerValue(null, "(null)", NormalizedDataType.STRING, true);
    }

    public static SlicerValue fromDisplayValue(String value) {
        if (value == null) return nullValue();
        if ("(null)".equals(value)) return new SlicerValue("(null)", value, NormalizedDataType.STRING, false);
        return new SlicerValue(value, value, NormalizedDataType.STRING, false);
    }

    public static SlicerValue fromValue(Object value) {
        if (value == null) return nullValue();
        String display = value.toString();
        NormalizedDataType dataType = inferType(value);
        return new SlicerValue(value, display, dataType, false);
    }

    public boolean matches(Object candidate) {
        return matches(fromValue(candidate));
    }

    public boolean matches(SlicerValue candidate) {
        if (candidate == null) return sqlNull;
        if (sqlNull) return candidate.sqlNull();
        if (candidate.sqlNull) return false;
        if (rawValue == null) return "(null)".equals(candidate.displayValue());
        if (candidate.rawValue == null) return "(null)".equals(displayValue);
        if (candidate.rawValue instanceof Number && rawValue instanceof Number) {
            return new BigDecimal(rawValue.toString()).compareTo(new BigDecimal(candidate.rawValue.toString())) == 0;
        }
        if (rawValue instanceof Boolean && candidate.rawValue instanceof Boolean) {
            return rawValue.equals(candidate.rawValue);
        }
        return rawValue.equals(candidate.rawValue) || rawValue.toString().equals(candidate.rawValue.toString());
    }

    /** Preset decoding may normalize an equivalent JDBC value to another Java representation. */
    @Override public boolean equals(Object other) {
        return other instanceof SlicerValue value && sqlNull == value.sqlNull && matches(value);
    }

    @Override public int hashCode() {
        if (sqlNull) return 31;
        if (rawValue instanceof Number) {
            try { return new BigDecimal(rawValue.toString()).stripTrailingZeros().toPlainString().hashCode(); }
            catch (NumberFormatException ignored) { /* fall through to the stable display form */ }
        }
        return rawValue == null ? displayValue.hashCode() : rawValue.toString().hashCode();
    }

    public String sqlLiteral() {
        if (sqlNull) return "NULL";
        if (rawValue == null) return "'(null)'";
        if (rawValue instanceof String) return "'" + rawValue.toString().replace("'", "''") + "'";
        if (rawValue instanceof Boolean) return ((Boolean) rawValue) ? "TRUE" : "FALSE";
        if (rawValue instanceof Number) return rawValue.toString();
        return "'" + rawValue.toString().replace("'", "''") + "'";
    }

    private static NormalizedDataType inferType(Object value) {
        if (value instanceof Number number) {
            if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
                return NormalizedDataType.INTEGER;
            }
            return NormalizedDataType.NUMBER;
        }
        if (value instanceof Boolean) return NormalizedDataType.BOOLEAN;
        if (value instanceof java.util.Date) return NormalizedDataType.DATE;
        if (value instanceof java.time.temporal.TemporalAccessor) return NormalizedDataType.DATETIME;
        return NormalizedDataType.STRING;
    }
}
