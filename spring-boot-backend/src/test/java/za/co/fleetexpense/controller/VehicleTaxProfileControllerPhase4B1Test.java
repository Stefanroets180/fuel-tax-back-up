package za.co.fleetexpense.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import za.co.fleetexpense.dto.TaxCalculationResult;
import za.co.fleetexpense.dto.TaxYearSummaryDTO;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.VehicleTaxProfile;
import za.co.fleetexpense.entity.enums.CompensationType;
import za.co.fleetexpense.entity.enums.TaxCalculationMethod;
import za.co.fleetexpense.entity.enums.TaxpayerType;
import za.co.fleetexpense.repository.OrganizationRepository;
import za.co.fleetexpense.repository.UserRepository;
import za.co.fleetexpense.repository.VehicleRepository;
import za.co.fleetexpense.repository.VehicleTaxProfileRepository;
import za.co.fleetexpense.security.UserPrincipal;
import za.co.fleetexpense.service.PermissionService;
import za.co.fleetexpense.service.TaxCalculationService;
import za.co.fleetexpense.service.TaxYearSummaryService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for VehicleTaxProfileController Phase 4B.1 eligibility wiring.
 * No Spring context, no database, no external dependencies.
 * Tests focus on:
 * - Eligibility is checked before calculations
 * - Ineligible methods do not run calculators
 * - Eligible methods do run calculators
 * - Response includes all three method entries
 */
@ExtendWith(MockitoExtension.class)
class VehicleTaxProfileControllerPhase4B1Test {

    @Mock
    private VehicleTaxProfileRepository vehicleTaxProfileRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaxCalculationService taxCalculationService;

    @Mock
    private TaxYearSummaryService taxYearSummaryService;

    @Mock
    private PermissionService permissionService;

    private VehicleTaxProfileController controller;

    private UUID vehicleId;
    private UUID organizationId;
    private UUID userId;
    private UserPrincipal principal;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new VehicleTaxProfileController(
                vehicleTaxProfileRepository,
                vehicleRepository,
                organizationRepository,
                userRepository,
                taxCalculationService,
                taxYearSummaryService,
                permissionService);

        vehicleId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        
        // Create User for UserPrincipal
        Organization organization = Organization.builder()
                .id(organizationId)
                .name("Test Org")
                .mode(za.co.fleetexpense.entity.enums.OrganizationMode.SOLO)
                .build();
        
        user = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("hashed")
                .role(za.co.fleetexpense.entity.enums.UserRole.ADMIN)
                .organization(organization)
                .build();
        
        principal = new UserPrincipal(user);
    }

    @Test
    void testEmployeeTravelAllowance_EligibleMethodsRunCalculators() {
        // Setup
        Vehicle vehicle = createVehicle(organizationId);
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.TRAVEL_ALLOWANCE,
                false);
        TaxYearSummaryDTO taxYearSummary = createTaxYearSummary(1000, 2000, 50000L);

        when(vehicleRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(profile);
        when(taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027)).thenReturn(taxYearSummary);

        // Mock eligibility
        when(taxCalculationService.isEligibleForMethod(
                eq(TaxCalculationMethod.ACTUAL_COSTS),
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class))).thenReturn(true);
        when(taxCalculationService.isEligibleForMethod(
                eq(TaxCalculationMethod.SARS_COST_SCALE),
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class))).thenReturn(true);
        when(taxCalculationService.isEligibleForMethod(
                eq(TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE),
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class))).thenReturn(false);

        // Mock calculations
        TaxCalculationResult actualCostsResult = TaxCalculationResult.builder()
                .method("ACTUAL_COSTS")
                .eligible(true)
                .totalDeductionCents(BigDecimal.valueOf(25000L))
                .build();
        TaxCalculationResult sarsCostScaleResult = TaxCalculationResult.builder()
                .method("SARS_COST_SCALE")
                .eligible(true)
                .totalDeductionCents(BigDecimal.valueOf(30000L))
                .build();

        when(taxCalculationService.calculateActualCosts(
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDate.class),
                any(LocalDate.class))).thenReturn(actualCostsResult);
        when(taxCalculationService.calculateSarsCostScale(
                any(VehicleTaxProfile.class),
                any(LocalDate.class),
                any(LocalDate.class),
                any(BigDecimal.class),
                any(BigDecimal.class))).thenReturn(sarsCostScaleResult);
        when(taxCalculationService.getIneligibilityReason(
                eq(TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE),
                any(VehicleTaxProfile.class)))
                .thenReturn("Prescribed-rate reimbursement is not a tax-deduction method");

        // Execute
        ResponseEntity<Map<String, Object>> response = controller.calculateTaxComparison(
                vehicleId, 2027, principal);

        // Verify
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        
        @SuppressWarnings("unchecked")
        Map<String, TaxCalculationResult> results = (Map<String, TaxCalculationResult>) body.get("results");
        assertNotNull(results);
        assertEquals(3, results.size());
        
        // Verify ACTUAL_COSTS ran calculator
        verify(taxCalculationService, times(1)).calculateActualCosts(
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDate.class),
                any(LocalDate.class));
        
        // Verify SARS_COST_SCALE ran calculator
        verify(taxCalculationService, times(1)).calculateSarsCostScale(
                any(VehicleTaxProfile.class),
                any(LocalDate.class),
                any(LocalDate.class),
                any(BigDecimal.class),
                any(BigDecimal.class));
        
        // Verify SIMPLIFIED_REIMBURSIVE did NOT run calculator
        verify(taxCalculationService, never()).calculateSimplifiedReimbursive(
                any(VehicleTaxProfile.class),
                any(LocalDate.class),
                any(LocalDate.class),
                any(BigDecimal.class));
        
        // Verify response contains all three methods
        assertTrue(results.containsKey("actualCosts"));
        assertTrue(results.containsKey("sarsCostScale"));
        assertTrue(results.containsKey("simplifiedReimbursement"));
        
        // Verify eligible methods have positive deductions
        assertTrue(results.get("actualCosts").getTotalDeductionCents().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(results.get("sarsCostScale").getTotalDeductionCents().compareTo(BigDecimal.ZERO) > 0);
        
        // Verify ineligible method has no positive deduction
        assertFalse(results.get("simplifiedReimbursement").getEligible());
        assertNotNull(results.get("simplifiedReimbursement").getIneligibilityReason());
    }

    @Test
    void testEmployeeReimbursement_AllMethodsIneligible_NoCalculatorsRun() {
        // Setup
        Vehicle vehicle = createVehicle(organizationId);
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.REIMBURSEMENT,
                false);
        TaxYearSummaryDTO taxYearSummary = createTaxYearSummary(1000, 2000, 50000L);

        when(vehicleRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(profile);
        when(taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027)).thenReturn(taxYearSummary);

        // Mock all methods as ineligible
        when(taxCalculationService.isEligibleForMethod(
                any(TaxCalculationMethod.class),
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class))).thenReturn(false);

        when(taxCalculationService.getIneligibilityReason(
                eq(TaxCalculationMethod.ACTUAL_COSTS),
                any(VehicleTaxProfile.class)))
                .thenReturn("Reimbursement tax treatment requires taxable/non-taxable reimbursement facts");
        when(taxCalculationService.getIneligibilityReason(
                eq(TaxCalculationMethod.SARS_COST_SCALE),
                any(VehicleTaxProfile.class)))
                .thenReturn("Reimbursement tax treatment requires taxable/non-taxable reimbursement facts");
        when(taxCalculationService.getIneligibilityReason(
                eq(TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE),
                any(VehicleTaxProfile.class)))
                .thenReturn("Prescribed-rate reimbursement is not a tax-deduction method");

        // Execute
        ResponseEntity<Map<String, Object>> response = controller.calculateTaxComparison(
                vehicleId, 2027, principal);

        // Verify
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        
        @SuppressWarnings("unchecked")
        Map<String, TaxCalculationResult> results = (Map<String, TaxCalculationResult>) body.get("results");
        assertNotNull(results);
        assertEquals(3, results.size());
        
        // Verify NO calculators were called
        verify(taxCalculationService, never()).calculateActualCosts(
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDate.class),
                any(LocalDate.class));
        verify(taxCalculationService, never()).calculateSarsCostScale(
                any(VehicleTaxProfile.class),
                any(LocalDate.class),
                any(LocalDate.class),
                any(BigDecimal.class),
                any(BigDecimal.class));
        verify(taxCalculationService, never()).calculateSimplifiedReimbursive(
                any(VehicleTaxProfile.class),
                any(LocalDate.class),
                any(LocalDate.class),
                any(BigDecimal.class));
        
        // Verify all methods are ineligible with reasons
        assertFalse(results.get("actualCosts").getEligible());
        assertNotNull(results.get("actualCosts").getIneligibilityReason());
        assertFalse(results.get("sarsCostScale").getEligible());
        assertNotNull(results.get("sarsCostScale").getIneligibilityReason());
        assertFalse(results.get("simplifiedReimbursement").getEligible());
        assertNotNull(results.get("simplifiedReimbursement").getIneligibilityReason());
    }

    @Test
    void testSoleProprietor_OnlyActualCostsEligible() {
        // Setup
        Vehicle vehicle = createVehicle(organizationId);
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.SOLE_PROPRIETOR,
                CompensationType.TRAVEL_ALLOWANCE,
                false);
        TaxYearSummaryDTO taxYearSummary = createTaxYearSummary(1000, 2000, 50000L);

        when(vehicleRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(profile);
        when(taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027)).thenReturn(taxYearSummary);

        // Mock eligibility
        when(taxCalculationService.isEligibleForMethod(
                eq(TaxCalculationMethod.ACTUAL_COSTS),
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class))).thenReturn(true);
        when(taxCalculationService.isEligibleForMethod(
                eq(TaxCalculationMethod.SARS_COST_SCALE),
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class))).thenReturn(false);
        when(taxCalculationService.isEligibleForMethod(
                eq(TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE),
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class))).thenReturn(false);

        TaxCalculationResult actualCostsResult = TaxCalculationResult.builder()
                .method("ACTUAL_COSTS")
                .eligible(true)
                .totalDeductionCents(BigDecimal.valueOf(25000L))
                .build();

        when(taxCalculationService.calculateActualCosts(
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDate.class),
                any(LocalDate.class))).thenReturn(actualCostsResult);
        when(taxCalculationService.getIneligibilityReason(
                eq(TaxCalculationMethod.SARS_COST_SCALE),
                any(VehicleTaxProfile.class)))
                .thenReturn("Sole Proprietor must use actual-cost business expenditure path");
        when(taxCalculationService.getIneligibilityReason(
                eq(TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE),
                any(VehicleTaxProfile.class)))
                .thenReturn("Sole Proprietor must use actual-cost business expenditure path");

        // Execute
        ResponseEntity<Map<String, Object>> response = controller.calculateTaxComparison(
                vehicleId, 2027, principal);

        // Verify
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        
        @SuppressWarnings("unchecked")
        Map<String, TaxCalculationResult> results = (Map<String, TaxCalculationResult>) body.get("results");
        assertNotNull(results);
        
        // Verify only ACTUAL_COSTS ran calculator
        verify(taxCalculationService, times(1)).calculateActualCosts(
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDate.class),
                any(LocalDate.class));
        verify(taxCalculationService, never()).calculateSarsCostScale(
                any(VehicleTaxProfile.class),
                any(LocalDate.class),
                any(LocalDate.class),
                any(BigDecimal.class),
                any(BigDecimal.class));
        verify(taxCalculationService, never()).calculateSimplifiedReimbursive(
                any(VehicleTaxProfile.class),
                any(LocalDate.class),
                any(LocalDate.class),
                any(BigDecimal.class));
        
        // Verify ACTUAL_COSTS eligible, others ineligible
        assertTrue(results.get("actualCosts").getEligible());
        assertFalse(results.get("sarsCostScale").getEligible());
        assertFalse(results.get("simplifiedReimbursement").getEligible());
    }

    @Test
    void testCompany_AllMethodsIneligible() {
        // Setup
        Vehicle vehicle = createVehicle(organizationId);
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.COMPANY,
                CompensationType.TRAVEL_ALLOWANCE,
                false);
        TaxYearSummaryDTO taxYearSummary = createTaxYearSummary(1000, 2000, 50000L);

        when(vehicleRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(profile);
        when(taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027)).thenReturn(taxYearSummary);

        // Mock all methods as ineligible
        when(taxCalculationService.isEligibleForMethod(
                any(TaxCalculationMethod.class),
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class))).thenReturn(false);

        when(taxCalculationService.getIneligibilityReason(
                any(TaxCalculationMethod.class),
                any(VehicleTaxProfile.class)))
                .thenReturn("Company vehicle expenditure requires the corporate business-expense tax regime");

        // Execute
        ResponseEntity<Map<String, Object>> response = controller.calculateTaxComparison(
                vehicleId, 2027, principal);

        // Verify
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        
        @SuppressWarnings("unchecked")
        Map<String, TaxCalculationResult> results = (Map<String, TaxCalculationResult>) body.get("results");
        assertNotNull(results);
        
        // Verify NO calculators were called
        verify(taxCalculationService, never()).calculateActualCosts(
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDate.class),
                any(LocalDate.class));
        verify(taxCalculationService, never()).calculateSarsCostScale(
                any(VehicleTaxProfile.class),
                any(LocalDate.class),
                any(LocalDate.class),
                any(BigDecimal.class),
                any(BigDecimal.class));
        verify(taxCalculationService, never()).calculateSimplifiedReimbursive(
                any(VehicleTaxProfile.class),
                any(LocalDate.class),
                any(LocalDate.class),
                any(BigDecimal.class));
        
        // Verify all methods ineligible
        assertFalse(results.get("actualCosts").getEligible());
        assertFalse(results.get("sarsCostScale").getEligible());
        assertFalse(results.get("simplifiedReimbursement").getEligible());
    }

    @Test
    void testCompanyProvidedVehicle_AllMethodsIneligible() {
        // Setup
        Vehicle vehicle = createVehicle(organizationId);
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.TRAVEL_ALLOWANCE,
                true);
        TaxYearSummaryDTO taxYearSummary = createTaxYearSummary(1000, 2000, 50000L);

        when(vehicleRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(profile);
        when(taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027)).thenReturn(taxYearSummary);

        // Mock all methods as ineligible
        when(taxCalculationService.isEligibleForMethod(
                any(TaxCalculationMethod.class),
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class))).thenReturn(false);

        when(taxCalculationService.getIneligibilityReason(
                any(TaxCalculationMethod.class),
                any(VehicleTaxProfile.class)))
                .thenReturn("Employer-provided vehicle requires the separate fringe-benefit tax regime");

        // Execute
        ResponseEntity<Map<String, Object>> response = controller.calculateTaxComparison(
                vehicleId, 2027, principal);

        // Verify
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        
        @SuppressWarnings("unchecked")
        Map<String, TaxCalculationResult> results = (Map<String, TaxCalculationResult>) body.get("results");
        assertNotNull(results);
        
        // Verify NO calculators were called
        verify(taxCalculationService, never()).calculateActualCosts(
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDate.class),
                any(LocalDate.class));
        verify(taxCalculationService, never()).calculateSarsCostScale(
                any(VehicleTaxProfile.class),
                any(LocalDate.class),
                any(LocalDate.class),
                any(BigDecimal.class),
                any(BigDecimal.class));
        verify(taxCalculationService, never()).calculateSimplifiedReimbursive(
                any(VehicleTaxProfile.class),
                any(LocalDate.class),
                any(LocalDate.class),
                any(BigDecimal.class));
        
        // Verify all methods ineligible
        assertFalse(results.get("actualCosts").getEligible());
        assertFalse(results.get("sarsCostScale").getEligible());
        assertFalse(results.get("simplifiedReimbursement").getEligible());
    }

    @Test
    void testEligibleActualCosts_CalculationResultUnchanged() {
        // Setup
        Vehicle vehicle = createVehicle(organizationId);
        VehicleTaxProfile profile = createProfile(
                TaxpayerType.EMPLOYEE,
                CompensationType.TRAVEL_ALLOWANCE,
                false);
        TaxYearSummaryDTO taxYearSummary = createTaxYearSummary(1000, 2000, 50000L);

        when(vehicleRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(profile);
        when(taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027)).thenReturn(taxYearSummary);

        when(taxCalculationService.isEligibleForMethod(
                eq(TaxCalculationMethod.ACTUAL_COSTS),
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class))).thenReturn(true);

        TaxCalculationResult originalResult = TaxCalculationResult.builder()
                .method("ACTUAL_COSTS")
                .eligible(true)
                .totalDeductionCents(BigDecimal.valueOf(25000L))
                .businessShare(BigDecimal.valueOf(0.5))
                .fixedCostCents(BigDecimal.valueOf(5000L))
                .fuelCostCents(BigDecimal.valueOf(10000L))
                .maintenanceCostCents(BigDecimal.valueOf(10000L))
                .build();

        when(taxCalculationService.calculateActualCosts(
                any(VehicleTaxProfile.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDate.class),
                any(LocalDate.class))).thenReturn(originalResult);

        // Execute
        ResponseEntity<Map<String, Object>> response = controller.calculateTaxComparison(
                vehicleId, 2027, principal);

        // Verify
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        
        @SuppressWarnings("unchecked")
        Map<String, TaxCalculationResult> results = (Map<String, TaxCalculationResult>) body.get("results");
        assertNotNull(results);
        
        TaxCalculationResult actualCostsResult = results.get("actualCosts");
        assertEquals("ACTUAL_COSTS", actualCostsResult.getMethod());
        assertTrue(actualCostsResult.getEligible());
        assertEquals(BigDecimal.valueOf(25000L), actualCostsResult.getTotalDeductionCents());
        assertEquals(BigDecimal.valueOf(0.5), actualCostsResult.getBusinessShare());
        assertEquals(BigDecimal.valueOf(5000L), actualCostsResult.getFixedCostCents());
        assertEquals(BigDecimal.valueOf(10000L), actualCostsResult.getFuelCostCents());
        assertEquals(BigDecimal.valueOf(10000L), actualCostsResult.getMaintenanceCostCents());
    }

    private Vehicle createVehicle(UUID organizationId) {
        Organization organization = Organization.builder()
                .id(organizationId)
                .name("Test Org")
                .build();
        
        return Vehicle.builder()
                .id(vehicleId)
                .organization(organization)
                .registrationNumber("ABC123")
                .make("Toyota")
                .model("Corolla")
                .year(2020)
                .fuelType(za.co.fleetexpense.entity.enums.FuelType.PETROL_UNLEADED_95)
                .build();
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

    private TaxYearSummaryDTO createTaxYearSummary(Integer businessKm, Integer totalKm, Long qualifyingExpenseCents) {
        TaxYearSummaryDTO dto = new TaxYearSummaryDTO();
        dto.setBusinessKm(businessKm);
        dto.setTotalKm(totalKm);
        dto.setQualifyingCurrentExpenseCents(qualifyingExpenseCents);
        dto.setDataQualityWarnings(List.of());
        return dto;
    }
}
