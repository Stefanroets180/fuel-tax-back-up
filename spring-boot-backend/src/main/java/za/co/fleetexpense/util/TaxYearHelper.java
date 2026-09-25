package za.co.fleetexpense.util;

import java.time.LocalDate;
import java.time.Month;

/**
 * Canonical helper for SARS assessment-year date calculations.
 * 
 * Assessment Year Convention:
 * assessmentYear = 2027 means:
 * - startInclusive = 2026-03-01
 * - endExclusive = 2027-03-01
 * 
 * This is the half-open interval [startInclusive, endExclusive)
 * representing the SARS tax year from 1 March to the last day of February.
 */
public final class TaxYearHelper {

    private TaxYearHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Get the inclusive start date for a given assessment year.
     * 
     * @param assessmentYear the assessment year (e.g., 2027 for 2026/27 tax year)
     * @return the inclusive start date (e.g., 2026-03-01 for assessmentYear 2027)
     */
    public static LocalDate getAssessmentYearStart(int assessmentYear) {
        return LocalDate.of(assessmentYear - 1, Month.MARCH, 1);
    }

    /**
     * Get the exclusive end date for a given assessment year.
     * 
     * @param assessmentYear the assessment year (e.g., 2027 for 2026/27 tax year)
     * @return the exclusive end date (e.g., 2027-03-01 for assessmentYear 2027)
     */
    public static LocalDate getAssessmentYearEndExclusive(int assessmentYear) {
        return LocalDate.of(assessmentYear, Month.MARCH, 1);
    }

    /**
     * Check if a given date falls within the specified assessment year.
     * Uses half-open interval semantics: [startInclusive, endExclusive)
     * 
     * @param date the date to check
     * @param assessmentYear the assessment year
     * @return true if the date is within the assessment year
     */
    public static boolean isDateInAssessmentYear(LocalDate date, int assessmentYear) {
        LocalDate start = getAssessmentYearStart(assessmentYear);
        LocalDate endExclusive = getAssessmentYearEndExclusive(assessmentYear);
        return !date.isBefore(start) && date.isBefore(endExclusive);
    }

    /**
     * Check if a VehicleTaxProfile's effective interval overlaps with the assessment year.
     * Uses half-open interval semantics:
     - effectiveFrom is inclusive
     * - effectiveTo is exclusive (null means open-ended)
     * 
     * @param effectiveFrom the profile's effective from date (inclusive)
     * @param effectiveTo the profile's effective to date (exclusive, null if open-ended)
     * @param assessmentYear the assessment year
     * @return true if the profile overlaps with the assessment year
     */
    public static boolean doesProfileOverlapAssessmentYear(
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            int assessmentYear) {
        
        LocalDate assessmentStart = getAssessmentYearStart(assessmentYear);
        LocalDate assessmentEndExclusive = getAssessmentYearEndExclusive(assessmentYear);
        
        // Profile starts after assessment year ends: no overlap
        if (effectiveFrom.isAfter(assessmentEndExclusive) || effectiveFrom.isEqual(assessmentEndExclusive)) {
            return false;
        }
        
        // Profile ends before assessment year starts: no overlap
        if (effectiveTo != null && (effectiveTo.isBefore(assessmentStart) || effectiveTo.isEqual(assessmentStart))) {
            return false;
        }
        
        return true;
    }

    /**
     * Check if a specific date falls within a profile's effective interval.
     * Uses half-open interval semantics:
     * - effectiveFrom is inclusive
     * - effectiveTo is exclusive (null means open-ended)
     * 
     * @param date the date to check
     * @param effectiveFrom the profile's effective from date (inclusive)
     * @param effectiveTo the profile's effective to date (exclusive, null if open-ended)
     * @return true if the date is within the profile's effective interval
     */
    public static boolean isDateInProfileInterval(
            LocalDate date,
            LocalDate effectiveFrom,
            LocalDate effectiveTo) {
        
        // Date is before profile starts: not in interval
        if (date.isBefore(effectiveFrom)) {
            return false;
        }
        
        // Profile has an end date and date is on or after it: not in interval
        if (effectiveTo != null && !date.isBefore(effectiveTo)) {
            return false;
        }
        
        return true;
    }
}
