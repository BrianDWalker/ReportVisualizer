/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import java.sql.Types;
import java.util.Locale;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;

/** Maps JDBC and DBeaver-specific types onto stable visualization types. */
public final class TypeNormalizer {
    private TypeNormalizer() {
    }

    public static NormalizedDataType normalize(DBDAttributeBinding attribute) {
        return normalize(attribute.getTypeID(), attribute.getTypeName(),
                attribute.getDataKind(), attribute.getScale());
    }

    static NormalizedDataType normalize(
            int typeId, String typeName, DBPDataKind dataKind, Integer scale) {
        // Some drivers expose semantic date/time or boolean columns through a
        // generic/string JDBC code. DBeaver's resolved data kind is more useful
        // for visualization in those two unambiguous cases.
        if (dataKind == DBPDataKind.DATETIME) {
            return normalizeDateTimeName(typeName);
        }
        if (dataKind == DBPDataKind.BOOLEAN) {
            return NormalizedDataType.BOOLEAN;
        }
        if (isTemporalTypeName(typeName)) {
            return normalizeDateTimeName(typeName);
        }
        return switch (typeId) {
            case Types.BIT, Types.BOOLEAN -> NormalizedDataType.BOOLEAN;
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT ->
                    NormalizedDataType.INTEGER;
            case Types.NUMERIC, Types.DECIMAL -> NormalizedDataType.DECIMAL;
            case Types.FLOAT, Types.REAL, Types.DOUBLE -> NormalizedDataType.NUMBER;
            case Types.DATE -> NormalizedDataType.DATE;
            case Types.TIME, Types.TIME_WITH_TIMEZONE -> NormalizedDataType.TIME;
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> NormalizedDataType.DATETIME;
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR,
                    Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR,
                    Types.CLOB, Types.NCLOB -> NormalizedDataType.STRING;
            default -> normalizeDataKind(typeName, dataKind, scale);
        };
    }

    private static boolean isTemporalTypeName(String typeName) {
        String name = typeName == null ? "" : typeName.toUpperCase(Locale.ROOT);
        return name.contains("DATE") || name.contains("TIME");
    }

    private static NormalizedDataType normalizeDataKind(
            String typeName, DBPDataKind dataKind, Integer scale) {
        if (dataKind == null) {
            return NormalizedDataType.OTHER;
        }
        return switch (dataKind) {
            case BOOLEAN -> NormalizedDataType.BOOLEAN;
            case NUMERIC -> scale != null && scale == 0
                    ? NormalizedDataType.NUMBER : NormalizedDataType.DECIMAL;
            case STRING, ROWID -> NormalizedDataType.STRING;
            case DATETIME -> normalizeDateTimeName(typeName);
            default -> NormalizedDataType.OTHER;
        };
    }

    private static NormalizedDataType normalizeDateTimeName(String typeName) {
        String name = typeName == null ? "" : typeName.toUpperCase(Locale.ROOT);
        boolean containsDate = name.contains("DATE");
        boolean containsTime = name.contains("TIME");
        if (containsDate && !containsTime) {
            return NormalizedDataType.DATE;
        }
        if (containsTime && !containsDate && !name.contains("STAMP")) {
            return NormalizedDataType.TIME;
        }
        return NormalizedDataType.DATETIME;
    }
}
