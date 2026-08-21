/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.junit.Test;

public class DateHierarchyNavigatorTest {
    @Test public void directlySelectsOriginalAndEveryCalendarLevel() {
        List<DateHierarchySelection> state = List.of();
        for (DateHierarchyLevel level : DateHierarchyLevel.values()) {
            state = DateHierarchyNavigator.select(state, 2, level);
            assertEquals(level, DateHierarchyNavigator.levelFor(state, 2));
        }
        state = DateHierarchyNavigator.select(state, 2, null);
        assertNull(DateHierarchyNavigator.levelFor(state, 2));
    }

    @Test public void drillsDownFromOriginalThroughDayAndBackUp() {
        DateHierarchyLevel level = null;
        level = DateHierarchyNavigator.drillDown(level); assertEquals(DateHierarchyLevel.YEAR, level);
        level = DateHierarchyNavigator.drillDown(level); assertEquals(DateHierarchyLevel.QUARTER, level);
        level = DateHierarchyNavigator.drillDown(level); assertEquals(DateHierarchyLevel.MONTH, level);
        level = DateHierarchyNavigator.drillDown(level); assertEquals(DateHierarchyLevel.DAY, level);
        assertEquals(DateHierarchyLevel.DAY, DateHierarchyNavigator.drillDown(level));
        level = DateHierarchyNavigator.drillUp(level); assertEquals(DateHierarchyLevel.MONTH, level);
        level = DateHierarchyNavigator.drillUp(level); assertEquals(DateHierarchyLevel.QUARTER, level);
        level = DateHierarchyNavigator.drillUp(level); assertEquals(DateHierarchyLevel.YEAR, level);
        assertNull(DateHierarchyNavigator.drillUp(level));
    }

    @Test public void changingOneFieldPreservesOtherHierarchySelections() {
        List<DateHierarchySelection> state = List.of(
                new DateHierarchySelection(1, DateHierarchyLevel.YEAR),
                new DateHierarchySelection(2, DateHierarchyLevel.MONTH));
        state = DateHierarchyNavigator.select(state, 2, DateHierarchyLevel.DAY);
        assertEquals(DateHierarchyLevel.YEAR, DateHierarchyNavigator.levelFor(state, 1));
        assertEquals(DateHierarchyLevel.DAY, DateHierarchyNavigator.levelFor(state, 2));
    }
}
