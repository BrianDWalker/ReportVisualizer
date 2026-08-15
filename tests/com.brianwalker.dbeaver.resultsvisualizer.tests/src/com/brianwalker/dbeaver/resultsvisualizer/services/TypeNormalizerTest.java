/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import static org.junit.Assert.assertEquals;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import java.sql.Types;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.junit.Test;

public class TypeNormalizerTest {

    @Test
    public void mapsJdbcTypes() {
        assertNormalized(NormalizedDataType.BOOLEAN, Types.BOOLEAN, "BOOLEAN");
        assertNormalized(NormalizedDataType.INTEGER, Types.BIGINT, "BIGINT");
        assertNormalized(NormalizedDataType.DECIMAL, Types.DECIMAL, "DECIMAL");
        assertNormalized(NormalizedDataType.NUMBER, Types.DOUBLE, "DOUBLE");
        assertNormalized(NormalizedDataType.STRING, Types.VARCHAR, "VARCHAR");
        assertNormalized(NormalizedDataType.DATE, Types.DATE, "DATE");
        assertNormalized(NormalizedDataType.TIME, Types.TIME_WITH_TIMEZONE, "TIME WITH TIME ZONE");
        assertNormalized(NormalizedDataType.DATETIME, Types.TIMESTAMP, "TIMESTAMP");
    }

    @Test
    public void fallsBackToDBeaverDataKindsForVendorTypes() {
        assertEquals(NormalizedDataType.BOOLEAN,
                TypeNormalizer.normalize(Types.OTHER, "FLAG", DBPDataKind.BOOLEAN, null));
        assertEquals(NormalizedDataType.STRING,
                TypeNormalizer.normalize(Types.OTHER, "UUID", DBPDataKind.STRING, null));
        assertEquals(NormalizedDataType.DATE,
                TypeNormalizer.normalize(Types.OTHER, "LOCAL_DATE", DBPDataKind.DATETIME, null));
        assertEquals(NormalizedDataType.TIME,
                TypeNormalizer.normalize(Types.OTHER, "LOCAL_TIME", DBPDataKind.DATETIME, null));
        assertEquals(NormalizedDataType.DATETIME,
                TypeNormalizer.normalize(Types.OTHER, "TIMESTAMP", DBPDataKind.DATETIME, null));
        assertEquals(NormalizedDataType.DATETIME,
                TypeNormalizer.normalize(Types.VARCHAR, "DATETIME", DBPDataKind.DATETIME, null));
        assertEquals(NormalizedDataType.DATETIME,
                TypeNormalizer.normalize(Types.VARCHAR, "DATETIME", DBPDataKind.STRING, null));
        assertEquals(NormalizedDataType.BOOLEAN,
                TypeNormalizer.normalize(Types.INTEGER, "BOOLEAN", DBPDataKind.BOOLEAN, null));
    }

    @Test
    public void leavesUnsupportedValuesAsOther() {
        assertEquals(NormalizedDataType.OTHER,
                TypeNormalizer.normalize(Types.BINARY, "BLOB", DBPDataKind.BINARY, null));
        assertEquals(NormalizedDataType.OTHER,
                TypeNormalizer.normalize(Types.OTHER, "mystery", null, null));
    }

    private static void assertNormalized(
            NormalizedDataType expected, int jdbcType, String typeName) {
        assertEquals(expected,
                TypeNormalizer.normalize(jdbcType, typeName, DBPDataKind.UNKNOWN, null));
    }
}
