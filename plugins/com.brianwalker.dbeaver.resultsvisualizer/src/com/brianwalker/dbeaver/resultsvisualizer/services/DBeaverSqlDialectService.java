/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import java.util.Locale;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;

/**
 * Centralizes dialect-sensitive SQL quoting and source-query rewrite strategy decisions.
 *
 * <p>Identifier quoting is derived directly from
 * {@link SQLDialect#getIdentifierQuoteStrings()} of the active DBeaver datasource when it
 * can be inferred from the current result-set context; if no live datasource information
 * is available, the service falls back to the ANSI double-quote default (the SQL-92
 * standard, and what PostgreSQL, Oracle, DB2, SQLite, and SQL Server with
 * QUOTED_IDENTIFIER ON accept). Reading the dialect's own declared quote-string pairs
 * (rather than round-tripping a sample identifier through
 * {@link SQLDialect#getQuotedIdentifier}) is required for correctness: DBeaver's
 * {@code AbstractSQLDialect.getQuotedIdentifier} only quotes a sample identifier when
 * {@code mustBeQuoted}/{@code forceQuotes} says quoting is actually needed, so an
 * inoffensive sample like {@code "x"} never reveals the dialect's real quote characters.
 * {@link SQLDialect#getIdentifierQuoteStrings()} also correctly exposes dialects with
 * asymmetric open/close pairs (for example SQL Server's {@code [}/{@code ]} bracket
 * quoting, see {@code SQLServerDialectBase.SQLSERVER_QUOTE_STRINGS} in the DBeaver
 * source), which a single-character quote assumption cannot represent. This lets the
 * plugin respect MySQL/MariaDB backtick and SQL Server bracket quoting without requiring
 * a broad rewrite of the aggregate builder API or a hardcoded per-vendor switch.</p>
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

    /** An open/close identifier-quote pair, e.g. {@code ("\"", "\"")} or {@code ("[", "]")}. */
    public record QuoteStyle(String open, String close) {
        public QuoteStyle {
            open = normalizePart(open);
            close = normalizePart(close);
        }

        private static String normalizePart(String part) {
            return part == null || part.isEmpty() ? "\"" : part;
        }
    }

    private static final QuoteStyle DEFAULT_QUOTE_STYLE = new QuoteStyle("\"", "\"");
    private static final ThreadLocal<QuoteStyle> ACTIVE_QUOTE =
            ThreadLocal.withInitial(() -> DEFAULT_QUOTE_STYLE);
    private static final String[] DISQUALIFYING_KEYWORDS = {
            "UNION", "INTERSECT", "EXCEPT", "JOIN", "WHERE", "GROUP BY", "HAVING",
            "ORDER BY", "LIMIT", "FETCH", "TOP", "WITH"
    };

    private DBeaverSqlDialectService() {
    }

    public static String defaultQuoteString() {
        return DEFAULT_QUOTE_STYLE.open();
    }

    public static QuoteStyle defaultQuoteStyle() {
        return DEFAULT_QUOTE_STYLE;
    }

    /** Convenience for a symmetric single-character quote style (e.g. {@code "\""} or {@code "`"}). */
    public static void installQuoteString(String quoteString) {
        installQuoteStyle(new QuoteStyle(quoteString, quoteString));
    }

    public static void installQuoteStyle(QuoteStyle style) {
        ACTIVE_QUOTE.set(style == null ? DEFAULT_QUOTE_STYLE : style);
    }

    public static void clearQuoteString() {
        ACTIVE_QUOTE.remove();
    }

    public static String quoteIdentifier(String identifier) {
        return quoteIdentifier(identifier, ACTIVE_QUOTE.get());
    }

    public static String quoteIdentifier(String identifier, DBPDataSource dataSource) {
        SQLDialect dialect = dataSource == null ? null : SQLUtils.getDialectFromDataSource(dataSource);
        return quoteIdentifier(identifier, quoteStyleFromDialect(dialect));
    }

    /**
     * Derives the active {@link QuoteStyle} from a live DBeaver {@link SQLDialect}, or the
     * ANSI default when {@code dialect} is {@code null} or does not declare a usable quote
     * pair.
     */
    public static QuoteStyle quoteStyleFromDialect(SQLDialect dialect) {
        return dialect == null ? DEFAULT_QUOTE_STYLE
                : quoteStyleFromQuoteStrings(dialect.getIdentifierQuoteStrings());
    }

    /**
     * Package-visible so it can be unit-tested with literal {@code String[][]} pairs
     * mirroring real dialects (e.g. MySQL's {@code {{"`","`"},{"\"","\""}}} or SQL
     * Server's {@code {{"[","]"},{"\"","\""}}}) without needing a full live
     * {@link SQLDialect} implementation in a unit test.
     */
    static QuoteStyle quoteStyleFromQuoteStrings(String[][] quoteStrings) {
        if (quoteStrings == null || quoteStrings.length == 0) return DEFAULT_QUOTE_STYLE;
        String[] primary = quoteStrings[0];
        if (primary == null || primary.length < 2
                || primary[0] == null || primary[0].isEmpty()
                || primary[1] == null || primary[1].isEmpty()) {
            return DEFAULT_QUOTE_STYLE;
        }
        return new QuoteStyle(primary[0], primary[1]);
    }

    private static String quoteIdentifier(String identifier, QuoteStyle style) {
        QuoteStyle normalized = style == null ? DEFAULT_QUOTE_STYLE : style;
        if (identifier == null || identifier.isBlank()) return normalized.open() + normalized.close();
        // Doubling the closing quote character handles both symmetric quoting (ANSI "..."
        // and MySQL `...`, where open == close) and asymmetric bracket quoting (SQL Server
        // [...]), matching how each convention escapes an embedded quote character.
        String escaped = identifier.replace(normalized.close(), normalized.close() + normalized.close());
        return normalized.open() + escaped + normalized.close();
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

