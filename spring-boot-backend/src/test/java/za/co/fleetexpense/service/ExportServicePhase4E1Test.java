package za.co.fleetexpense.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import za.co.fleetexpense.dto.ExportRequest;
import za.co.fleetexpense.dto.TaxYearSummaryDTO;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.VehicleTaxProfile;
import za.co.fleetexpense.entity.enums.CompensationType;
import za.co.fleetexpense.entity.enums.ExportFormat;
import za.co.fleetexpense.entity.enums.TaxCalculationMethod;
import za.co.fleetexpense.entity.enums.TaxpayerType;
import za.co.fleetexpense.exception.ResourceNotFoundException;
import za.co.fleetexpense.exception.ValidationException;
import za.co.fleetexpense.repository.VehicleTaxProfileRepository;
import za.co.fleetexpense.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class ExportServicePhase4E1Test {

    @Mock
    private TaxYearSummaryService taxYearSummaryService;

    @Mock
    private TaxCalculationService taxCalculationService;

    @Mock
    private VehicleTaxProfileRepository vehicleTaxProfileRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private ExportService exportService;

    private UUID vehicleId;
    private UUID organizationId;
    private ExportRequest exportRequest;
    private TaxYearSummaryDTO summaryDTO;
    private VehicleTaxProfile oldProfile;
    private VehicleTaxProfile currentProfile;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        vehicle = new Vehicle();
        vehicle.setId(vehicleId);

        exportRequest = new ExportRequest();
        exportRequest.setVehicleId(vehicleId);
        exportRequest.setTaxYear(2027);
        exportRequest.setFormat(ExportFormat.EXCEL);

        summaryDTO = new TaxYearSummaryDTO();
        summaryDTO.setTaxYear(2027);
        summaryDTO.setBusinessKm(8000);
        summaryDTO.setTotalKm(10000);
        summaryDTO.setQualifyingCurrentExpenseCents(5000000L);

        oldProfile = new VehicleTaxProfile();
        oldProfile.setId(UUID.randomUUID());
        oldProfile.setVehicle(vehicle);
        oldProfile.setEffectiveFrom(LocalDate.of(2025, 1, 1));
        oldProfile.setEffectiveTo(LocalDate.of(2027, 6, 30));
        oldProfile.setVehicleCostCents(300000L);
        oldProfile.setTaxpayerType(TaxpayerType.EMPLOYEE);
        oldProfile.setCompensationType(CompensationType.TRAVEL_ALLOWANCE);
        oldProfile.setIsCompanyProvidedVehicle(false);

        currentProfile = new VehicleTaxProfile();
        currentProfile.setId(UUID.randomUUID());
        currentProfile.setVehicle(vehicle);
        currentProfile.setEffectiveFrom(LocalDate.of(2027, 7, 1));
        currentProfile.setEffectiveTo(null);
        currentProfile.setVehicleCostCents(350000L);
        currentProfile.setTaxpayerType(TaxpayerType.EMPLOYEE);
        currentProfile.setCompensationType(CompensationType.TRAVEL_ALLOWANCE);
        currentProfile.setIsCompanyProvidedVehicle(false);
    }

    private Object invokeCalculateSelectedDeduction(UUID vehicleId, ExportRequest request, TaxYearSummaryDTO summary, TaxCalculationMethod resolvedMethod) {
        try {
            Method method = ExportService.class.getDeclaredMethod(
                "calculateSelectedDeduction",
                UUID.class,
                ExportRequest.class,
                TaxYearSummaryDTO.class,
                TaxCalculationMethod.class
            );
            method.setAccessible(true);
            return method.invoke(exportService, vehicleId, request, summary, resolvedMethod);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Unwrap the actual exception thrown by the method
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testExportHistoricalProfile_AssessmentYear2027_SelectsOldProfile() {
        // Given: assessment year 2027, old profile applies
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxCalculationService.isEligibleForMethod(eq(TaxCalculationMethod.ACTUAL_COSTS), eq(oldProfile), any(), any()))
                .thenReturn(true);
        when(taxCalculationService.calculateActualCosts(any(), any(), any(), any(), any(), any()))
                .thenReturn(createEligibleResult());

        // When
        invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.ACTUAL_COSTS);

        // Then: old profile was selected for assessment year 2027
        verify(taxCalculationService).findProfileForTaxYear(vehicleId, 2027);
        verify(taxCalculationService, never()).findProfileForTaxYear(vehicleId, 2028);
    }

    @Test
    void testExportNoProfile_ThrowsResourceNotFoundException() {
        // Given: no profile applies to assessment year
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(null);

        // When/Then: ResourceNotFoundException with assessment year message
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
            invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.ACTUAL_COSTS)
        );
        assertTrue(ex.getMessage().contains("assessment year 2027"));
    }

    @Test
    void testExportBoundaries_AssessmentYear2027_UsesCanonicalHelper() {
        // Given: assessment year 2027
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxCalculationService.isEligibleForMethod(eq(TaxCalculationMethod.ACTUAL_COSTS), eq(oldProfile), any(), any()))
                .thenReturn(true);
        when(taxCalculationService.calculateActualCosts(any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    LocalDate start = inv.getArgument(4);
                    LocalDate end = inv.getArgument(5);
                    
                    // Verify inclusive boundaries: 2026-03-01 to 2027-02-28
                    assertEquals(LocalDate.of(2026, Month.MARCH, 1), start);
                    assertEquals(LocalDate.of(2027, Month.FEBRUARY, 28), end);
                    
                    return createEligibleResult();
                });

        // When
        invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.ACTUAL_COSTS);

        // Then: inclusive boundaries used for calculateActualCosts
        verify(taxCalculationService).calculateActualCosts(
                eq(oldProfile), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class),
                eq(LocalDate.of(2026, Month.MARCH, 1)),
                eq(LocalDate.of(2027, Month.FEBRUARY, 28))
        );
    }

    @Test
    void testExportBoundaries_AssessmentYear2028_LeapYear() {
        // Given: assessment year 2028 (leap year)
        exportRequest.setTaxYear(2028);
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2028)).thenReturn(currentProfile);
        when(taxCalculationService.isEligibleForMethod(eq(TaxCalculationMethod.ACTUAL_COSTS), eq(currentProfile), any(), any()))
                .thenReturn(true);
        when(taxCalculationService.calculateActualCosts(any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    LocalDate start = inv.getArgument(4);
                    LocalDate end = inv.getArgument(5);
                    
                    // Verify inclusive boundaries: 2027-03-01 to 2028-02-29
                    assertEquals(LocalDate.of(2027, Month.MARCH, 1), start);
                    assertEquals(LocalDate.of(2028, Month.FEBRUARY, 29), end);
                    
                    return createEligibleResult();
                });

        // When
        invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.ACTUAL_COSTS);

        // Then: leap year inclusive boundaries used
        verify(taxCalculationService).calculateActualCosts(
                eq(currentProfile), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class),
                eq(LocalDate.of(2027, Month.MARCH, 1)),
                eq(LocalDate.of(2028, Month.FEBRUARY, 29))
        );
    }

    @Test
    void testExportHistoricalProfile_DoesNotUseFindByVehicleIdAndEffectiveToIsNull() {
        // Given: historical profile selection
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxCalculationService.isEligibleForMethod(eq(TaxCalculationMethod.ACTUAL_COSTS), eq(oldProfile), any(), any()))
                .thenReturn(true);
        when(taxCalculationService.calculateActualCosts(any(), any(), any(), any(), any(), any()))
                .thenReturn(createEligibleResult());

        // When
        invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.ACTUAL_COSTS);

        // Then: findByVehicleIdAndEffectiveToIsNull should NOT be called
        verify(vehicleTaxProfileRepository, never()).findByVehicleIdAndEffectiveToIsNull(any());
    }

    // ==================== PHASE 4F.1 ELIGIBILITY TESTS ====================

    @Test
    void testExportEligibility_EmployeeTravelAllowance_ActualCosts_Passes() {
        // Given: EMPLOYEE + TRAVEL_ALLOWANCE + ACTUAL_COSTS
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxCalculationService.isEligibleForMethod(eq(TaxCalculationMethod.ACTUAL_COSTS), eq(oldProfile), any(), any()))
                .thenReturn(true);
        when(taxCalculationService.calculateActualCosts(any(), any(), any(), any(), any(), any()))
                .thenReturn(createEligibleResult());

        // When
        invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.ACTUAL_COSTS);

        // Then: eligibility passes, calculator invoked exactly once
        verify(taxCalculationService).isEligibleForMethod(eq(TaxCalculationMethod.ACTUAL_COSTS), eq(oldProfile), any(), any());
        verify(taxCalculationService).calculateActualCosts(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testExportEligibility_EmployeeTravelAllowance_SarsCostScale_Passes() {
        // Given: EMPLOYEE + TRAVEL_ALLOWANCE + SARS_COST_SCALE
        exportRequest.setCalculationMethod("SARS_COST_SCALE");
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxCalculationService.isEligibleForMethod(eq(TaxCalculationMethod.SARS_COST_SCALE), eq(oldProfile), any(), any()))
                .thenReturn(true);
        when(taxCalculationService.calculateSarsCostScale(any(), any(), any(), any(), any()))
                .thenReturn(createEligibleResult());

        // When
        invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.SARS_COST_SCALE);

        // Then: eligibility passes, calculator invoked exactly once
        verify(taxCalculationService).isEligibleForMethod(eq(TaxCalculationMethod.SARS_COST_SCALE), eq(oldProfile), any(), any());
        verify(taxCalculationService).calculateSarsCostScale(any(), any(), any(), any(), any());
    }

    @Test
    void testExportEligibility_EmployeeReimbursement_ActualCosts_Fails() {
        // Given: EMPLOYEE + REIMBURSEMENT + ACTUAL_COSTS
        oldProfile.setCompensationType(CompensationType.REIMBURSEMENT);
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxCalculationService.isEligibleForMethod(eq(TaxCalculationMethod.ACTUAL_COSTS), eq(oldProfile), any(), any()))
                .thenReturn(false);
        when(taxCalculationService.getIneligibilityReason(eq(TaxCalculationMethod.ACTUAL_COSTS), eq(oldProfile)))
                .thenReturn("Reimbursement tax treatment requires taxable/non-taxable reimbursement facts");

        // When/Then: eligibility fails, ValidationException thrown
        ValidationException ex = assertThrows(ValidationException.class, () ->
            invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.ACTUAL_COSTS)
        );
        assertTrue(ex.getMessage().contains("Reimbursement tax treatment"));

        // Then: calculator NEVER invoked
        verify(taxCalculationService, never()).calculateActualCosts(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testExportEligibility_Company_ActualCosts_Fails() {
        // Given: COMPANY + ACTUAL_COSTS
        oldProfile.setTaxpayerType(TaxpayerType.COMPANY);
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxCalculationService.isEligibleForMethod(eq(TaxCalculationMethod.ACTUAL_COSTS), eq(oldProfile), any(), any()))
                .thenReturn(false);
        when(taxCalculationService.getIneligibilityReason(eq(TaxCalculationMethod.ACTUAL_COSTS), eq(oldProfile)))
                .thenReturn("Company vehicle expenditure requires the corporate business-expense tax regime");

        // When/Then: eligibility fails
        ValidationException ex = assertThrows(ValidationException.class, () ->
            invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.ACTUAL_COSTS)
        );
        assertTrue(ex.getMessage().contains("corporate"));

        // Then: calculator NEVER invoked
        verify(taxCalculationService, never()).calculateActualCosts(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testExportEligibility_CompanyProvidedVehicle_SarsCostScale_Fails() {
        // Given: company-provided vehicle + SARS_COST_SCALE
        exportRequest.setCalculationMethod("SARS_COST_SCALE");
        oldProfile.setIsCompanyProvidedVehicle(true);
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxCalculationService.isEligibleForMethod(eq(TaxCalculationMethod.SARS_COST_SCALE), eq(oldProfile), any(), any()))
                .thenReturn(false);
        when(taxCalculationService.getIneligibilityReason(eq(TaxCalculationMethod.SARS_COST_SCALE), eq(oldProfile)))
                .thenReturn("Employer-provided vehicle requires the separate fringe-benefit tax regime");

        // When/Then: eligibility fails
        ValidationException ex = assertThrows(ValidationException.class, () ->
            invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.SARS_COST_SCALE)
        );
        assertTrue(ex.getMessage().contains("fringe-benefit"));

        // Then: calculator NEVER invoked
        verify(taxCalculationService, never()).calculateSarsCostScale(any(), any(), any(), any(), any());
    }

    @Test
    void testExportEligibility_SoleProprietor_ActualCosts_Passes() {
        // Given: SOLE_PROPRIETOR + ACTUAL_COSTS
        oldProfile.setTaxpayerType(TaxpayerType.SOLE_PROPRIETOR);
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxCalculationService.isEligibleForMethod(eq(TaxCalculationMethod.ACTUAL_COSTS), eq(oldProfile), any(), any()))
                .thenReturn(true);
        when(taxCalculationService.calculateActualCosts(any(), any(), any(), any(), any(), any()))
                .thenReturn(createEligibleResult());

        // When
        invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.ACTUAL_COSTS);

        // Then: eligibility passes, calculator invoked
        verify(taxCalculationService).isEligibleForMethod(eq(TaxCalculationMethod.ACTUAL_COSTS), eq(oldProfile), any(), any());
        verify(taxCalculationService).calculateActualCosts(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testExportEligibility_SoleProprietor_SarsCostScale_Fails() {
        // Given: SOLE_PROPRIETOR + SARS_COST_SCALE
        exportRequest.setCalculationMethod("SARS_COST_SCALE");
        oldProfile.setTaxpayerType(TaxpayerType.SOLE_PROPRIETOR);
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxCalculationService.isEligibleForMethod(eq(TaxCalculationMethod.SARS_COST_SCALE), eq(oldProfile), any(), any()))
                .thenReturn(false);
        when(taxCalculationService.getIneligibilityReason(eq(TaxCalculationMethod.SARS_COST_SCALE), eq(oldProfile)))
                .thenReturn("Sole Proprietor must use actual-cost business expenditure path");

        // When/Then: eligibility fails
        ValidationException ex = assertThrows(ValidationException.class, () ->
            invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.SARS_COST_SCALE)
        );
        assertTrue(ex.getMessage().contains("actual-cost"));

        // Then: calculator NEVER invoked
        verify(taxCalculationService, never()).calculateSarsCostScale(any(), any(), any(), any(), any());
    }

    // ==================== PHASE 4F.2.1 RESOLUTION TESTS ====================

    @Test
    void testExportResolution_ExplicitRequestWins() {
        // Given: explicit request SARS_COST_SCALE, org default ACTUAL_COSTS
        // Resolution happens in gatherExportData before calculateSelectedDeduction
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxCalculationService.isEligibleForMethod(eq(TaxCalculationMethod.SARS_COST_SCALE), eq(oldProfile), any(), any()))
                .thenReturn(true);
        when(taxCalculationService.calculateSarsCostScale(any(), any(), any(), any(), any()))
                .thenReturn(createEligibleResult());

        // When: resolved method SARS_COST_SCALE is passed
        invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.SARS_COST_SCALE);

        // Then: SARS_COST_SCALE calculator invoked (explicit request wins)
        verify(taxCalculationService).calculateSarsCostScale(any(), any(), any(), any(), any());
        verify(taxCalculationService, never()).calculateActualCosts(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testExportResolution_NullRequestUsesOrgDefault() {
        // Given: null request, org default SARS_COST_SCALE
        // Resolution happens in gatherExportData before calculateSelectedDeduction
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxCalculationService.isEligibleForMethod(eq(TaxCalculationMethod.SARS_COST_SCALE), eq(oldProfile), any(), any()))
                .thenReturn(true);
        when(taxCalculationService.calculateSarsCostScale(any(), any(), any(), any(), any()))
                .thenReturn(createEligibleResult());

        // When: resolved method SARS_COST_SCALE is passed
        invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.SARS_COST_SCALE);

        // Then: org default used, SARS_COST_SCALE calculator invoked
        verify(taxCalculationService).calculateSarsCostScale(any(), any(), any(), any(), any());
    }

    @Test
    void testExportResolution_NullRequestNullOrgDefault_ThrowsError() {
        // Given: both null
        when(taxCalculationService.resolveCalculationMethod(isNull(), isNull()))
                .thenThrow(new ValidationException("Organization default tax calculation method is not set."));

        // When/Then: explicit validation error
        ValidationException ex = assertThrows(ValidationException.class, () ->
            taxCalculationService.resolveCalculationMethod(null, null)
        );
        assertEquals("Organization default tax calculation method is not set.", ex.getMessage());
    }

    @Test
    void testExportResolution_ProfileDefaultDoesNotOverrideOrgDefault() {
        // Given: org default ACTUAL_COSTS, profile default SARS_COST_SCALE (ignored)
        oldProfile.setDefaultCalculationMethod(TaxCalculationMethod.SARS_COST_SCALE);
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxCalculationService.isEligibleForMethod(eq(TaxCalculationMethod.ACTUAL_COSTS), eq(oldProfile), any(), any()))
                .thenReturn(true);
        when(taxCalculationService.calculateActualCosts(any(), any(), any(), any(), any(), any()))
                .thenReturn(createEligibleResult());

        // When: resolved method ACTUAL_COSTS is passed
        invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.ACTUAL_COSTS);

        // Then: org default wins, ACTUAL_COSTS calculator invoked (profile default ignored)
        verify(taxCalculationService).calculateActualCosts(any(), any(), any(), any(), any(), any());
        verify(taxCalculationService, never()).calculateSarsCostScale(any(), any(), any(), any(), any());
    }

    @Test
    void testExportResolution_EligibilityAfterResolution() {
        // Given: org default SARS_COST_SCALE, but profile ineligible for SARS_COST_SCALE
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxCalculationService.isEligibleForMethod(eq(TaxCalculationMethod.SARS_COST_SCALE), eq(oldProfile), any(), any()))
                .thenReturn(false);
        when(taxCalculationService.getIneligibilityReason(eq(TaxCalculationMethod.SARS_COST_SCALE), eq(oldProfile)))
                .thenReturn("Profile ineligible for SARS_COST_SCALE");

        // When/Then: eligibility rejects resolved method, calculator never invoked
        ValidationException ex = assertThrows(ValidationException.class, () ->
            invokeCalculateSelectedDeduction(vehicleId, exportRequest, summaryDTO, TaxCalculationMethod.SARS_COST_SCALE)
        );
        assertTrue(ex.getMessage().contains("ineligible"));
        verify(taxCalculationService, never()).calculateSarsCostScale(any(), any(), any(), any(), any());
    }

    private za.co.fleetexpense.dto.TaxCalculationResult createEligibleResult() {
        return za.co.fleetexpense.dto.TaxCalculationResult.builder()
                .method("ACTUAL_COSTS")
                .eligible(true)
                .totalDeductionCents(BigDecimal.valueOf(4000000L))
                .build();
    }
}
