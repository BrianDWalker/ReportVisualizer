/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.temporal.TemporalAccessor;
import java.util.UUID;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;

/** Converts driver/DBeaver values into standalone Java values. */
final class SnapshotValueConverter {
    private SnapshotValueConverter() {
    }

    static Object convert(DBDAttributeBinding attribute,
            NormalizedDataType normalizedType, Object value) {
        if (value == null || DBUtils.isNullValue(value)) return null;
        if (value instanceof String || value instanceof Boolean
                || value instanceof BigDecimal || value instanceof BigInteger
                || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long
                || value instanceof Float || value instanceof Double) return value;
        if (value instanceof Character character) return character.toString();
        if (value instanceof Number number) {
            Object portableNumber = convertVendorNumber(number, normalizedType);
            if (portableNumber != null) return portableNumber;
        }
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        if (value instanceof Time time) return time.toLocalTime();
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();
        if (value instanceof TemporalAccessor || value instanceof java.time.Instant) return value;
        if (value instanceof java.util.Date date) return date.toInstant();
        if (value instanceof UUID uuid) return uuid.toString();
        if (value instanceof byte[] bytes) return "<binary: " + bytes.length + " bytes>";
        try {
            return attribute.getValueRenderer().getValueDisplayString(
                    attribute, value, DBDDisplayFormat.UI);
        } catch (RuntimeException ignored) {
            return String.valueOf(value);
        }
    }

    static Object convertPortable(NormalizedDataType normalizedType, Object value) {
        if (value == null || DBUtils.isNullValue(value)) return null;
        if (value instanceof String || value instanceof Boolean
                || value instanceof BigDecimal || value instanceof BigInteger
                || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long
                || value instanceof Float || value instanceof Double) return value;
        if (value instanceof Character character) return character.toString();
        if (value instanceof Number number) {
            Object portable = convertVendorNumber(number, normalizedType);
            if (portable != null) return portable;
        }
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        if (value instanceof Time time) return time.toLocalTime();
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();
        if (value instanceof TemporalAccessor || value instanceof java.time.Instant) return value;
        if (value instanceof java.util.Date date) return date.toInstant();
        if (value instanceof UUID uuid) return uuid.toString();
        if (value instanceof byte[] bytes) return "<binary: " + bytes.length + " bytes>";
        return String.valueOf(value);
    }

    private static Object convertVendorNumber(Number number, NormalizedDataType type) {
        try {
            BigDecimal decimal = new BigDecimal(number.toString());
            return type == NormalizedDataType.INTEGER ? decimal.toBigIntegerExact() : decimal;
        } catch (ArithmeticException | NumberFormatException ignored) {
            return null;
        }
    }
}
