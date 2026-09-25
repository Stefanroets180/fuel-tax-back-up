package za.co.fleetexpense.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.co.fleetexpense.dto.TaxCalculationResult;
import za.co.fleetexpense.entity.SarsCostScaleBracket;
import za.co.fleetexpense.entity.SarsPrescribedRate;
import za.co.fleetexpense.entity.VehicleTaxProfile;
import za.co.fleetexpense.entity.enums.CompensationType;
import za.co.fleetexpense.entity.enums.TaxCalculationMethod;
import za.co.fleetexpense.entity.enums.TaxpayerType;
import za.co.fleetexpense.repository.SarsCostScaleBracketRepository;
import za.co.fleetexpense.repository.SarsPrescribedRateRepository;
import za.co.fleetexpense.repository.VehicleTaxProfileRepository;
import za.co.fleetexpense.exception.ValidationException;
import za.co.fleetexpense.util.TaxYearHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxCalculationService {

    private final SarsCostScaleBracketRepository costScaleBracketRepository;
    private final SarsPrescribedRateRepository prescribedRateRepository;
    private final VehicleTaxProfileRepository taxProfileRepository;

    /**
     * Calculate tax deduction using Actual Costs method.
     * Formula: qualifyingCurrentExpenseCents × businessShare
     * Sole proprietors add wear-and-tear: claimYears = clamp(yearsToEnd,0,5) - clamp(yearsToStart,0,5)
     */
    public TaxCalculationResult calculateActualCosts(
            VehicleTaxProfile profile,
            BigDecimal qualifyingCurrentExpenseCents,
            BigDecimal businessKm,
            BigDecimal totalKm,
            LocalDate taxYearStart,
            LocalDate taxYearEnd) {

        // Calculate business share
        BigDecimal businessShare = totalKm.compareTo(BigDecimal.ZERO) > 0 
                ? businessKm.divide(totalKm, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        // Cap business share at 1.0 (100%) to handle data quality issues where businessKm > totalKm
        if (businessShare.compareTo(BigDecimal.ONE) > 0) {
            businessShare = BigDecimal.ONE;
        }

        // Base deduction: qualifying expenses × businessShare
        BigDecimal totalDeduction = qualifyingCurrentExpenseCents.multiply(businessShare)
                .setScale(0, RoundingMode.HALF_UP);

        // Add wear-and-tear for sole proprietors
        BigDecimal wearAndTearCents = BigDecimal.ZERO;
        if (TaxpayerType.SOLE_PROPRIETOR.equals(profile.getTaxpayerType())) {
            // Calculate vehicle age in years at tax year start and end
            LocalDate purchaseDate = profile.getDatePlacedInBusinessUse();
            if (purchaseDate != null) {
                long yearsToStart = ChronoUnit.YEARS.between(purchaseDate, taxYearStart);
                long yearsToEnd = ChronoUnit.YEARS.between(purchaseDate, taxYearEnd);
                
                // Claim years decay: clamp to 0-5 range
                int claimYearsStart = (int) Math.max(0, Math.min(5, yearsToStart));
                int claimYearsEnd = (int) Math.max(0, Math.min(5, yearsToEnd));
                int claimYears = claimYearsEnd - claimYearsStart;
                
                // Wear-and-tear: 20% per year of vehicle cost (simplified)
                if (claimYears > 0) {
                    BigDecimal wearAndTearRate = BigDecimal.valueOf(0.20).multiply(BigDecimal.valueOf(claimYears));
                    wearAndTearCents = BigDecimal.valueOf(profile.getVehicleCostCents()).multiply(wearAndTearRate)
                            .multiply(businessShare)
                            .setScale(0, RoundingMode.HALF_UP);
                }
            }
        }

        totalDeduction = totalDeduction.add(wearAndTearCents);

        Map<String, Object> breakdown = new HashMap<>();
        breakdown.put("qualifyingCurrentExpenseCents", qualifyingCurrentExpenseCents);
        breakdown.put("businessShare", businessShare);
        breakdown.put("wearAndTearCents", wearAndTearCents);
        breakdown.put("taxpayerType", profile.getTaxpayerType());

        return TaxCalculationResult.builder()
                .method("ACTUAL_COSTS")
                .totalDeductionCents(totalDeduction)
                .fixedCostCents(wearAndTearCents)
                .fuelCostCents(BigDecimal.ZERO)
                .maintenanceCostCents(BigDecimal.ZERO)
                .businessShare(businessShare)
                .businessUseDays(null)
                .daysInTaxYear(null)
                .bracketDescription("Actual Costs Method")
                .eligible(true)
                .breakdown(breakdown)
                .build();
    }

    /**
     * Calculate tax deduction using SARS Cost Scale method.
     * Formula: fixedCost × (businessUseDays / daysInTaxYear) × businessShare
     * Fuel/maintenance: businessKm × rate / 10 (direct form)
     * Borne-by rules: EMPLOYEE only (exclude EMPLOYER and SHARED)
     */
    public TaxCalculationResult calculateSarsCostScale(
            VehicleTaxProfile profile,
            LocalDate taxYearStart,
            LocalDate taxYearEnd,
            BigDecimal businessKm,
            BigDecimal totalKm) {

        Integer taxYear = taxYearEnd.getYear();
        
        // Get bracket based on vehicle cost
        Optional<SarsCostScaleBracket> bracketOpt = costScaleBracketRepository
                .findByTaxYearAndVehicleValue(taxYear, profile.getVehicleCostCents());
        
        if (bracketOpt.isEmpty()) {
            return TaxCalculationResult.builder()
                    .method("SARS_COST_SCALE")
                    .eligible(false)
                    .ineligibilityReason("No cost scale bracket found for vehicle value")
                    .build();
        }
        
        SarsCostScaleBracket bracket = bracketOpt.get();
        
        // Normalize to half-open interval: caller passes inclusive end, convert to exclusive
        LocalDate taxYearEndExclusive = taxYearEnd.plusDays(1);
        
        // Calculate business use days within tax year using half-open semantics
        LocalDate effectiveFrom = profile.getEffectiveFrom();
        LocalDate effectiveTo = profile.getEffectiveTo(); // null means open-ended
        LocalDate datePlacedInBusinessUse = profile.getDatePlacedInBusinessUse();
        
        // Determine profile interval within tax year using half-open semantics
        // Profile interval: [effectiveFrom, effectiveTo) where effectiveTo null means open-ended
        // Tax year interval: [taxYearStart, taxYearEndExclusive)
        LocalDate profileStart = effectiveFrom.isBefore(taxYearStart) ? taxYearStart : effectiveFrom;
        if (datePlacedInBusinessUse != null && datePlacedInBusinessUse.isAfter(profileStart)) {
            profileStart = datePlacedInBusinessUse;
        }
        
        // For half-open interval, effectiveTo is exclusive
        // If effectiveTo is null (open-ended), use taxYearEndExclusive as exclusive bound
        // Otherwise, use the earlier of effectiveTo and taxYearEndExclusive
        LocalDate profileEndExclusive;
        if (effectiveTo == null) {
            profileEndExclusive = taxYearEndExclusive;
        } else {
            profileEndExclusive = effectiveTo.isAfter(taxYearEndExclusive) ? taxYearEndExclusive : effectiveTo;
        }
        
        // Business use days using half-open interval: [profileStart, profileEndExclusive)
        long businessUseDays = Math.max(0, ChronoUnit.DAYS.between(profileStart, profileEndExclusive));
        long daysInTaxYear = ChronoUnit.DAYS.between(taxYearStart, taxYearEndExclusive);
        
        // Calculate business share
        BigDecimal businessShare = totalKm.compareTo(BigDecimal.ZERO) > 0 
                ? businessKm.divide(totalKm, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        // Cap business share at 1.0 (100%) to handle data quality issues where businessKm > totalKm
        if (businessShare.compareTo(BigDecimal.ONE) > 0) {
            businessShare = BigDecimal.ONE;
        }
        
        // Apply borne-by rules: EMPLOYEE only
        BigDecimal fixedCost = BigDecimal.valueOf(bracket.getFixedCostCents());
        
        // Direct form: businessKm × fuelTenthsCentsPerKm / 10
        BigDecimal fuelCost = BigDecimal.ZERO;
        if ("EMPLOYEE".equalsIgnoreCase(profile.getFuelBorneBy())) {
            fuelCost = BigDecimal.valueOf(bracket.getFuelCostTenthsCentsPerKm())
                    .multiply(businessKm)
                    .divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP);
        }
        
        // Direct form: businessKm × maintenanceTenthsCentsPerKm / 10
        BigDecimal maintenanceCost = BigDecimal.ZERO;
        if ("EMPLOYEE".equalsIgnoreCase(profile.getMaintenanceBorneBy()) && 
            !profile.getCoveredByMaintenancePlan()) {
            maintenanceCost = BigDecimal.valueOf(bracket.getMaintenanceCostTenthsCentsPerKm())
                    .multiply(businessKm)
                    .divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP);
        }
        
        // Approved formula: fixedCost × (businessUseDays / daysInTaxYear) × businessShare
        BigDecimal dayProRatedFixedCost = fixedCost.multiply(
                BigDecimal.valueOf(businessUseDays).divide(BigDecimal.valueOf(daysInTaxYear), 4, RoundingMode.HALF_UP)
        );
        
        BigDecimal totalDeduction = dayProRatedFixedCost.multiply(businessShare)
                .add(fuelCost)
                .add(maintenanceCost)
                .setScale(0, RoundingMode.HALF_UP);
        
        Map<String, Object> breakdown = new HashMap<>();
        breakdown.put("bracketIndex", bracket.getBracketIndex());
        breakdown.put("minVehicleValue", bracket.getMinVehicleValueCents());
        breakdown.put("maxVehicleValue", bracket.getMaxVehicleValueCents());
        breakdown.put("rawFixedCost", bracket.getFixedCostCents());
        breakdown.put("rawFuelCostPerKm", bracket.getFuelCostTenthsCentsPerKm());
        breakdown.put("rawMaintenanceCostPerKm", bracket.getMaintenanceCostTenthsCentsPerKm());
        breakdown.put("fuelBorneBy", profile.getFuelBorneBy());
        breakdown.put("maintenanceBorneBy", profile.getMaintenanceBorneBy());
        breakdown.put("coveredByMaintenancePlan", profile.getCoveredByMaintenancePlan());
        
        return TaxCalculationResult.builder()
                .method("SARS_COST_SCALE")
                .totalDeductionCents(totalDeduction)
                .fixedCostCents(dayProRatedFixedCost.multiply(businessShare).setScale(0, RoundingMode.HALF_UP))
                .fuelCostCents(fuelCost.setScale(0, RoundingMode.HALF_UP))
                .maintenanceCostCents(maintenanceCost.setScale(0, RoundingMode.HALF_UP))
                .businessShare(businessShare)
                .businessUseDays((int) businessUseDays)
                .daysInTaxYear((int) daysInTaxYear)
                .bracketDescription(bracket.getSourceName())
                .eligible(true)
                .breakdown(breakdown)
                .build();
    }

    /**
     * Calculate tax deduction using Simplified Reimbursive method.
     * Formula: prescribed rate × business km
     */
    public TaxCalculationResult calculateSimplifiedReimbursive(
            VehicleTaxProfile profile,
            LocalDate taxYearStart,
            LocalDate taxYearEnd,
            BigDecimal businessKm) {

        Integer taxYear = taxYearEnd.getYear();
        
        Optional<SarsPrescribedRate> rateOpt = prescribedRateRepository
                .findByTaxYear(taxYear);
        
        if (rateOpt.isEmpty()) {
            return TaxCalculationResult.builder()
                    .method("SIMPLIFIED_REIMBURSIVE")
                    .eligible(false)
                    .ineligibilityReason("No prescribed rate found for tax year")
                    .build();
        }
        
        SarsPrescribedRate rate = rateOpt.get();
        
        // Simplified reimbursive: rate per km × business km
        BigDecimal totalDeduction = BigDecimal.valueOf(rate.getRateCentsPerKm())
                .multiply(businessKm)
                .setScale(0, RoundingMode.HALF_UP);
        
        Map<String, Object> breakdown = new HashMap<>();
        breakdown.put("rateCentsPerKm", rate.getRateCentsPerKm());
        breakdown.put("businessKm", businessKm);
        
        return TaxCalculationResult.builder()
                .method("SIMPLIFIED_REIMBURSIVE")
                .totalDeductionCents(totalDeduction)
                .fixedCostCents(BigDecimal.ZERO)
                .fuelCostCents(BigDecimal.ZERO)
                .maintenanceCostCents(BigDecimal.ZERO)
                .businessShare(BigDecimal.ONE)
                .businessUseDays(null)
                .daysInTaxYear(null)
                .bracketDescription(rate.getSourceName())
                .eligible(true)
                .breakdown(breakdown)
                .build();
    }

    /**
     * Check eligibility based on eligibility matrix.
     * 
     * Broad regime gates evaluated before method-specific logic:
     * 1. Employer-provided vehicle -> deferred fringe-benefit regime
     * 2. COMPANY taxpayer -> deferred corporate regime
     * 3. SOLE_PROPRIETOR -> ACTUAL_COSTS only
     * 4. EMPLOYEE -> evaluate compensation type
     * 
     * ACTUAL_COSTS:      EMPLOYEE+ALLOWANCE ✓ | EMPLOYEE+REIMB ✗ | SOLE_PROP ✓(+W&T) | COMPANY ✗
     * SARS_COST_SCALE:   EMPLOYEE+ALLOWANCE ✓ | EMPLOYEE+REIMB ✗ | SOLE_PROP ✗ | COMPANY ✗
     * SIMPLIFIED_REIMB:  EMPLOYEE+ALLOWANCE ✗ | EMPLOYEE+REIMB ✗ | SOLE_PROP ✗ | COMPANY ✗
     */
    public boolean isEligibleForMethod(
            TaxCalculationMethod method,
            VehicleTaxProfile profile,
            BigDecimal businessKm,
            BigDecimal totalKm) {
        
        TaxpayerType taxpayerType = profile.getTaxpayerType();
        CompensationType compensationType = profile.getCompensationType();
        Boolean isCompanyProvidedVehicle = profile.getIsCompanyProvidedVehicle();
        
        // Regime gate 1: Employer-provided vehicle
        if (Boolean.TRUE.equals(isCompanyProvidedVehicle)) {
            return false; // Deferred fringe-benefit regime
        }
        
        // Regime gate 2: COMPANY taxpayer
        if (TaxpayerType.COMPANY.equals(taxpayerType)) {
            return false; // Deferred corporate regime
        }
        
        // Regime gate 3: SOLE_PROPRIETOR
        if (TaxpayerType.SOLE_PROPRIETOR.equals(taxpayerType)) {
            // Sole proprietor only eligible for ACTUAL_COSTS
            return method == TaxCalculationMethod.ACTUAL_COSTS;
        }
        
        // Regime gate 4: EMPLOYEE
        if (TaxpayerType.EMPLOYEE.equals(taxpayerType)) {
            switch (method) {
                case ACTUAL_COSTS:
                    // Only eligible with fixed travel allowance
                    return CompensationType.TRAVEL_ALLOWANCE.equals(compensationType);
                    
                case SARS_COST_SCALE:
                    // Only eligible with fixed travel allowance
                    return CompensationType.TRAVEL_ALLOWANCE.equals(compensationType);
                    
                case SIMPLIFIED_REIMBURSIVE:
                    // Not eligible - prescribed-rate reimbursement is not a tax-deduction method
                    return false;
                    
                default:
                    return false;
            }
        }
        
        return false;
    }
    
    /**
     * Get ineligibility reason for a method.
     * Provides explicit reasons distinguishing between different regime ineligibilities.
     */
    public String getIneligibilityReason(
            TaxCalculationMethod method,
            VehicleTaxProfile profile) {
        
        TaxpayerType taxpayerType = profile.getTaxpayerType();
        CompensationType compensationType = profile.getCompensationType();
        Boolean isCompanyProvidedVehicle = profile.getIsCompanyProvidedVehicle();
        
        // Regime gate 1: Employer-provided vehicle
        if (Boolean.TRUE.equals(isCompanyProvidedVehicle)) {
            return "Employer-provided vehicle requires the separate fringe-benefit tax regime, which is not yet implemented.";
        }
        
        // Regime gate 2: COMPANY taxpayer
        if (TaxpayerType.COMPANY.equals(taxpayerType)) {
            return "Company vehicle expenditure requires the corporate business-expense tax regime and is not calculated by the individual travel-deduction engine.";
        }
        
        // Regime gate 3: SOLE_PROPRIETOR
        if (TaxpayerType.SOLE_PROPRIETOR.equals(taxpayerType)) {
            switch (method) {
                case ACTUAL_COSTS:
                    return null; // Eligible
                case SARS_COST_SCALE:
                    return "Sole Proprietor must use actual-cost business expenditure path; Section 8 cost scales are not available.";
                case SIMPLIFIED_REIMBURSIVE:
                    return "Sole Proprietor must use actual-cost business expenditure path; prescribed-rate reimbursement is not a tax-deduction method.";
                default:
                    return "Unknown calculation method";
            }
        }
        
        // Regime gate 4: EMPLOYEE
        if (TaxpayerType.EMPLOYEE.equals(taxpayerType)) {
            switch (method) {
                case ACTUAL_COSTS:
                    if (!CompensationType.TRAVEL_ALLOWANCE.equals(compensationType)) {
                        return "Reimbursement tax treatment requires taxable/non-taxable reimbursement facts that are not captured by the current Vehicle Tax Profile.";
                    }
                    return null; // Eligible
                    
                case SARS_COST_SCALE:
                    if (!CompensationType.TRAVEL_ALLOWANCE.equals(compensationType)) {
                        return "Reimbursement tax treatment requires taxable/non-taxable reimbursement facts that are not captured by the current Vehicle Tax Profile.";
                    }
                    return null; // Eligible
                    
                case SIMPLIFIED_REIMBURSIVE:
                    return "Prescribed-rate reimbursement is not a tax-deduction method; it is a PAYE withholding treatment for non-taxable reimbursements.";
                    
                default:
                    return "Unknown calculation method";
            }
        }
        
        return "Unknown taxpayer type";
    }
    
    /**
     * Compute the number of days in a tax year (inclusive).
     * Tax year boundaries are inclusive: March 1 to February 28/29.
     */
    public int computeDaysInTaxYear(LocalDate taxYearStart, LocalDate taxYearEnd) {
        return (int) ChronoUnit.DAYS.between(taxYearStart, taxYearEnd) + 1;
    }
    
    /**
     * Find the active tax profile for a vehicle in a given assessment year.
     * Uses canonical assessment-year convention and half-open interval semantics.
     * 
     * assessmentYear = 2027 means [2026-03-01, 2027-03-01)
     * 
     * Profile interval uses half-open semantics:
     * - effectiveFrom is inclusive
     * - effectiveTo is exclusive (null means open-ended)
     * 
     * Returns null if no profile overlaps the assessment year.
     * Throws ValidationException if multiple profiles overlap (ambiguous history).
     */
    public VehicleTaxProfile findProfileForTaxYear(UUID vehicleId, Integer assessmentYear) {
        LocalDate assessmentStart = TaxYearHelper.getAssessmentYearStart(assessmentYear);
        LocalDate assessmentEndExclusive = TaxYearHelper.getAssessmentYearEndExclusive(assessmentYear);
        
        // Find all profiles for the vehicle
        List<VehicleTaxProfile> profiles = taxProfileRepository.findByVehicleId(vehicleId);
        
        VehicleTaxProfile matchingProfile = null;
        
        for (VehicleTaxProfile profile : profiles) {
            LocalDate effectiveFrom = profile.getEffectiveFrom();
            LocalDate effectiveTo = profile.getEffectiveTo();
            
            // Check if profile overlaps with assessment year using half-open semantics
            boolean overlaps = TaxYearHelper.doesProfileOverlapAssessmentYear(
                    effectiveFrom, effectiveTo, assessmentYear);
            
            if (overlaps) {
                // If we already found a matching profile, this is ambiguous
                if (matchingProfile != null) {
                    throw new ValidationException(
                            String.format("Multiple Vehicle Tax Profiles apply within assessment year %d for vehicle %s. " +
                                    "Mid-year profile changes are not yet supported by tax calculation.",
                                    assessmentYear, vehicleId));
                }
                matchingProfile = profile;
            }
        }
        
        return matchingProfile;
    }

    /**
     * Canonical resolver for tax calculation method.
     * 
     * Resolution hierarchy:
     * 1. Explicit requested method wins
     * 2. Organization default method
     * 3. Fail with explicit error if both are null
     * 
     * VehicleTaxProfile.defaultCalculationMethod is NOT consulted in this phase.
     * 
     * @param requestedMethod The method explicitly requested (may be null)
     * @param organizationDefaultMethod The organization's default method (may be null)
     * @return The resolved calculation method
     * @throws ValidationException if both requested and organization default are null
     */
    public TaxCalculationMethod resolveCalculationMethod(
            TaxCalculationMethod requestedMethod,
            TaxCalculationMethod organizationDefaultMethod) {
        if (requestedMethod != null) {
            return requestedMethod;
        }
        
        if (organizationDefaultMethod != null) {
            return organizationDefaultMethod;
        }
        
        throw new ValidationException("Organization default tax calculation method is not set.");
    }
}
