package za.co.fleetexpense.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.fleetexpense.entity.VehicleTaxProfile;
import za.co.fleetexpense.entity.enums.CompensationType;
import za.co.fleetexpense.entity.enums.TaxCalculationMethod;
import za.co.fleetexpense.entity.enums.TaxpayerType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for TaxCalculationService Phase 4B eligibility guardrails.
 * No Spring context, no database, no external dependencies.
 * Tests focus on:
 * - EMPLOYEE + TRAVEL_ALLOWANCE eligibility
 * - EMPLOYEE + REIMBURSEMENT ineligibility
 * - SOLE_PROPRIETOR ACTUAL_COSTS only
 * - COMPANY ineligibility
 * - Employer-provided vehicle ineligibility
 * - Explicit ineligibility reasons
 */
@ExtendWith(MockitoExtension.class)
class TaxCalculationServicePhase4BTest {

    @Mock
    private za.co.fleetexpense.repository.SarsCostScaleBracketRepository costScaleBracketRepository;

    @Mock
    private za.co.fleetexpense.repository.SarsPrescribedRateRepository prescribedRateRepository;

    @Mock
    private za.co.fleetexpense.repository.VehicleTaxProfileRepository taxProfileRepository;

    private TaxCalculationService taxCalculationService;

    @BeforeEach
    void setUp() {
        taxCalculationService = new TaxCalculationService(
                costScaleBracketRepository,
                prescribedRateRepository,
                taxProfileRepository);
    }

    @Test
    void testEmployeeTravelAllowance_ActualCosts_Eligible() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.TRAVEL_ALLOWANCE,
                false);

        assertTrue(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.ACTUAL_COSTS,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        assertNull(taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.ACTUAL_COSTS,
                profile));
    }

    @Test
    void testEmployeeTravelAllowance_SarsCostScale_Eligible() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.TRAVEL_ALLOWANCE,
                false);

        assertTrue(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.SARS_COST_SCALE,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        assertNull(taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.SARS_COST_SCALE,
                profile));
    }

    @Test
    void testEmployeeTravelAllowance_SimplifiedReimbursive_Ineligible() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.TRAVEL_ALLOWANCE,
                false);

        assertFalse(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        String reason = taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE,
                profile);
        assertNotNull(reason);
        assertTrue(reason.contains("Prescribed-rate reimbursement is not a tax-deduction method"));
    }

    @Test
    void testEmployeeReimbursement_ActualCosts_Ineligible() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.REIMBURSEMENT,
                false);

        assertFalse(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.ACTUAL_COSTS,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        String reason = taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.ACTUAL_COSTS,
                profile);
        assertNotNull(reason);
        assertTrue(reason.contains("Reimbursement tax treatment requires taxable/non-taxable reimbursement facts"));
    }

    @Test
    void testEmployeeReimbursement_SarsCostScale_Ineligible() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.REIMBURSEMENT,
                false);

        assertFalse(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.SARS_COST_SCALE,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        String reason = taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.SARS_COST_SCALE,
                profile);
        assertNotNull(reason);
        assertTrue(reason.contains("Reimbursement tax treatment requires taxable/non-taxable reimbursement facts"));
    }

    @Test
    void testEmployeeReimbursement_SimplifiedReimbursive_Ineligible() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.REIMBURSEMENT,
                false);

        assertFalse(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        String reason = taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE,
                profile);
        assertNotNull(reason);
        assertTrue(reason.contains("Prescribed-rate reimbursement is not a tax-deduction method"));
    }

    @Test
    void testSoleProprietor_ActualCosts_Eligible() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.SOLE_PROPRIETOR,
                CompensationType.TRAVEL_ALLOWANCE, // CompensationType ignored for Sole Proprietor
                false);

        assertTrue(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.ACTUAL_COSTS,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        assertNull(taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.ACTUAL_COSTS,
                profile));
    }

    @Test
    void testSoleProprietor_SarsCostScale_Ineligible_WithAllowance() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.SOLE_PROPRIETOR,
                CompensationType.TRAVEL_ALLOWANCE,
                false);

        assertFalse(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.SARS_COST_SCALE,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        String reason = taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.SARS_COST_SCALE,
                profile);
        assertNotNull(reason);
        assertTrue(reason.contains("Sole Proprietor must use actual-cost business expenditure path"));
        assertTrue(reason.contains("Section 8 cost scales are not available"));
    }

    @Test
    void testSoleProprietor_SarsCostScale_Ineligible_WithReimbursement() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.SOLE_PROPRIETOR,
                CompensationType.REIMBURSEMENT,
                false);

        assertFalse(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.SARS_COST_SCALE,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        String reason = taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.SARS_COST_SCALE,
                profile);
        assertNotNull(reason);
        assertTrue(reason.contains("Sole Proprietor must use actual-cost business expenditure path"));
    }

    @Test
    void testSoleProprietor_SimplifiedReimbursive_Ineligible() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.SOLE_PROPRIETOR,
                CompensationType.REIMBURSEMENT,
                false);

        assertFalse(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        String reason = taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE,
                profile);
        assertNotNull(reason);
        assertTrue(reason.contains("Sole Proprietor must use actual-cost business expenditure path"));
    }

    @Test
    void testCompany_ActualCosts_Ineligible() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.COMPANY,
                CompensationType.TRAVEL_ALLOWANCE,
                false);

        assertFalse(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.ACTUAL_COSTS,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        String reason = taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.ACTUAL_COSTS,
                profile);
        assertNotNull(reason);
        assertTrue(reason.contains("Company vehicle expenditure requires the corporate business-expense tax regime"));
        assertTrue(reason.contains("not calculated by the individual travel-deduction engine"));
    }

    @Test
    void testCompany_SarsCostScale_Ineligible() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.COMPANY,
                CompensationType.TRAVEL_ALLOWANCE,
                false);

        assertFalse(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.SARS_COST_SCALE,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        String reason = taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.SARS_COST_SCALE,
                profile);
        assertNotNull(reason);
        assertTrue(reason.contains("Company vehicle expenditure requires the corporate business-expense tax regime"));
    }

    @Test
    void testCompany_SimplifiedReimbursive_Ineligible() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.COMPANY,
                CompensationType.REIMBURSEMENT,
                false);

        assertFalse(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        String reason = taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE,
                profile);
        assertNotNull(reason);
        assertTrue(reason.contains("Company vehicle expenditure requires the corporate business-expense tax regime"));
    }

    @Test
    void testEmployeeCompanyProvidedVehicle_ActualCosts_Ineligible() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.TRAVEL_ALLOWANCE,
                true);

        assertFalse(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.ACTUAL_COSTS,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        String reason = taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.ACTUAL_COSTS,
                profile);
        assertNotNull(reason);
        assertTrue(reason.contains("Employer-provided vehicle requires the separate fringe-benefit tax regime"));
        assertTrue(reason.contains("not yet implemented"));
    }

    @Test
    void testEmployeeCompanyProvidedVehicle_SarsCostScale_Ineligible() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.TRAVEL_ALLOWANCE,
                true);

        assertFalse(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.SARS_COST_SCALE,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        String reason = taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.SARS_COST_SCALE,
                profile);
        assertNotNull(reason);
        assertTrue(reason.contains("Employer-provided vehicle requires the separate fringe-benefit tax regime"));
    }

    @Test
    void testEmployeeCompanyProvidedVehicle_SimplifiedReimbursive_Ineligible() {
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.REIMBURSEMENT,
                true);

        assertFalse(taxCalculationService.isEligibleForMethod(
                TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE,
                profile,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2000)));

        String reason = taxCalculationService.getIneligibilityReason(
                TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE,
                profile);
        assertNotNull(reason);
        assertTrue(reason.contains("Employer-provided vehicle requires the separate fringe-benefit tax regime"));
    }

    @Test
    void testEveryIneligibleResultHasNonBlankReason() {
        // Test all ineligible combinations have explicit reasons
        
        // EMPLOYEE + REIMBURSEMENT
        VehicleTaxProfile employeeReimb = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.REIMBURSEMENT,
                false);
        
        assertNotNull(taxCalculationService.getIneligibilityReason(TaxCalculationMethod.ACTUAL_COSTS, employeeReimb));
        assertNotNull(taxCalculationService.getIneligibilityReason(TaxCalculationMethod.SARS_COST_SCALE, employeeReimb));
        assertNotNull(taxCalculationService.getIneligibilityReason(TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE, employeeReimb));
        
        // EMPLOYEE + TRAVEL_ALLOWANCE + SIMPLIFIED_REIMBURSIVE
        VehicleTaxProfile employeeAllowance = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.TRAVEL_ALLOWANCE,
                false);
        
        assertNotNull(taxCalculationService.getIneligibilityReason(TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE, employeeAllowance));
        
        // SOLE_PROPRIETOR + SARS_COST_SCALE
        VehicleTaxProfile soleProp = createProfile(
                TaxpayerType.SOLE_PROPRIETOR,
                CompensationType.TRAVEL_ALLOWANCE,
                false);
        
        assertNotNull(taxCalculationService.getIneligibilityReason(TaxCalculationMethod.SARS_COST_SCALE, soleProp));
        assertNotNull(taxCalculationService.getIneligibilityReason(TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE, soleProp));
        
        // COMPANY
        VehicleTaxProfile company = createProfile(
                TaxpayerType.COMPANY,
                CompensationType.TRAVEL_ALLOWANCE,
                false);
        
        assertNotNull(taxCalculationService.getIneligibilityReason(TaxCalculationMethod.ACTUAL_COSTS, company));
        assertNotNull(taxCalculationService.getIneligibilityReason(TaxCalculationMethod.SARS_COST_SCALE, company));
        assertNotNull(taxCalculationService.getIneligibilityReason(TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE, company));
        
        // EMPLOYEE + company-provided vehicle
        VehicleTaxProfile companyCar = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.TRAVEL_ALLOWANCE,
                true);
        
        assertNotNull(taxCalculationService.getIneligibilityReason(TaxCalculationMethod.ACTUAL_COSTS, companyCar));
        assertNotNull(taxCalculationService.getIneligibilityReason(TaxCalculationMethod.SARS_COST_SCALE, companyCar));
        assertNotNull(taxCalculationService.getIneligibilityReason(TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE, companyCar));
    }

    private VehicleTaxProfile createProfile(
            TaxpayerType taxpayerType,
            CompensationType compensationType,
            Boolean isCompanyProvidedVehicle) {
        return VehicleTaxProfile.builder()
                .id(UUID.randomUUID())
                .vehicleCostCents(50000000L)
                .datePlacedInBusinessUse(LocalDate.of(2025, 1, 1))
                .taxpayerVatRegistered(false)
                .taxpayerType(taxpayerType)
                .compensationType(compensationType)
                .fuelBorneBy("EMPLOYEE")
                .maintenanceBorneBy("EMPLOYEE")
                .coveredByMaintenancePlan(false)
                .effectiveFrom(LocalDate.of(2026, 3, 1))
                .effectiveTo(null)
                .defaultCalculationMethod(TaxCalculationMethod.ACTUAL_COSTS)
                .isCompanyProvidedVehicle(isCompanyProvidedVehicle)
                .build();
    }
}
