package za.co.fleetexpense.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.TaxYearSummary;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.VehicleTaxProfile;
import za.co.fleetexpense.exception.ResourceNotFoundException;
import za.co.fleetexpense.exception.ValidationException;
import za.co.fleetexpense.mapper.TaxYearSummaryMapper;
import za.co.fleetexpense.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxYearSummaryServicePhase4E1Test {

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

    @Mock
    private TaxCalculationService taxCalculationService;

    @InjectMocks
    private TaxYearSummaryService taxYearSummaryService;

    private UUID vehicleId;
    private UUID organizationId;
    private Vehicle vehicle;
    private Organization organization;
    private VehicleTaxProfile oldProfile;
    private VehicleTaxProfile currentProfile;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        organization = new Organization();
        organization.setId(organizationId);
        organization.setName("Test Org");

        vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setRegistrationNumber("ABC123");
        vehicle.setOrganization(organization);

        // Old profile: 2026-03-01 to 2027-03-01 (applies to assessment year 2027)
        oldProfile = new VehicleTaxProfile();
        oldProfile.setId(UUID.randomUUID());
        oldProfile.setVehicle(vehicle);
        oldProfile.setEffectiveFrom(LocalDate.of(2026, Month.MARCH, 1));
        oldProfile.setEffectiveTo(LocalDate.of(2027, Month.MARCH, 1));
        oldProfile.setVehicleCostCents(50000000L); // R500,000
        oldProfile.setIsCompanyProvidedVehicle(false);

        // Current profile: 2027-03-01 to null (applies to assessment year 2028+)
        currentProfile = new VehicleTaxProfile();
        currentProfile.setId(UUID.randomUUID());
        currentProfile.setVehicle(vehicle);
        currentProfile.setEffectiveFrom(LocalDate.of(2027, Month.MARCH, 1));
        currentProfile.setEffectiveTo(null);
        currentProfile.setVehicleCostCents(60000000L); // R600,000
        currentProfile.setIsCompanyProvidedVehicle(true);
    }

    @Test
    void testHistoricalSummarySelection_AssessmentYear2027_SelectsOldProfile() {
        // Given: assessment year 2027, old profile applies
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027)).thenReturn(Optional.empty());
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(taxYearSummaryMapper.toDTO(any())).thenReturn(new za.co.fleetexpense.dto.TaxYearSummaryDTO());
        when(tripService.getTotalKmForTaxYearWithSource(any(), anyInt()))
                .thenReturn(new TripService.DistanceCalculationResult(10000, 8000, 2000, 0, 0, null, List.of()));
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(any(), any(), any())).thenReturn(0L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(odometerVerificationRepository.findByVehicleYearAndType(any(), any(), any())).thenReturn(Optional.empty());

        // When
        taxYearSummaryService.calculateSummary(vehicleId, 2027, organizationId);

        // Then: old profile was selected for assessment year 2027
        verify(taxCalculationService).findProfileForTaxYear(vehicleId, 2027);
        verify(taxCalculationService, never()).findProfileForTaxYear(vehicleId, 2028);
    }

    @Test
    void testCurrentAssessmentYear_AssessmentYear2028_SelectsCurrentProfile() {
        // Given: assessment year 2028, current profile applies
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2028)).thenReturn(currentProfile);
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2028)).thenReturn(Optional.empty());
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(taxYearSummaryMapper.toDTO(any())).thenReturn(new za.co.fleetexpense.dto.TaxYearSummaryDTO());
        when(tripService.getTotalKmForTaxYearWithSource(any(), anyInt()))
                .thenReturn(new TripService.DistanceCalculationResult(10000, 8000, 2000, 0, 0, null, List.of()));
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(any(), any(), any())).thenReturn(0L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(odometerVerificationRepository.findByVehicleYearAndType(any(), any(), any())).thenReturn(Optional.empty());

        // When
        taxYearSummaryService.calculateSummary(vehicleId, 2028, organizationId);

        // Then: current profile was selected
        verify(taxCalculationService).findProfileForTaxYear(vehicleId, 2028);
    }

    @Test
    void testMultipleOverlappingProfiles_ThrowsValidationException() {
        // Given: multiple profiles overlap assessment year
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027))
                .thenThrow(new ValidationException("Multiple Vehicle Tax Profiles apply within assessment year 2027"));
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027)).thenReturn(Optional.empty());
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        // When/Then: ValidationException is thrown
        assertThrows(ValidationException.class, () -> 
            taxYearSummaryService.calculateSummary(vehicleId, 2027, organizationId)
        );
    }

    @Test
    void testNoProfile_PreservesExistingBehavior() {
        // Given: no profile applies to assessment year
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(null);
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027)).thenReturn(Optional.empty());
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(taxYearSummaryMapper.toDTO(any())).thenReturn(new za.co.fleetexpense.dto.TaxYearSummaryDTO());
        when(tripService.getTotalKmForTaxYearWithSource(any(), anyInt()))
                .thenReturn(new TripService.DistanceCalculationResult(10000, 8000, 2000, 0, 0, null, List.of()));
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(any(), any(), any())).thenReturn(0L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(odometerVerificationRepository.findByVehicleYearAndType(any(), any(), any())).thenReturn(Optional.empty());

        // When/Then: should not throw, preserves existing no-profile behavior
        assertDoesNotThrow(() -> 
            taxYearSummaryService.calculateSummary(vehicleId, 2027, organizationId)
        );
    }

    @Test
    void testHistoricalProfileSelection_DoesNotUseFindByVehicleIdAndEffectiveToIsNull() {
        // Given: historical profile selection
        when(taxCalculationService.findProfileForTaxYear(vehicleId, 2027)).thenReturn(oldProfile);
        when(taxYearSummaryRepository.findByVehicleIdAndTaxYear(vehicleId, 2027)).thenReturn(Optional.empty());
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(taxYearSummaryMapper.toDTO(any())).thenReturn(new za.co.fleetexpense.dto.TaxYearSummaryDTO());
        when(tripService.getTotalKmForTaxYearWithSource(any(), anyInt()))
                .thenReturn(new TripService.DistanceCalculationResult(10000, 8000, 2000, 0, 0, null, List.of()));
        when(tripRepository.countUnclassifiedTripsByVehicleAndTaxYear(any(), any(), any())).thenReturn(0L);
        when(expenseRepository.findByVehicleId(vehicleId)).thenReturn(List.of());
        when(odometerVerificationRepository.findByVehicleYearAndType(any(), any(), any())).thenReturn(Optional.empty());

        // When
        taxYearSummaryService.calculateSummary(vehicleId, 2027, organizationId);

        // Then: findByVehicleIdAndEffectiveToIsNull should NOT be called
        verify(vehicleTaxProfileRepository, never()).findByVehicleIdAndEffectiveToIsNull(any());
    }
}
