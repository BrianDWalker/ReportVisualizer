/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import java.util.Locale;

/**
 * Centralizes dialect-sensitive SQL quoting and source-query rewrite strategy decisions.
 *
 * <p><b>Known limitation:</b> identifier quoting is currently a fixed ANSI double-quote
 * (the SQL-92 standard, and what PostgreSQL, Oracle, DB2, SQLite, and SQL Server with
 * QUOTED_IDENTIFIER ON all accept). It is not yet derived from the active DBeaver
 * datasource's {@code SQLDialect}, so a MySQL/MariaDB connection running in
 * non-ANSI-quotes mode (backtick identifiers) is not correctly supported. Deriving this
 * from the live execution context requires threading a dialect/quoting capability from
 * {@code ResultSetService} down through {@link AggregateQueryBuilder}, which is left as
 * follow-up work rather than a broad signature change here.</p>
 *
 * <p>The direct-vs-derived rewrite decision below deliberately does not rely on a plain
 * top-level regex split: a naive "first FROM after SELECT" match can be fooled by a
 * parenthesized scalar/correlated subquery in the select list (e.g.
 * {@code SELECT (SELECT MAX(x) FROM y) AS z FROM t}), and a plain substring scan for
 * disallowed keywords ignores paren nesting. {@link #topLevelFromIndex(String)} instead
 * walks the SQL text tracking parenthesis depth and single-quoted string literals so only
 * a truly top-level {@code FROM} qualifies for {@link QueryStrategy#DIRECT_REWRITE}.</p>
 */
public final class DBeaverSqlDialectService {
    public enum QueryStrategy {
        DIRECT_REWRITE,
        DERIVED_TABLE_FALLBACK
    }

    private static final String[] DISQUALIFYING_KEYWORDS = {
            "UNION", "INTERSECT", "EXCEPT", "JOIN", "WHERE", "GROUP BY", "HAVING",
            "ORDER BY", "LIMIT", "FETCH", "TOP", "WITH"
    };

    private DBeaverSqlDialectService() {
    }

    public static String quoteIdentifier(String identifier) {
        return identifier == null || identifier.isBlank()
                ? "\"\""
                : "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    public static QueryStrategy strategyFor(String sourceSql) {
        String normalized = safeSingleStatement(sourceSql);
        if (normalized == null) return QueryStrategy.DERIVED_TABLE_FALLBACK;
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("SELECT")) return QueryStrategy.DERIVED_TABLE_FALLBACK;

        int fromIndex = topLevelFromIndex(normalized);
        if (fromIndex < 0) return QueryStrategy.DERIVED_TABLE_FALLBACK;

        String afterFrom = normalized.substring(fromIndex + 4);
        String afterFromUpper = afterFrom.toUpperCase(Locale.ROOT);
        for (String keyword : DISQUALIFYING_KEYWORDS) {
            if (containsWord(afterFromUpper, keyword)) return QueryStrategy.DERIVED_TABLE_FALLBACK;
        }
        // A bare "FROM table" reference never needs parentheses; a "(" after FROM means a
        // subquery, function-table, or join expression that isn't safe to rewrite blindly.
        if (afterFrom.contains("(")) return QueryStrategy.DERIVED_TABLE_FALLBACK;
        return QueryStrategy.DIRECT_REWRITE;
    }

    public static String sourceClause(String sourceSql, QueryStrategy strategy) {
        String normalized = safeSingleStatement(sourceSql);
        if (normalized == null) normalized = sourceSql == null ? "" : sourceSql.trim();
        if (strategy == QueryStrategy.DIRECT_REWRITE) {
            int fromIndex = topLevelFromIndex(normalized);
            if (fromIndex >= 0) return normalized.substring(fromIndex + 4).trim();
        }
        return "(\n" + normalized + "\n) rv_source";
    }

    /**
     * Trims the source SQL and rejects it (returns {@code null}) unless it is safely a
     * single statement: no SQL comments (which could hide terminators or keywords from
     * this lightweight scan) and no statement terminator other than one optional trailing
     * semicolon.
     */
    private static String safeSingleStatement(String sourceSql) {
        if (sourceSql == null || sourceSql.isBlank()) return null;
        String trimmed = sourceSql.trim();
        if (trimmed.contains("--") || trimmed.contains("/*")) return null;
        String withoutTerminator = trimmed.endsWith(";")
                ? trimmed.substring(0, trimmed.length() - 1).trim() : trimmed;
        if (withoutTerminator.contains(";")) return null;
        return withoutTerminator;
    }

    /**
     * Index of the first top-level (paren-depth 0, outside string literals) {@code FROM}
     * keyword, or -1 if none exists. Used both to decide the rewrite strategy and to
     * extract the source-table clause, so both agree on exactly the same split point.
     */
    private static int topLevelFromIndex(String sql) {
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inString) {
                if (c == '\'') {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') { i++; continue; }
                    inString = false;
                }
                continue;
            }
            if (c == '\'') { inString = true; continue; }
            if (c == '(') { depth++; continue; }
            if (c == ')') { depth--; continue; }
            if (depth == 0 && isWordAt(sql, i, "FROM")) return i;
        }
        return -1;
    }

    private static boolean containsWord(String upperHaystack, String upperWord) {
        int index = upperHaystack.indexOf(upperWord);
        while (index >= 0) {
            boolean leftBoundary = index == 0 || !Character.isLetterOrDigit(upperHaystack.charAt(index - 1));
            int end = index + upperWord.length();
            boolean rightBoundary = end >= upperHaystack.length() || !Character.isLetterOrDigit(upperHaystack.charAt(end));
            if (leftBoundary && rightBoundary) return true;
            index = upperHaystack.indexOf(upperWord, index + 1);
        }
        return false;
    }

    private static boolean isWordAt(String sql, int index, String word) {
        int end = index + word.length();
        if (end > sql.length() || !sql.regionMatches(true, index, word, 0, word.length())) return false;
        boolean leftBoundary = index == 0 || !Character.isLetterOrDigit(sql.charAt(index - 1));
        boolean rightBoundary = end >= sql.length() || !Character.isLetterOrDigit(sql.charAt(end));
        return leftBoundary && rightBoundary;
    }
}

