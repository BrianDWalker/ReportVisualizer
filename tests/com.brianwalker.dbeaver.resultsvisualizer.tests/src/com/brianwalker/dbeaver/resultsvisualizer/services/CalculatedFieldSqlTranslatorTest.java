/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import static org.junit.Assert.assertEquals;

import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import com.brianwalker.dbeaver.resultsvisualizer.model.Nullability;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import java.util.List;
import org.junit.Test;

public class CalculatedFieldSqlTranslatorTest {
    @Test public void expandsCalculatedFieldsToSourceSqlIncludingDependencies() {
        CalculatedFieldSqlTranslator translator = new CalculatedFieldSqlTranslator(
                List.of(column(0, "revenue"), column(1, "cost")),
                List.of(new CalculatedFieldDefinition("Profit", "[revenue] - [cost]"),
                        new CalculatedFieldDefinition("Margin", "[Profit] / [revenue]")));

        assertEquals("(\"revenue\" - \"cost\")", translator.expressionFor("Profit"));
        assertEquals("((\"revenue\" - \"cost\") / \"revenue\")",
                translator.expressionFor("Margin"));
    }

    @Test public void translatesLocalMinAndMaxToScalarSqlFunctions() {
        CalculatedFieldSqlTranslator translator = new CalculatedFieldSqlTranslator(
                List.of(column(0, "amount")), List.of());
        assertEquals("LEAST(\"amount\", 1000) + GREATEST(\"amount\", 0)",
                translator.translateExpression("MIN([amount], 1000) + MAX([amount], 0)"));
    }

    @Test public void translatesModToPercentOperatorForCrossDialectSupport() {
        CalculatedFieldSqlTranslator translator = new CalculatedFieldSqlTranslator(
                List.of(column(0, "amount")), List.of());
        assertEquals("((\"amount\") % (10))", translator.translateExpression("MOD([amount], 10)"));
    }

    @Test public void translatesIfToStandardCaseWhenExpression() {
        CalculatedFieldSqlTranslator translator = new CalculatedFieldSqlTranslator(
                List.of(column(0, "amount")), List.of());
        assertEquals("CASE WHEN \"amount\" > 0 THEN \"amount\" ELSE 0 END",
                translator.translateExpression("IF([amount] > 0, [amount], 0)"));
    }

    @Test public void leavesComparisonsAndBooleanOperatorsAsStandardSql() {
        CalculatedFieldSqlTranslator translator = new CalculatedFieldSqlTranslator(
                List.of(column(0, "amount"), column(1, "cost")), List.of());
        assertEquals("\"amount\" > 0 AND \"cost\" <= 10 OR NOT \"amount\" = \"cost\"",
                translator.translateExpression("[amount] > 0 AND [cost] <= 10 OR NOT [amount] = [cost]"));
        assertEquals("COALESCE(\"amount\", \"cost\")",
                translator.translateExpression("COALESCE([amount], [cost])"));
        assertEquals("NULLIF(\"amount\", \"cost\")",
                translator.translateExpression("NULLIF([amount], [cost])"));
    }

    @Test public void translatesNestedModAndIfCallsUsingParenAwareArgumentSplitting() {
        CalculatedFieldSqlTranslator translator = new CalculatedFieldSqlTranslator(
                List.of(column(0, "amount"), column(1, "cost")), List.of());
        assertEquals("CASE WHEN ((\"amount\") % (\"cost\" + 1)) > 0 THEN \"amount\" ELSE \"cost\" END",
                translator.translateExpression("IF(MOD([amount], [cost] + 1) > 0, [amount], [cost])"));
    }

    @Test public void usesConfiguredIdentifierQuoteStyleAcrossCalculatedFieldSql() {
        DBeaverSqlDialectService.installQuoteString("`");
        try {
            CalculatedFieldSqlTranslator translator = new CalculatedFieldSqlTranslator(
                    List.of(column(0, "order id"), column(1, "select")),
                    List.of(new CalculatedFieldDefinition("Quoted", "[order id] + [select]")));
            assertEquals("(`order id` + `select`)", translator.expressionFor("Quoted"));
        } finally {
            DBeaverSqlDialectService.clearQuoteString();
        }
    }

    private static ResultColumn column(int index, String name) {
        return new ResultColumn(index, name, name, 3, "NUMBER",
                NormalizedDataType.NUMBER, Nullability.NULLABLE);
    }
}
