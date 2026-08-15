/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Covers {@link DBeaverResultSetService#isTruncated} boundary semantics: DBeaver's own
 * "no limit configured" convention (non-positive limit), the exact-at-limit boundary
 * (must NOT count as truncated without other evidence), strictly-over-limit, and the
 * explicit {@code hasMoreData} override.
 */
public class DBeaverResultSetServiceTest {

    @Test
    public void notTruncatedWhenNoLimitIsConfigured() {
        assertFalse(DBeaverResultSetService.isTruncated(5_000, 0, false));
        assertFalse(DBeaverResultSetService.isTruncated(5_000, -1, false));
    }

    @Test
    public void notTruncatedWhenBelowTheConfiguredLimit() {
        assertFalse(DBeaverResultSetService.isTruncated(99, 100, false));
    }

    @Test
    public void notTruncatedWhenExactlyAtTheConfiguredLimitWithNoOtherEvidence() {
        assertFalse(DBeaverResultSetService.isTruncated(100, 100, false));
    }

    @Test
    public void truncatedWhenStrictlyAboveTheConfiguredLimit() {
        assertTrue(DBeaverResultSetService.isTruncated(101, 100, false));
    }

    @Test
    public void truncatedWhenDbeaverExplicitlyReportsMoreDataRegardlessOfCount() {
        assertTrue(DBeaverResultSetService.isTruncated(1, 100, true));
        assertTrue(DBeaverResultSetService.isTruncated(0, 0, true));
    }
}
