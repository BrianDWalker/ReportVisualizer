/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Expands validated local formulas into equivalent SQL over the original source query. */
public final class CalculatedFieldSqlTranslator {
    private static final Pattern FIELD = Pattern.compile("\\[([^]\\r\\n]+)]");
    private final Map<String, String> expressions = new LinkedHashMap<>();

    public CalculatedFieldSqlTranslator(List<ResultColumn> sourceColumns,
            List<CalculatedFieldDefinition> definitions) {
        for (ResultColumn column : sourceColumns) {
            String sql = quote(column.displayName());
            expressions.putIfAbsent(key(column.name()), sql);
            expressions.putIfAbsent(key(column.label()), sql);
            expressions.putIfAbsent(key(column.displayName()), sql);
        }
        for (CalculatedFieldDefinition definition : definitions) {
            expressions.put(key(definition.name()), "(" + translateExpression(definition.expression()) + ")");
        }
    }

    public String expressionFor(String fieldName) {
        String expression = expressions.get(key(fieldName));
        if (expression == null) throw new IllegalArgumentException("Unknown SQL field: " + fieldName);
        return expression;
    }

    public String translateExpression(String formula) {
        Matcher matcher = FIELD.matcher(formula);
        StringBuffer translated = new StringBuffer();
        while (matcher.find()) {
            String replacement = expressions.get(key(matcher.group(1)));
            if (replacement == null) {
                throw new IllegalArgumentException("Formula field is unavailable in the source SQL: ["
                        + matcher.group(1).trim() + "]");
            }
            matcher.appendReplacement(translated, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(translated);
        return translated.toString()
                .replaceAll("(?i)\\bMIN\\s*\\(", "LEAST(")
                .replaceAll("(?i)\\bMAX\\s*\\(", "GREATEST(");
    }

    private static String key(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    private static String quote(String identifier) {
        return DBeaverSqlDialectService.quoteIdentifier(identifier);
    }
}
