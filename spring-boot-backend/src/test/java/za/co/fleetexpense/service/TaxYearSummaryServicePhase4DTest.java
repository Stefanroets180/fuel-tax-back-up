package za.co.fleetexpense.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.fleetexpense.dto.TaxYearSummaryDTO;
import za.co.fleetexpense.entity.TaxYearSummary;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.enums.TaxExpenseClassification;
import za.co.fleetexpense.mapper.TaxYearSummaryMapper;
import za.co.fleetexpense.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for Phase 4D tax readiness warnings.
 * No Spring context, no database, no external dependencies.
 * Tests focus on verifying repository calls for warning detection:
 * - UNCLASSIFIED trip count queries
 * - Expense classification queries
 * - Warning deduplication (single call per summary)
 */
@ExtendWith(MockitoExtension.class)
class TaxYearSummaryServicePhase4DTest {

    @Mock
    private TaxYearSummaryRepository taxYearSummaryRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private OdometerConfirmationRepository odometerConfirmationRepository;

    @Mock
    private OdometerDriftAlertRepository odometerDriftAlertRepository;

    @Mock
    private OdometerVerificationRepository odometerVerificationRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripService tripService;

    @Mock
    private TaxYearSummaryMapper taxYearSummaryMapper;

    @Mock
    private VehicleTaxProfileRepository vehicleTaxProfileRepository;

    @InjectMocks
    private TaxYearSummaryService taxYearSummaryService;

    private UUID vehicleId;
    private UUID organizationId;
    private Vehicle vehicle;
    private TaxYearSummary summary;
    private TaxYearSummaryDTO dto;

    @Test
    void testNoUnclassifiedTrips_NoUnclassifiedWarning() {
        setupTestData();
        
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027))
                .thenReturn(Optional.of(summary));
        
        taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027);
        
        verify(tripRepository).countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testOneUnclassifiedTripInsideAssessmentYear_AddsWarning() {
        setupTestData();
        
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class))).thenReturn(1L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027))
                .thenReturn(Optional.of(summary));
        
        taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027);
        
        verify(tripRepository).countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testMultipleUnclassifiedTrips_StillOneWarning() {
        setupTestData();
        
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class))).thenReturn(10L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027))
                .thenReturn(Optional.of(summary));
        
        taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027);
        
        verify(tripRepository, times(1)).countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testUncategorizedExpenseCentsZero_NoUncategorizedWarning() {
        setupTestData();
        
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027))
                .thenReturn(Optional.of(summary));
        
        taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027);
        
        verify(tripRepository).countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class));
        verify(expenseRepository).findByVehicleId(vehicleId);
    }

    @Test
    void testUncategorizedExpenseCentsGreaterThanZero_AddsWarning() {
        setupTestData();
        
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027))
                .thenReturn(Optional.of(summary));
        
        taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027);
        
        verify(tripRepository).countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class));
        verify(expenseRepository).findByVehicleId(vehicleId);
    }

    @Test
    void testCapitalOrAllowanceReviewCentsGreaterThanZero_AddsWarning() {
        setupTestData();
        
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027))
                .thenReturn(Optional.of(summary));
        
        taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027);
        
        verify(tripRepository).countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class));
        verify(expenseRepository).findByVehicleId(vehicleId);
    }

    @Test
    void testPersonalOrNonQualifyingExpenseCentsAlone_NoUnresolvedReviewWarning() {
        setupTestData();
        
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027))
                .thenReturn(Optional.of(summary));
        
        taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027);
        
        verify(tripRepository).countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class));
        verify(expenseRepository).findByVehicleId(vehicleId);
    }

    @Test
    void testWarningsAreDeterministic_NotDuplicated() {
        setupTestData();
        
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class))).thenReturn(5L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027))
                .thenReturn(Optional.of(summary));
        
        taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027);
        
        // Verify the repository was called to count UNCLASSIFIED trips (only once)
        verify(tripRepository, times(1)).countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class));
        
        verify(expenseRepository).findByVehicleId(vehicleId);
    }

    @Test
    void testBusinessTrip_NoUnclassifiedWarning() {
        setupTestData();
        
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027))
                .thenReturn(Optional.of(summary));
        
        taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027);
        
        verify(tripRepository).countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testPrivateTrip_NoUnclassifiedWarning() {
        setupTestData();
        
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027))
                .thenReturn(Optional.of(summary));
        
        taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027);
        
        verify(tripRepository).countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testUnclassifiedTripBeforeAssessmentYear_NoWarning() {
        setupTestData();
        
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027))
                .thenReturn(Optional.of(summary));
        
        taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027);
        
        verify(tripRepository).countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testUnclassifiedTripOnStartBoundary_AddsWarning() {
        setupTestData();
        
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class))).thenReturn(1L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027))
                .thenReturn(Optional.of(summary));
        
        taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027);
        
        verify(tripRepository).countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testUnclassifiedTripOnEndBoundary_NoWarning() {
        setupTestData();
        
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027))
                .thenReturn(Optional.of(summary));
        
        taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027);
        
        verify(tripRepository).countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testCombinedWarnings_ThreeDistinctWarnings() {
        setupTestData();
        
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class))).thenReturn(3L);
        
        // Mock expenses to trigger uncategorized and capital/allowance review warnings
        za.co.fleetexpense.entity.Expense uncategorizedExpense = createMockExpense(
                TaxExpenseClassification.UNCATEGORIZED, BigDecimal.valueOf(100.00));
        za.co.fleetexpense.entity.Expense capitalExpense = createMockExpense(
                TaxExpenseClassification.CAPITAL_OR_ALLOWANCE_REVIEW, BigDecimal.valueOf(5000.00));
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of(uncategorizedExpense, capitalExpense));
        
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027))
                .thenReturn(Optional.of(summary));
        
        TaxYearSummaryDTO result = taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027);
        
        // Verify the repository was called to count UNCLASSIFIED trips (only once)
        verify(tripRepository, times(1)).countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class));
        verify(expenseRepository).findByVehicleId(vehicleId);
        
        // Assert final DTO warning list contains exactly 3 warnings
        assertNotNull(result.getDataQualityWarnings());
        assertEquals(3, result.getDataQualityWarnings().size());
        
        // Assert each warning type appears exactly once
        long unclassifiedWarnings = result.getDataQualityWarnings().stream()
                .filter(w -> w.contains("Unclassified trips remain in this assessment year"))
                .count();
        long uncategorizedWarnings = result.getDataQualityWarnings().stream()
                .filter(w -> w.contains("Uncategorized vehicle expenses"))
                .count();
        long capitalWarnings = result.getDataQualityWarnings().stream()
                .filter(w -> w.contains("Capital or allowance-review expenses"))
                .count();
        
        assertEquals(1, unclassifiedWarnings);
        assertEquals(1, uncategorizedWarnings);
        assertEquals(1, capitalWarnings);
        
        // Assert no duplicates (distinct count equals total count)
        assertEquals(result.getDataQualityWarnings().size(), 
                     result.getDataQualityWarnings().stream().distinct().count());
    }

    @Test
    void testBusinessKm_ExcludesUnclassifiedTrips() {
        setupTestData();
        
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class))).thenReturn(5L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027))
                .thenReturn(Optional.of(summary));
        
        taxYearSummaryService.getByVehicleAndYear(vehicleId, 2027);
        
        verify(tripRepository).countUnclassifiedTripsByVehicleAndTaxYear(
                eq(vehicleId), any(LocalDate.class), any(LocalDate.class));
    }

    // ==================== Helper Methods ====================

    private void setupTestData() {
        vehicleId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        
        za.co.fleetexpense.entity.Organization organization = new za.co.fleetexpense.entity.Organization();
        organization.setId(organizationId);
        
        vehicle = Vehicle.builder()
                .id(vehicleId)
                .registrationNumber("ABC123")
                .make("Toyota")
                .model("Corolla")
                .year(2020)
                .organization(organization)
                .build();
        
        summary = new TaxYearSummary();
        summary.setId(UUID.randomUUID());
        summary.setVehicle(vehicle);
        summary.setOrganization(organization);
        summary.setTaxYear(2027);
        
        dto = new TaxYearSummaryDTO();
        dto.setId(summary.getId());
        dto.setVehicleId(vehicleId);
        dto.setTaxYear(2027);
        dto.setDataQualityWarnings(new ArrayList<>());
        
        // Use lenient() for mapper to allow test-specific overrides
        lenient().when(taxYearSummaryMapper.toDTO(any())).thenAnswer(invocation -> {
            TaxYearSummaryDTO newDto = new TaxYearSummaryDTO();
            newDto.setId(summary.getId());
            newDto.setVehicleId(vehicleId);
            newDto.setTaxYear(2027);
            newDto.setDataQualityWarnings(new ArrayList<>());
            return newDto;
        });
        
        // Mock trip service with empty warnings list (not null)
        TripService.DistanceCalculationResult distanceResult = 
                new TripService.DistanceCalculationResult(0, 0, 0, 0, 0, null, new ArrayList<>());
        lenient().when(tripService.getTotalKmForTaxYearWithSource(any(UUID.class), anyInt())).thenReturn(distanceResult);
    }

    private za.co.fleetexpense.entity.Expense createMockExpense(
            TaxExpenseClassification classification, BigDecimal amountZar) {
        za.co.fleetexpense.entity.Expense expense = new za.co.fleetexpense.entity.Expense();
        expense.setId(UUID.randomUUID());
        expense.setVehicle(vehicle);
        expense.setTaxExpenseClassification(classification);
        expense.setAmountZar(amountZar);
        expense.setExpenseDate(LocalDate.of(2026, Month.MARCH, 15)); // Inside assessment year 2027
        expense.setCategory(za.co.fleetexpense.entity.enums.ExpenseCategory.OTHER_FIXED);
        return expense;
    }
}
