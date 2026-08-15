/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Centralizes dialect-sensitive SQL quoting and source-query rewrite strategy decisions. */
public final class DBeaverSqlDialectService {
    public enum QueryStrategy {
        DIRECT_REWRITE,
        DERIVED_TABLE_FALLBACK
    }

    private static final Pattern SIMPLE_SELECT_FROM = Pattern.compile(
            "(?is)^\\s*SELECT\\s+.*?\\s+FROM\\s+(.+?)\\s*$");

    private DBeaverSqlDialectService() {
    }

    public static String quoteIdentifier(String identifier) {
        return identifier == null || identifier.isBlank()
                ? "\"\""
                : "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    public static QueryStrategy strategyFor(String sourceSql) {
        if (sourceSql == null || sourceSql.isBlank()) {
            return QueryStrategy.DERIVED_TABLE_FALLBACK;
        }
        String trimmed = sourceSql.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("SELECT") || !upper.contains(" FROM ")) {
            return QueryStrategy.DERIVED_TABLE_FALLBACK;
        }
        if (upper.contains(" UNION ") || upper.contains(" INTERSECT ") || upper.contains(" EXCEPT ")
                || upper.contains(" WITH ") || upper.contains(" JOIN ") || upper.contains(" ORDER BY ")
                || upper.contains(" WHERE ") || upper.contains(" GROUP BY ") || upper.contains(" HAVING ")
                || upper.contains(" LIMIT ") || upper.contains(" FETCH ") || upper.contains(" TOP ")) {
            return QueryStrategy.DERIVED_TABLE_FALLBACK;
        }
        return QueryStrategy.DIRECT_REWRITE;
    }

    public static String sourceClause(String sourceSql, QueryStrategy strategy) {
        String normalized = sourceSql == null ? "" : sourceSql.trim();
        if (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        if (strategy == QueryStrategy.DIRECT_REWRITE) {
            Matcher matcher = SIMPLE_SELECT_FROM.matcher(normalized);
            return matcher.matches() ? matcher.group(1).trim() : normalized;
        }
        return "(\n" + normalized + "\n) rv_source";
    }
}
