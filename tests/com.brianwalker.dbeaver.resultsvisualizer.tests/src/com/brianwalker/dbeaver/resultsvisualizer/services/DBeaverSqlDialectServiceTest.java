/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import static org.junit.Assert.assertEquals;

import com.brianwalker.dbeaver.resultsvisualizer.services.DBeaverSqlDialectService.QuoteStyle;
import org.junit.After;
import org.junit.Test;

/**
 * Exercises {@link DBeaverSqlDialectService} identifier-quoting through its public
 * abstraction, using literal {@code String[][]} quote-string tables copied from real
 * DBeaver dialect implementations (see {@code MySQLDialect.MYSQL_QUOTE_STRINGS} and
 * {@code SQLServerDialectBase.SQLSERVER_QUOTE_STRINGS} in the DBeaver source) rather than
 * a hand-built vendor switch, so this does not depend on constructing a full live
 * {@code SQLDialect} implementation inside a unit test.
 */
public class DBeaverSqlDialectServiceTest {

    @After
    public void clearActiveQuote() {
        DBeaverSqlDialectService.clearQuoteString();
    }

    @Test public void quotesANormalIdentifierWithTheAnsiDefault() {
        assertEquals("\"region\"", DBeaverSqlDialectService.quoteIdentifier("region"));
    }

    @Test public void quotesAnIdentifierContainingSpacesWithTheAnsiDefault() {
        assertEquals("\"order id\"", DBeaverSqlDialectService.quoteIdentifier("order id"));
    }

    @Test public void quotesAReservedWordLikeIdentifierUnconditionally() {
        // The plugin quotes every generated identifier rather than selectively quoting
        // only identifiers a dialect would otherwise require quoting; this keeps generated
        // SQL correct without depending on the dialect's own quoting-necessity heuristic.
        assertEquals("\"select\"", DBeaverSqlDialectService.quoteIdentifier("select"));
    }

    @Test public void escapesAnEmbeddedAnsiQuoteCharacterByDoublingIt() {
        assertEquals("\"O\"\"Brien\"", DBeaverSqlDialectService.quoteIdentifier("O\"Brien"));
    }

    @Test public void derivesBacktickQuotingFromAMySqlStyleDialectQuoteTable() {
        // Mirrors org.jkiss.dbeaver.ext.mysql.model.MySQLDialect.MYSQL_QUOTE_STRINGS.
        String[][] mysqlQuoteStrings = {{"`", "`"}, {"\"", "\""}};
        QuoteStyle style = DBeaverSqlDialectService.quoteStyleFromQuoteStrings(mysqlQuoteStrings);
        DBeaverSqlDialectService.installQuoteStyle(style);
        assertEquals("`region`", DBeaverSqlDialectService.quoteIdentifier("region"));
        assertEquals("`O``Brien`", DBeaverSqlDialectService.quoteIdentifier("O`Brien"));
    }

    @Test public void derivesAsymmetricBracketQuotingFromASqlServerStyleDialectQuoteTable() {
        // Mirrors org.jkiss.dbeaver.ext.mssql.model.SQLServerDialectBase.SQLSERVER_QUOTE_STRINGS.
        // SQL Server's bracket quoting is asymmetric (open "[" != close "]"), which a
        // single-character quote assumption cannot represent.
        String[][] sqlServerQuoteStrings = {{"[", "]"}, {"\"", "\""}};
        QuoteStyle style = DBeaverSqlDialectService.quoteStyleFromQuoteStrings(sqlServerQuoteStrings);
        DBeaverSqlDialectService.installQuoteStyle(style);
        assertEquals("[region]", DBeaverSqlDialectService.quoteIdentifier("region"));
        // Embedded-quote escaping doubles only the closing bracket, matching T-SQL's own
        // bracket-identifier escaping convention.
        assertEquals("[order]]id]", DBeaverSqlDialectService.quoteIdentifier("order]id"));
    }

    @Test public void fallsBackToAnsiWhenTheDialectDeclaresNoQuoteStrings() {
        QuoteStyle style = DBeaverSqlDialectService.quoteStyleFromQuoteStrings(new String[0][0]);
        assertEquals(DBeaverSqlDialectService.defaultQuoteStyle(), style);
    }

    @Test public void fallsBackToAnsiWhenTheQuoteStringsTableIsNull() {
        QuoteStyle style = DBeaverSqlDialectService.quoteStyleFromQuoteStrings(null);
        assertEquals(DBeaverSqlDialectService.defaultQuoteStyle(), style);
    }

    @Test public void fallsBackToAnsiWhenTheDialectIsUnavailable() {
        QuoteStyle style = DBeaverSqlDialectService.quoteStyleFromDialect(null);
        assertEquals(DBeaverSqlDialectService.defaultQuoteStyle(), style);
    }

    @Test public void generatesDirectRewriteSqlUsingTheActiveQuoteStyle() {
        DBeaverSqlDialectService.installQuoteString("`");
        try {
            String strategySql = AggregateQueryBuilder.buildQuery(
                    "SELECT order_dt, revenue, region FROM sales",
                    snapshot(),
                    new com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationConfiguration(
                            com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartType.BAR, 0, 1, 2,
                            com.brianwalker.dbeaver.resultsvisualizer.visualization.Aggregation.SUM),
                    java.util.List.of(AggregateQueryBuilder.resultDimension(snapshot(), 2)),
                    java.util.List.of(), java.util.List.of()).sql();
            assertEquals(true, strategySql.contains("`region`"));
            assertEquals(true, strategySql.contains("FROM sales"));
        } finally {
            DBeaverSqlDialectService.clearQuoteString();
        }
    }

    @Test public void generatesDerivedTableFallbackSqlUsingTheActiveQuoteStyle() {
        DBeaverSqlDialectService.installQuoteString("`");
        try {
            String strategySql = AggregateQueryBuilder.buildQuery(
                    "SELECT order_dt, revenue FROM sales UNION SELECT order_dt, revenue FROM archived_sales",
                    snapshot(),
                    new com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationConfiguration(
                            com.brianwalker.dbeaver.resultsvisualizer.visualization.ChartType.BAR, 0, 1, 2,
                            com.brianwalker.dbeaver.resultsvisualizer.visualization.Aggregation.SUM),
                    java.util.List.of(AggregateQueryBuilder.resultDimension(snapshot(), 0)),
                    java.util.List.of(AggregateQueryBuilder.resultDimension(snapshot(), 2)),
                    java.util.List.of()).sql();
            assertEquals(true, strategySql.contains("rv_source"));
            assertEquals(true, strategySql.contains("`order_dt`"));
        } finally {
            DBeaverSqlDialectService.clearQuoteString();
        }
    }

    private static com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot snapshot() {
        return new com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot("sales", java.util.List.of(
                column(0, "order_dt", com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType.DATETIME),
                column(1, "revenue", com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType.DECIMAL),
                column(2, "region", com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType.STRING)),
                java.util.List.of(), 0, false, java.time.Instant.now());
    }

    private static com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn column(
            int index, String name, com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType type) {
        return new com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn(index, name, name,
                java.sql.Types.OTHER, type.name(), type,
                com.brianwalker.dbeaver.resultsvisualizer.model.Nullability.NULLABLE);
    }
}
