/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.model;

import java.util.Objects;

/** Immutable, DBeaver-independent result-column metadata. */
public record ResultColumn(
        int index,
        String name,
        String label,
        int databaseTypeId,
        String databaseTypeName,
        NormalizedDataType normalizedType,
        Nullability nullability) {

    public ResultColumn {
        if (index < 0) {
            throw new IllegalArgumentException("Column index must be zero or greater.");
        }
        name = Objects.requireNonNullElse(name, "");
        label = Objects.requireNonNullElse(label, name);
        databaseTypeName = Objects.requireNonNullElse(databaseTypeName, "");
        normalizedType = Objects.requireNonNull(normalizedType, "normalizedType");
        nullability = Objects.requireNonNull(nullability, "nullability");
    }

    public String displayName() {
        return label.isBlank() ? name : label;
    }
}
