/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.calculatedfields;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import com.brianwalker.dbeaver.resultsvisualizer.model.Nullability;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class ExpressionCompilerTest {

    @Test
    public void evaluatesFieldsConstantsPrecedenceAndParentheses() throws Exception {
        CompiledExpression expression = ExpressionCompiler.compile(
                "([revenue] - [cost]) * 100 / [revenue]", columns());

        assertEquals(25.0, expression.evaluate(List.of(200, 150)), 0.0001);
    }

    @Test
    public void supportsAbsRoundAndUnaryOperators() throws Exception {
        CompiledExpression expression = ExpressionCompiler.compile(
                "ROUND(ABS(-[cost]) + 0.49)", columns());

        assertEquals(151.0, expression.evaluate(List.of(200, 150.5)), 0.0001);
    }

    @Test
    public void supportsGuidedNumericFunctionsAndRoundPrecision() throws Exception {
        CompiledExpression expression = ExpressionCompiler.compile(
                "MAX(ROUND(POWER(SQRT([revenue]), 2) / 3, 2), MIN([cost], 10))",
                columns());

        assertEquals(30.33, expression.evaluate(List.of(91, 12)), 0.0001);
        assertEquals(10.0, expression.evaluate(List.of(9, 12)), 0.0001);
        assertNull(ExpressionCompiler.compile("SQRT(-[revenue])", columns())
                .evaluate(List.of(9, 12)));
    }

    @Test
    public void propagatesNullIncompatibleAndDivideByZeroAsNull() throws Exception {
        CompiledExpression divide = ExpressionCompiler.compile("[revenue] / [cost]", columns());

        assertNull(divide.evaluate(Arrays.asList(null, 10)));
        assertNull(divide.evaluate(List.of("not numeric", 10)));
        assertNull(divide.evaluate(List.of(10, 0)));
    }

    @Test
    public void reportsUnknownFieldsAndIncompleteExpressions() throws Exception {
        assertCompileError("[reveneu] - [cost]", "Unknown field: [reveneu]");
        assertCompileError("[revenue] /", "Expression is incomplete");
    }

    @Test
    public void rejectsAnythingOutsideRestrictedGrammar() throws Exception {
        assertCompileError("Runtime([revenue])", "Unknown function: RUNTIME");
        assertCompileError("[revenue]; DROP TABLE sales", "Unexpected ';'");
    }

    private static void assertCompileError(String expression, String expected) throws Exception {
        try {
            ExpressionCompiler.compile(expression, columns());
            fail("Expected calculated-field validation failure");
        } catch (CalculatedFieldException error) {
            org.junit.Assert.assertTrue(error.getMessage(), error.getMessage().contains(expected));
        }
    }

    private static List<ResultColumn> columns() {
        return List.of(column(0, "revenue"), column(1, "cost"));
    }

    private static ResultColumn column(int index, String name) {
        return new ResultColumn(index, name, name, Types.DECIMAL, "DECIMAL",
                NormalizedDataType.DECIMAL, Nullability.NULLABLE);
    }
}
