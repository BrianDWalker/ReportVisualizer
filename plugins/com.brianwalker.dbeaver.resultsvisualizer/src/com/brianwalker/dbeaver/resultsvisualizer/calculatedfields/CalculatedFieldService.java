/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.calculatedfields;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import com.brianwalker.dbeaver.resultsvisualizer.model.Nullability;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultRow;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Validates definitions and projects their values beside the original result fields. */
public final class CalculatedFieldService {

    public void validate(ResultSetSnapshot snapshot, CalculatedFieldDefinition definition)
            throws CalculatedFieldException {
        if (snapshot.columns().stream().anyMatch(column ->
                column.displayName().equalsIgnoreCase(definition.name())
                || column.name().equalsIgnoreCase(definition.name()))) {
            throw new CalculatedFieldException(
                    "A field named '" + definition.name() + "' already exists.");
        }
        ExpressionCompiler.compile(definition.expression(), snapshot.columns());
    }

    public CalculatedFieldProjection project(ResultSetSnapshot base,
            List<CalculatedFieldDefinition> definitions) {
        ResultSetSnapshot current = base;
        List<String> errors = new ArrayList<>();
        for (CalculatedFieldDefinition definition : definitions) {
            try {
                validate(current, definition);
                current = append(current, definition,
                        ExpressionCompiler.compile(definition.expression(), current.columns()));
            } catch (CalculatedFieldException error) {
                errors.add(definition.name() + ": " + error.getMessage());
            }
        }
        return new CalculatedFieldProjection(current, errors);
    }

    private static ResultSetSnapshot append(ResultSetSnapshot snapshot,
            CalculatedFieldDefinition definition, CompiledExpression expression) {
        int index = snapshot.columns().size();
        List<ResultColumn> columns = new ArrayList<>(snapshot.columns());
        columns.add(new ResultColumn(index, definition.name(),
                definition.name() + " (Calculated)", Types.DOUBLE, "CALCULATED",
                NormalizedDataType.NUMBER, Nullability.NULLABLE));

        List<ResultRow> rows = new ArrayList<>(snapshot.rows().size());
        for (ResultRow row : snapshot.rows()) {
            List<Object> values = new ArrayList<>(row.values());
            values.add(expression.evaluate(values));
            rows.add(new ResultRow(row.sourceIndex(), values));
        }
        return new ResultSetSnapshot(snapshot.sourceName(), columns, rows,
                snapshot.availableRowCount(), snapshot.truncated(), snapshot.capturedAt(),
                snapshot.configuredRowLimit());
    }
}
