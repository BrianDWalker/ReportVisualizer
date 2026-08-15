/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import com.brianwalker.dbeaver.resultsvisualizer.calculatedfields.CalculatedFieldDefinition;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
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

    /**
     * Translates a validated calculated-field formula into standard SQL. Most of the local
     * formula grammar (COALESCE, NULLIF, comparisons, AND/OR/NOT, arithmetic) is already valid
     * ANSI SQL syntax once field references are substituted, so it passes through unchanged.
     * A few constructs need rewriting because they aren't scalar SQL, or aren't universally
     * supported across the dialects DBeaver connects to:
     * <ul>
     *   <li>{@code MIN(a, b)}/{@code MAX(a, b)} are SQL aggregate functions, not two-argument
     *       scalar functions, so they are rewritten to {@code LEAST}/{@code GREATEST}.</li>
     *   <li>{@code MOD(a, b)} is rewritten to {@code (a % b)} since some dialects (for example
     *       SQL Server and SQLite) don't provide a {@code MOD} function, only the {@code %}
     *       operator.</li>
     *   <li>{@code IF(condition, whenTrue, whenFalse)} is rewritten to the equivalent
     *       {@code CASE WHEN condition THEN whenTrue ELSE whenFalse END}, since {@code IF(...)}
     *       as an expression is a MySQL-specific extension, not standard SQL.</li>
     * </ul>
     * {@code LOG} is intentionally left as a pass-through: its argument order and base (natural
     * vs. base-10) differ across dialects, and resolving that safely for every supported
     * dialect is out of scope here; formulas relying on multi-argument {@code LOG} inside the
     * Source Query should be verified against the target database.
     */
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
        String sql = translated.toString()
                .replaceAll("(?i)\\bMIN\\s*\\(", "LEAST(")
                .replaceAll("(?i)\\bMAX\\s*\\(", "GREATEST(");
        sql = rewriteFunctionCalls(sql, "MOD", 2,
                arguments -> "((" + arguments.get(0) + ") % (" + arguments.get(1) + "))");
        sql = rewriteFunctionCalls(sql, "IF", 3,
                arguments -> "CASE WHEN " + arguments.get(0) + " THEN " + arguments.get(1)
                        + " ELSE " + arguments.get(2) + " END");
        return sql;
    }

    /**
     * Rewrites every top-level call to {@code functionName(...)} in {@code sql} using
     * {@code rewriter}, provided the call has exactly {@code expectedArgumentCount} arguments at
     * paren-depth 0 (nested parentheses/commas inside an argument are not split). Calls with an
     * unexpected arity, or unbalanced parentheses, are left untouched rather than guessed at, so
     * a malformed match can never corrupt the generated SQL.
     */
    private static String rewriteFunctionCalls(String sql, String functionName, int expectedArgumentCount,
            Function<List<String>, String> rewriter) {
        Pattern call = Pattern.compile("(?i)\\b" + functionName + "\\s*\\(");
        Matcher matcher = call.matcher(sql);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find(lastEnd)) {
            int argumentsStart = matcher.end();
            int depth = 1;
            int index = argumentsStart;
            while (index < sql.length() && depth > 0) {
                char character = sql.charAt(index);
                if (character == '(') depth++;
                else if (character == ')') { depth--; if (depth == 0) break; }
                index++;
            }
            if (depth != 0) {
                result.append(sql, lastEnd, matcher.end());
                lastEnd = matcher.end();
                continue;
            }
            List<String> arguments = splitTopLevelArguments(sql, argumentsStart, index);
            result.append(sql, lastEnd, matcher.start());
            result.append(arguments.size() == expectedArgumentCount
                    ? rewriter.apply(arguments) : sql.substring(matcher.start(), index + 1));
            lastEnd = index + 1;
        }
        result.append(sql.substring(lastEnd));
        return result.toString();
    }

    private static List<String> splitTopLevelArguments(String sql, int argumentsStart, int argumentsEnd) {
        List<String> arguments = new ArrayList<>();
        int depth = 0;
        int start = argumentsStart;
        for (int index = argumentsStart; index < argumentsEnd; index++) {
            char character = sql.charAt(index);
            if (character == '(') depth++;
            else if (character == ')') depth--;
            else if (character == ',' && depth == 0) {
                arguments.add(sql.substring(start, index).trim());
                start = index + 1;
            }
        }
        arguments.add(sql.substring(start, argumentsEnd).trim());
        return arguments;
    }

    private static String key(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    private static String quote(String identifier) {
        return DBeaverSqlDialectService.quoteIdentifier(identifier);
    }
}
