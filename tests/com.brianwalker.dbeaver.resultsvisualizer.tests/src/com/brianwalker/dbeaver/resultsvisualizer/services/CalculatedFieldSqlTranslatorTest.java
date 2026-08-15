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
