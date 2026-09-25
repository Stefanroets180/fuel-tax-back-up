package za.co.fleetexpense.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.fleetexpense.entity.SarsCostScaleBracket;
import za.co.fleetexpense.entity.SarsPrescribedRate;
import za.co.fleetexpense.entity.VehicleTaxProfile;
import za.co.fleetexpense.entity.enums.CompensationType;
import za.co.fleetexpense.entity.enums.TaxCalculationMethod;
import za.co.fleetexpense.entity.enums.TaxpayerType;
import za.co.fleetexpense.exception.ValidationException;
import za.co.fleetexpense.repository.SarsCostScaleBracketRepository;
import za.co.fleetexpense.repository.SarsPrescribedRateRepository;
import za.co.fleetexpense.repository.VehicleTaxProfileRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for TaxCalculationService Phase 4A fixes.
 * No Spring context, no database, no external dependencies.
 * Tests focus on:
 * - Assessment-year convention consistency
 * - Half-open profile date semantics
 * - Deterministic profile selection
 */
@ExtendWith(MockitoExtension.class)
class TaxCalculationServicePhase4ATest {

    @Mock
    private SarsCostScaleBracketRepository costScaleBracketRepository;

    @Mock
    private SarsPrescribedRateRepository prescribedRateRepository;

    @Mock
    private VehicleTaxProfileRepository taxProfileRepository;

    private TaxCalculationService taxCalculationService;

    @BeforeEach
    void setUp() {
        taxCalculationService = new TaxCalculationService(
                costScaleBracketRepository,
                prescribedRateRepository,
                taxProfileRepository);
    }

    @Test
    void testFindProfileForTaxYear_UsesCanonicalAssessmentYearConvention() {
        // This test verifies that findProfileForTaxYear uses the same convention
        // as TaxYearSummaryService: assessmentYear 2027 means [2026-03-01, 2027-03-01)
        
        UUID vehicleId = UUID.randomUUID();
        VehicleTaxProfile profile = createProfile(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 9, 1));
        
        List<VehicleTaxProfile> profiles = List.of(profile);
        when(taxProfileRepository.findByVehicleId(vehicleId)).thenReturn(profiles);
        
        // Find profile for assessment year 2027
        VehicleTaxProfile result = taxCalculationService.findProfileForTaxYear(vehicleId, 2027);
        
        assertNotNull(result);
        assertEquals(profile.getId(), result.getId());
    }

    @Test
    void testFindProfileForTaxYear_ProfileEndingOnBoundaryDoesNotMatchBoundary() {
        // Profile ending on 2026-09-01 should NOT match 2026-09-01 (exclusive boundary)
        
        UUID vehicleId = UUID.randomUUID();
        VehicleTaxProfile profile = createProfile(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 9, 1));
        
        List<VehicleTaxProfile> profiles = List.of(profile);
        when(taxProfileRepository.findByVehicleId(vehicleId)).thenReturn(profiles);
        
        VehicleTaxProfile result = taxCalculationService.findProfileForTaxYear(vehicleId, 2027);
        
        // Profile should match assessment year 2027 (overlaps [2026-03-01, 2027-03-01))
        assertNotNull(result);
        
        // But profile should NOT match date 2026-09-01 itself
        // This is verified by the half-open semantics in TaxYearHelper
        assertFalse(za.co.fleetexpense.util.TaxYearHelper.isDateInProfileInterval(
                LocalDate.of(2026, 9, 1),
                profile.getEffectiveFrom(),
                profile.getEffectiveTo()));
    }

    @Test
    void testFindProfileForTaxYear_ProfileStartingOnBoundaryMatchesBoundary() {
        // Profile starting on 2026-09-01 SHOULD match 2026-09-01 (inclusive boundary)
        
        UUID vehicleId = UUID.randomUUID();
        VehicleTaxProfile profile = createProfile(
                LocalDate.of(2026, 9, 1),
                null);
        
        List<VehicleTaxProfile> profiles = List.of(profile);
        when(taxProfileRepository.findByVehicleId(vehicleId)).thenReturn(profiles);
        
        VehicleTaxProfile result = taxCalculationService.findProfileForTaxYear(vehicleId, 2027);
        
        assertNotNull(result);
        
        // Profile should match date 2026-09-01
        assertTrue(za.co.fleetexpense.util.TaxYearHelper.isDateInProfileInterval(
                LocalDate.of(2026, 9, 1),
                profile.getEffectiveFrom(),
                profile.getEffectiveTo()));
    }

    @Test
    void testFindProfileForTaxYear_OpenEndedProfileMatchesFutureDates() {
        // Open-ended profile (effectiveTo = null) should match future dates
        
        UUID vehicleId = UUID.randomUUID();
        VehicleTaxProfile profile = createProfile(
                LocalDate.of(2026, 9, 1),
                null);
        
        List<VehicleTaxProfile> profiles = List.of(profile);
        when(taxProfileRepository.findByVehicleId(vehicleId)).thenReturn(profiles);
        
        VehicleTaxProfile result = taxCalculationService.findProfileForTaxYear(vehicleId, 2027);
        
        assertNotNull(result);
        
        // Profile should match a future date
        assertTrue(za.co.fleetexpense.util.TaxYearHelper.isDateInProfileInterval(
                LocalDate.of(2027, 6, 15),
                profile.getEffectiveFrom(),
                profile.getEffectiveTo()));
    }

    @Test
    void testFindProfileForTaxYear_DeterministicSelection_SingleProfile() {
        // When only one profile overlaps, it should be selected deterministically
        
        UUID vehicleId = UUID.randomUUID();
        VehicleTaxProfile profile = createProfile(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 9, 1));
        
        List<VehicleTaxProfile> profiles = List.of(profile);
        when(taxProfileRepository.findByVehicleId(vehicleId)).thenReturn(profiles);
        
        VehicleTaxProfile result = taxCalculationService.findProfileForTaxYear(vehicleId, 2027);
        
        assertNotNull(result);
        assertEquals(profile.getId(), result.getId());
    }

    @Test
    void testFindProfileForTaxYear_DeterministicSelection_NoProfile() {
        // When no profile overlaps, return null
        
        UUID vehicleId = UUID.randomUUID();
        VehicleTaxProfile profile = createProfile(
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 9, 1));
        
        List<VehicleTaxProfile> profiles = List.of(profile);
        when(taxProfileRepository.findByVehicleId(vehicleId)).thenReturn(profiles);
        
        VehicleTaxProfile result = taxCalculationService.findProfileForTaxYear(vehicleId, 2027);
        
        assertNull(result);
    }

    @Test
    void testFindProfileForTaxYear_MultipleProfilesOverlapping_ThrowsValidationException() {
        // When multiple profiles overlap the assessment year, throw ValidationException
        // This prevents arbitrary first-list selection and provides explicit failure
        
        UUID vehicleId = UUID.randomUUID();
        VehicleTaxProfile profile1 = createProfile(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 9, 1));
        VehicleTaxProfile profile2 = createProfile(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 1));
        
        List<VehicleTaxProfile> profiles = List.of(profile1, profile2);
        when(taxProfileRepository.findByVehicleId(vehicleId)).thenReturn(profiles);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            taxCalculationService.findProfileForTaxYear(vehicleId, 2027);
        });
        
        assertTrue(exception.getMessage().contains("Multiple Vehicle Tax Profiles apply within assessment year 2027"));
        assertTrue(exception.getMessage().contains("Mid-year profile changes are not yet supported"));
    }

    @Test
    void testFindProfileForTaxYear_ProfileChangesInsideAssessmentYear_ThrowsValidationException() {
        // When profile changes inside assessment year, multiple profiles will overlap
        // Should throw ValidationException with explicit message
        
        UUID vehicleId = UUID.randomUUID();
        VehicleTaxProfile oldProfile = createProfile(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 9, 1));
        VehicleTaxProfile newProfile = createProfile(
                LocalDate.of(2026, 9, 1),
                null);
        
        List<VehicleTaxProfile> profiles = List.of(oldProfile, newProfile);
        when(taxProfileRepository.findByVehicleId(vehicleId)).thenReturn(profiles);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            taxCalculationService.findProfileForTaxYear(vehicleId, 2027);
        });
        
        assertTrue(exception.getMessage().contains("Multiple Vehicle Tax Profiles apply within assessment year 2027"));
        assertTrue(exception.getMessage().contains("Mid-year profile changes are not yet supported"));
    }

    @Test
    void testFindProfileForTaxYear_NonOverlappingProfiles_ReturnsCorrectOne() {
        // When profiles don't overlap, the correct one should be selected
        
        UUID vehicleId = UUID.randomUUID();
        VehicleTaxProfile oldProfile = createProfile(
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2026, 2, 28));
        VehicleTaxProfile newProfile = createProfile(
                LocalDate.of(2026, 9, 1),
                null);
        
        List<VehicleTaxProfile> profiles = List.of(oldProfile, newProfile);
        when(taxProfileRepository.findByVehicleId(vehicleId)).thenReturn(profiles);
        
        VehicleTaxProfile result = taxCalculationService.findProfileForTaxYear(vehicleId, 2027);
        
        // Only newProfile overlaps assessment year 2027
        assertNotNull(result);
        assertEquals(newProfile.getId(), result.getId());
    }

    @Test
    void testCalculateSarsCostScale_UsesHalfOpenSemantics() {
        // Verify that calculateSarsCostScale uses half-open interval for businessUseDays
        
        UUID vehicleId = UUID.randomUUID();
        VehicleTaxProfile profile = createProfile(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 9, 1));
        
        SarsCostScaleBracket bracket = new SarsCostScaleBracket();
        bracket.setFixedCostCents(100000L);
        bracket.setFuelCostTenthsCentsPerKm(1329L);
        bracket.setMaintenanceCostTenthsCentsPerKm(491L);
        
        when(costScaleBracketRepository.findByTaxYearAndVehicleValue(anyInt(), any()))
                .thenReturn(Optional.of(bracket));
        
        LocalDate taxYearStart = LocalDate.of(2026, 3, 1);
        LocalDate taxYearEnd = LocalDate.of(2027, 2, 28);
        
        var result = taxCalculationService.calculateSarsCostScale(
                profile, taxYearStart, taxYearEnd,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(2000));
        
        assertNotNull(result);
        // Business use days should be calculated using half-open interval
        // Profile interval: [2026-04-01, 2026-09-01)
        // Days = 153 (April 1 to August 31 inclusive)
        assertEquals(153, result.getBusinessUseDays());
    }

    @Test
    void testCalculateSarsCostScale_OpenEndedProfile() {
        // Verify that open-ended profile (effectiveTo = null) works correctly
        
        UUID vehicleId = UUID.randomUUID();
        VehicleTaxProfile profile = createProfile(
                LocalDate.of(2026, 4, 1),
                null);
        
        SarsCostScaleBracket bracket = new SarsCostScaleBracket();
        bracket.setFixedCostCents(100000L);
        bracket.setFuelCostTenthsCentsPerKm(1329L);
        bracket.setMaintenanceCostTenthsCentsPerKm(491L);
        
        when(costScaleBracketRepository.findByTaxYearAndVehicleValue(anyInt(), any()))
                .thenReturn(Optional.of(bracket));
        
        LocalDate taxYearStart = LocalDate.of(2026, 3, 1);
        LocalDate taxYearEnd = LocalDate.of(2027, 2, 28);
        
        var result = taxCalculationService.calculateSarsCostScale(
                profile, taxYearStart, taxYearEnd,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(2000));
        
        assertNotNull(result);
        // Business use days using half-open: [2026-04-01, 2027-03-01)
        // ChronoUnit.DAYS.between(2026-04-01, 2027-03-01) = 334
        assertEquals(334, result.getBusinessUseDays());
    }

    @Test
    void testFindProfileForTaxYear_RepositoryListOrderDoesNotAffectFailure() {
        // Repository list order should not affect the validation failure
        // Test both orderings to ensure deterministic behavior
        
        UUID vehicleId = UUID.randomUUID();
        VehicleTaxProfile profile1 = createProfile(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 9, 1));
        VehicleTaxProfile profile2 = createProfile(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 1));
        
        // Test with profile1 first
        List<VehicleTaxProfile> profilesOrder1 = List.of(profile1, profile2);
        when(taxProfileRepository.findByVehicleId(vehicleId)).thenReturn(profilesOrder1);
        
        ValidationException exception1 = assertThrows(ValidationException.class, () -> {
            taxCalculationService.findProfileForTaxYear(vehicleId, 2027);
        });
        assertTrue(exception1.getMessage().contains("Multiple Vehicle Tax Profiles apply"));
        
        // Test with profile2 first
        List<VehicleTaxProfile> profilesOrder2 = List.of(profile2, profile1);
        when(taxProfileRepository.findByVehicleId(vehicleId)).thenReturn(profilesOrder2);
        
        ValidationException exception2 = assertThrows(ValidationException.class, () -> {
            taxCalculationService.findProfileForTaxYear(vehicleId, 2027);
        });
        assertTrue(exception2.getMessage().contains("Multiple Vehicle Tax Profiles apply"));
    }

    @Test
    void testFindProfileForTaxYear_BoundaryTransitionOutsideAssessmentYear_NoFalseAmbiguity() {
        // Boundary transition outside the assessment year should not create false ambiguity
        // Old profile ends before assessment year starts, new profile starts after assessment year ends
        
        UUID vehicleId = UUID.randomUUID();
        VehicleTaxProfile oldProfile = createProfile(
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2026, 2, 28)); // Ends before 2026-03-01
        VehicleTaxProfile newProfile = createProfile(
                LocalDate.of(2027, 4, 1), // Starts after 2027-03-01
                null);
        
        List<VehicleTaxProfile> profiles = List.of(oldProfile, newProfile);
        when(taxProfileRepository.findByVehicleId(vehicleId)).thenReturn(profiles);
        
        // Neither profile overlaps assessment year 2027 [2026-03-01, 2027-03-01)
        VehicleTaxProfile result = taxCalculationService.findProfileForTaxYear(vehicleId, 2027);
        
        assertNull(result);
    }

    @Test
    void testFindProfileForTaxYear_BoundaryTransitionAtAssessmentYearStart_NoFalseAmbiguity() {
        // Transition exactly at assessment year start should not create false ambiguity
        // Old profile ends on 2026-03-01 (exclusive), new profile starts on 2026-03-01 (inclusive)
        
        UUID vehicleId = UUID.randomUUID();
        VehicleTaxProfile oldProfile = createProfile(
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2026, 3, 1)); // Exclusive boundary
        VehicleTaxProfile newProfile = createProfile(
                LocalDate.of(2026, 3, 1), // Inclusive boundary
                null);
        
        List<VehicleTaxProfile> profiles = List.of(oldProfile, newProfile);
        when(taxProfileRepository.findByVehicleId(vehicleId)).thenReturn(profiles);
        
        // Only newProfile overlaps assessment year 2027 [2026-03-01, 2027-03-01)
        VehicleTaxProfile result = taxCalculationService.findProfileForTaxYear(vehicleId, 2027);
        
        assertNotNull(result);
        assertEquals(newProfile.getId(), result.getId());
    }

    private VehicleTaxProfile createProfile(LocalDate effectiveFrom, LocalDate effectiveTo) {
        return VehicleTaxProfile.builder()
                .id(UUID.randomUUID())
                .vehicleCostCents(50000000L)
                .datePlacedInBusinessUse(LocalDate.of(2025, 1, 1))
                .taxpayerVatRegistered(false)
                .taxpayerType(TaxpayerType.EMPLOYEE)
                .compensationType(CompensationType.TRAVEL_ALLOWANCE)
                .fuelBorneBy("EMPLOYEE")
                .maintenanceBorneBy("EMPLOYEE")
                .coveredByMaintenancePlan(false)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .defaultCalculationMethod(TaxCalculationMethod.ACTUAL_COSTS)
                .isCompanyProvidedVehicle(false)
                .build();
    }
}
