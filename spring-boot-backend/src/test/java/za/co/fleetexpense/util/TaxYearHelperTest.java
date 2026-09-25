package za.co.fleetexpense.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

/**
 * Pure unit tests for TaxYearHelper.
 * No Spring context, no database, no external dependencies.
 */
class TaxYearHelperTest {

    @Test
    void testAssessmentYear2027_StartDate() {
        LocalDate start = TaxYearHelper.getAssessmentYearStart(2027);
        assertEquals(LocalDate.of(2026, 3, 1), start);
    }

    @Test
    void testAssessmentYear2027_EndExclusive() {
        LocalDate endExclusive = TaxYearHelper.getAssessmentYearEndExclusive(2027);
        assertEquals(LocalDate.of(2027, 3, 1), endExclusive);
    }

    @Test
    void testAssessmentYear2026_StartDate() {
        LocalDate start = TaxYearHelper.getAssessmentYearStart(2026);
        assertEquals(LocalDate.of(2025, 3, 1), start);
    }

    @Test
    void testAssessmentYear2026_EndExclusive() {
        LocalDate endExclusive = TaxYearHelper.getAssessmentYearEndExclusive(2026);
        assertEquals(LocalDate.of(2026, 3, 1), endExclusive);
    }

    @Test
    void testIsDateInAssessmentYear_Inside() {
        LocalDate date = LocalDate.of(2026, 6, 15);
        assertTrue(TaxYearHelper.isDateInAssessmentYear(date, 2027));
    }

    @Test
    void testIsDateInAssessmentYear_StartBoundary() {
        LocalDate date = LocalDate.of(2026, 3, 1);
        assertTrue(TaxYearHelper.isDateInAssessmentYear(date, 2027));
    }

    @Test
    void testIsDateInAssessmentYear_EndBoundary() {
        LocalDate date = LocalDate.of(2027, 2, 28);
        assertTrue(TaxYearHelper.isDateInAssessmentYear(date, 2027));
    }

    @Test
    void testIsDateInAssessmentYear_BeforeStart() {
        LocalDate date = LocalDate.of(2026, 2, 28);
        assertFalse(TaxYearHelper.isDateInAssessmentYear(date, 2027));
    }

    @Test
    void testIsDateInAssessmentYear_AfterEnd() {
        LocalDate date = LocalDate.of(2027, 3, 1);
        assertFalse(TaxYearHelper.isDateInAssessmentYear(date, 2027));
    }

    @Test
    void testDoesProfileOverlapAssessmentYear_FullyInside() {
        LocalDate effectiveFrom = LocalDate.of(2026, 4, 1);
        LocalDate effectiveTo = LocalDate.of(2026, 9, 1);
        assertTrue(TaxYearHelper.doesProfileOverlapAssessmentYear(effectiveFrom, effectiveTo, 2027));
    }

    @Test
    void testDoesProfileOverlapAssessmentYear_StartsBeforeYear() {
        LocalDate effectiveFrom = LocalDate.of(2026, 1, 1);
        LocalDate effectiveTo = LocalDate.of(2026, 9, 1);
        assertTrue(TaxYearHelper.doesProfileOverlapAssessmentYear(effectiveFrom, effectiveTo, 2027));
    }

    @Test
    void testDoesProfileOverlapAssessmentYear_EndsAfterYear() {
        LocalDate effectiveFrom = LocalDate.of(2026, 9, 1);
        LocalDate effectiveTo = LocalDate.of(2027, 6, 1);
        assertTrue(TaxYearHelper.doesProfileOverlapAssessmentYear(effectiveFrom, effectiveTo, 2027));
    }

    @Test
    void testDoesProfileOverlapAssessmentYear_OpenEnded() {
        LocalDate effectiveFrom = LocalDate.of(2026, 9, 1);
        LocalDate effectiveTo = null;
        assertTrue(TaxYearHelper.doesProfileOverlapAssessmentYear(effectiveFrom, effectiveTo, 2027));
    }

    @Test
    void testDoesProfileOverlapAssessmentYear_EndsBeforeYear() {
        LocalDate effectiveFrom = LocalDate.of(2025, 9, 1);
        LocalDate effectiveTo = LocalDate.of(2026, 2, 28);
        assertFalse(TaxYearHelper.doesProfileOverlapAssessmentYear(effectiveFrom, effectiveTo, 2027));
    }

    @Test
    void testDoesProfileOverlapAssessmentYear_StartsAfterYear() {
        LocalDate effectiveFrom = LocalDate.of(2027, 4, 1);
        LocalDate effectiveTo = LocalDate.of(2027, 9, 1);
        assertFalse(TaxYearHelper.doesProfileOverlapAssessmentYear(effectiveFrom, effectiveTo, 2027));
    }

    @Test
    void testDoesProfileOverlapAssessmentYear_StartsAtYearEnd() {
        LocalDate effectiveFrom = LocalDate.of(2027, 3, 1);
        LocalDate effectiveTo = LocalDate.of(2027, 9, 1);
        assertFalse(TaxYearHelper.doesProfileOverlapAssessmentYear(effectiveFrom, effectiveTo, 2027));
    }

    @Test
    void testDoesProfileOverlapAssessmentYear_EndsAtYearStart() {
        LocalDate effectiveFrom = LocalDate.of(2025, 9, 1);
        LocalDate effectiveTo = LocalDate.of(2026, 3, 1);
        assertFalse(TaxYearHelper.doesProfileOverlapAssessmentYear(effectiveFrom, effectiveTo, 2027));
    }

    @Test
    void testIsDateInProfileInterval_Inside() {
        LocalDate date = LocalDate.of(2026, 6, 15);
        LocalDate effectiveFrom = LocalDate.of(2026, 4, 1);
        LocalDate effectiveTo = LocalDate.of(2026, 9, 1);
        assertTrue(TaxYearHelper.isDateInProfileInterval(date, effectiveFrom, effectiveTo));
    }

    @Test
    void testIsDateInProfileInterval_StartBoundary() {
        LocalDate date = LocalDate.of(2026, 4, 1);
        LocalDate effectiveFrom = LocalDate.of(2026, 4, 1);
        LocalDate effectiveTo = LocalDate.of(2026, 9, 1);
        assertTrue(TaxYearHelper.isDateInProfileInterval(date, effectiveFrom, effectiveTo));
    }

    @Test
    void testIsDateInProfileInterval_EndBoundary() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        LocalDate effectiveFrom = LocalDate.of(2026, 4, 1);
        LocalDate effectiveTo = LocalDate.of(2026, 9, 1);
        assertFalse(TaxYearHelper.isDateInProfileInterval(date, effectiveFrom, effectiveTo));
    }

    @Test
    void testIsDateInProfileInterval_BeforeStart() {
        LocalDate date = LocalDate.of(2026, 3, 31);
        LocalDate effectiveFrom = LocalDate.of(2026, 4, 1);
        LocalDate effectiveTo = LocalDate.of(2026, 9, 1);
        assertFalse(TaxYearHelper.isDateInProfileInterval(date, effectiveFrom, effectiveTo));
    }

    @Test
    void testIsDateInProfileInterval_AfterEnd() {
        LocalDate date = LocalDate.of(2026, 9, 2);
        LocalDate effectiveFrom = LocalDate.of(2026, 4, 1);
        LocalDate effectiveTo = LocalDate.of(2026, 9, 1);
        assertFalse(TaxYearHelper.isDateInProfileInterval(date, effectiveFrom, effectiveTo));
    }

    @Test
    void testIsDateInProfileInterval_OpenEnded() {
        LocalDate date = LocalDate.of(2027, 6, 15);
        LocalDate effectiveFrom = LocalDate.of(2026, 4, 1);
        LocalDate effectiveTo = null;
        assertTrue(TaxYearHelper.isDateInProfileInterval(date, effectiveFrom, effectiveTo));
    }

    @Test
    void testProfileBoundaryTransition_NoGap() {
        // Old profile: [2026-03-01, 2026-09-01)
        LocalDate oldFrom = LocalDate.of(2026, 3, 1);
        LocalDate oldTo = LocalDate.of(2026, 9, 1);
        
        // New profile: [2026-09-01, null)
        LocalDate newFrom = LocalDate.of(2026, 9, 1);
        LocalDate newTo = null;
        
        // On 2026-08-31: old matches
        assertTrue(TaxYearHelper.isDateInProfileInterval(LocalDate.of(2026, 8, 31), oldFrom, oldTo));
        assertFalse(TaxYearHelper.isDateInProfileInterval(LocalDate.of(2026, 8, 31), newFrom, newTo));
        
        // On 2026-09-01: old does NOT match, new DOES match
        assertFalse(TaxYearHelper.isDateInProfileInterval(LocalDate.of(2026, 9, 1), oldFrom, oldTo));
        assertTrue(TaxYearHelper.isDateInProfileInterval(LocalDate.of(2026, 9, 1), newFrom, newTo));
    }
}
