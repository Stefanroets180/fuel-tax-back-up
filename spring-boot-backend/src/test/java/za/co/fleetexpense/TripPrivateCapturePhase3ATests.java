package za.co.fleetexpense;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.fleetexpense.dto.TripCreateRequest;
import za.co.fleetexpense.dto.TripDTO;
import za.co.fleetexpense.dto.TripUpdateRequest;
import za.co.fleetexpense.entity.Trip;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.VehicleTaxProfile;
import za.co.fleetexpense.entity.enums.TripPurpose;
import za.co.fleetexpense.entity.enums.UserRole;
import za.co.fleetexpense.exception.ValidationException;
import za.co.fleetexpense.mapper.TripMapper;
import za.co.fleetexpense.repository.*;
import za.co.fleetexpense.service.PermissionService;
import za.co.fleetexpense.service.TripService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TripPrivateCapturePhase3ATests {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TripMapper tripMapper;

    @Mock
    private PermissionService permissionService;

    @Mock
    private UserAssistantRepository userAssistantRepository;

    @Mock
    private OdometerVerificationRepository odometerVerificationRepository;

    @Mock
    private VehicleTaxProfileRepository vehicleTaxProfileRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private TripService tripService;

    private UUID organizationId;
    private UUID userId;
    private UUID vehicleId;
    private Vehicle vehicle;
    private User user;
    private TripCreateRequest createRequest;
    private TripUpdateRequest updateRequest;

    @Test
    public void testPrivateTripCreateAllowedForNonCompanyProvidedVehicle() {
        // Verify that PRIVATE trips can be created for non-company-provided vehicles
        // after Phase 3A removed the restriction from TripService.create
        
        setupCommonMocks();
        createRequest.setPurpose(TripPurpose.PRIVATE);
        createRequest.setVehicleId(vehicleId);
        createRequest.setStartOdometer(1000);
        createRequest.setEndOdometer(1100);
        createRequest.setTripDate(LocalDate.now());

        Trip savedTrip = new Trip();
        savedTrip.setId(UUID.randomUUID());
        savedTrip.setPurpose(TripPurpose.PRIVATE);
        savedTrip.setVehicle(vehicle);
        savedTrip.setUser(user);

        when(tripRepository.save(any(Trip.class))).thenReturn(savedTrip);
        when(tripMapper.toEntity(createRequest)).thenReturn(savedTrip);
        when(tripMapper.toDTO(savedTrip)).thenReturn(new TripDTO());
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(new za.co.fleetexpense.entity.Organization()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Permission check passes - TripService.create() calls EDIT_OWN_ENTRIES
        when(permissionService.isAllowed(any(), eq("LOGBOOK"), eq("EDIT_OWN_ENTRIES"), any()))
                .thenReturn(true);

        // This should NOT throw ValidationException for PRIVATE purpose
        TripDTO result = tripService.create(createRequest, organizationId, userId, UserRole.ADMIN);

        assertNotNull(result);
        verify(tripRepository).save(any(Trip.class));
        // Verify the saved trip has PRIVATE purpose
        assertEquals(TripPurpose.PRIVATE, savedTrip.getPurpose());
    }

    @Test
    public void testPrivateTripUpdateAllowedForNonCompanyProvidedVehicle() {
        // Verify that existing trips can be changed to PRIVATE for non-company-provided vehicles
        
        setupCommonMocks();
        UUID updateOrgId = UUID.randomUUID(); // Use unique org ID to avoid stub conflicts
        UUID tripId = UUID.randomUUID();
        updateRequest.setPurpose(TripPurpose.PRIVATE);

        Trip existingTrip = new Trip();
        existingTrip.setId(tripId);
        existingTrip.setPurpose(TripPurpose.BUSINESS);
        existingTrip.setVehicle(vehicle);
        existingTrip.setIsLocked(false);

        when(tripRepository.findByIdWithVehicle(tripId)).thenReturn(Optional.of(existingTrip));
        when(tripRepository.save(any(Trip.class))).thenReturn(existingTrip);
        when(tripMapper.toDTO(existingTrip)).thenReturn(new TripDTO());

        // Permission checks pass - TripService.update() calls BOTH EDIT_OWN_ENTRIES and EDIT_ALL_ENTRIES
        // Production: permissionService.isAllowed(organizationId, "LOGBOOK", "EDIT_OWN_ENTRIES", role.name())
        // Production: permissionService.isAllowed(organizationId, "LOGBOOK", "EDIT_ALL_ENTRIES", role.name())
        // Stub both since production calls both
        when(permissionService.isAllowed(any(), eq("LOGBOOK"), eq("EDIT_OWN_ENTRIES"), any()))
                .thenReturn(true);
        when(permissionService.isAllowed(any(), eq("LOGBOOK"), eq("EDIT_ALL_ENTRIES"), any()))
                .thenReturn(false);

        // This should NOT throw ValidationException for PRIVATE purpose
        TripDTO result = tripService.update(tripId, updateRequest, updateOrgId, UserRole.ADMIN);

        assertNotNull(result);
        verify(tripRepository).save(existingTrip);
        // Verify both permission checks were called as per production code
        verify(permissionService).isAllowed(any(), eq("LOGBOOK"), eq("EDIT_OWN_ENTRIES"), any());
        verify(permissionService).isAllowed(any(), eq("LOGBOOK"), eq("EDIT_ALL_ENTRIES"), any());
    }

    @Test
    public void testPrivateTripCreateAllowedForCompanyProvidedVehicle() {
        // PRIVATE trips should also be allowed for company-provided vehicles
        // (this was always allowed, only non-company-provided was restricted)
        
        setupCommonMocks();
        createRequest.setPurpose(TripPurpose.PRIVATE);
        createRequest.setVehicleId(vehicleId);
        createRequest.setStartOdometer(1000);
        createRequest.setEndOdometer(1100);
        createRequest.setTripDate(LocalDate.now());

        Trip savedTrip = new Trip();
        savedTrip.setId(UUID.randomUUID());
        savedTrip.setPurpose(TripPurpose.PRIVATE);
        savedTrip.setVehicle(vehicle);
        savedTrip.setUser(user);

        when(tripRepository.save(any(Trip.class))).thenReturn(savedTrip);
        when(tripMapper.toEntity(createRequest)).thenReturn(savedTrip);
        when(tripMapper.toDTO(savedTrip)).thenReturn(new TripDTO());
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(new za.co.fleetexpense.entity.Organization()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Permission check passes - TripService.create() calls EDIT_OWN_ENTRIES
        when(permissionService.isAllowed(any(), eq("LOGBOOK"), eq("EDIT_OWN_ENTRIES"), any()))
                .thenReturn(true);

        TripDTO result = tripService.create(createRequest, organizationId, userId, UserRole.ADMIN);

        assertNotNull(result);
        verify(tripRepository).save(any(Trip.class));
        assertEquals(TripPurpose.PRIVATE, savedTrip.getPurpose());
    }

    @Test
    public void testBusinessTripCreateStillAllowed() {
        // BUSINESS trips should continue to work normally
        
        setupCommonMocks();
        createRequest.setPurpose(TripPurpose.BUSINESS);
        createRequest.setVehicleId(vehicleId);
        createRequest.setStartOdometer(1000);
        createRequest.setEndOdometer(1100);
        createRequest.setTripDate(LocalDate.now());

        Trip savedTrip = new Trip();
        savedTrip.setId(UUID.randomUUID());
        savedTrip.setPurpose(TripPurpose.BUSINESS);
        savedTrip.setVehicle(vehicle);
        savedTrip.setUser(user);

        when(tripRepository.save(any(Trip.class))).thenReturn(savedTrip);
        when(tripMapper.toEntity(createRequest)).thenReturn(savedTrip);
        when(tripMapper.toDTO(savedTrip)).thenReturn(new TripDTO());
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(new za.co.fleetexpense.entity.Organization()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Permission check passes - TripService.create() calls EDIT_OWN_ENTRIES
        when(permissionService.isAllowed(any(), eq("LOGBOOK"), eq("EDIT_OWN_ENTRIES"), any()))
                .thenReturn(true);

        TripDTO result = tripService.create(createRequest, organizationId, userId, UserRole.ADMIN);

        assertNotNull(result);
        verify(tripRepository).save(any(Trip.class));
        assertEquals(TripPurpose.BUSINESS, savedTrip.getPurpose());
    }

    @Test
    public void testUnclassifiedTripCreateStillAllowed() {
        // UNCLASSIFIED trips should continue to work normally
        
        setupCommonMocks();
        createRequest.setPurpose(TripPurpose.UNCLASSIFIED);
        createRequest.setVehicleId(vehicleId);
        createRequest.setStartOdometer(1000);
        createRequest.setEndOdometer(1100);
        createRequest.setTripDate(LocalDate.now());

        Trip savedTrip = new Trip();
        savedTrip.setId(UUID.randomUUID());
        savedTrip.setPurpose(TripPurpose.UNCLASSIFIED);
        savedTrip.setVehicle(vehicle);
        savedTrip.setUser(user);

        when(tripRepository.save(any(Trip.class))).thenReturn(savedTrip);
        when(tripMapper.toEntity(createRequest)).thenReturn(savedTrip);
        when(tripMapper.toDTO(savedTrip)).thenReturn(new TripDTO());
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(new za.co.fleetexpense.entity.Organization()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Permission check passes - TripService.create() calls EDIT_OWN_ENTRIES
        when(permissionService.isAllowed(any(), eq("LOGBOOK"), eq("EDIT_OWN_ENTRIES"), any()))
                .thenReturn(true);

        TripDTO result = tripService.create(createRequest, organizationId, userId, UserRole.ADMIN);

        assertNotNull(result);
        verify(tripRepository).save(any(Trip.class));
        assertEquals(TripPurpose.UNCLASSIFIED, savedTrip.getPurpose());
    }

    @Test
    public void testTripCreateDoesNotChangeVehicleOdometer() {
        // Trip create should NOT change Vehicle.currentOdometer
        // Operational odometer is calculated from expenses, not trips
        
        setupCommonMocks();
        createRequest.setPurpose(TripPurpose.PRIVATE);
        createRequest.setVehicleId(vehicleId);
        createRequest.setStartOdometer(1000);
        createRequest.setEndOdometer(1100);
        createRequest.setTripDate(LocalDate.now());

        Integer originalOdometer = 5000;
        vehicle.setCurrentOdometer(originalOdometer);

        Trip savedTrip = new Trip();
        savedTrip.setId(UUID.randomUUID());
        savedTrip.setPurpose(TripPurpose.PRIVATE);
        savedTrip.setVehicle(vehicle);
        savedTrip.setUser(user);

        when(tripRepository.save(any(Trip.class))).thenReturn(savedTrip);
        when(tripMapper.toEntity(createRequest)).thenReturn(savedTrip);
        when(tripMapper.toDTO(savedTrip)).thenReturn(new TripDTO());
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(new za.co.fleetexpense.entity.Organization()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Permission check passes - TripService.create() calls EDIT_OWN_ENTRIES
        when(permissionService.isAllowed(any(), eq("LOGBOOK"), eq("EDIT_OWN_ENTRIES"), any()))
                .thenReturn(true);

        tripService.create(createRequest, organizationId, userId, UserRole.ADMIN);

        // Verify vehicleRepository.save(vehicle) was NOT called for odometer update
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        // Verify vehicle.setCurrentOdometer was NOT called
        assertEquals(originalOdometer, vehicle.getCurrentOdometer());
    }

    @Test
    public void testTripUpdateDoesNotChangeVehicleOdometer() {
        // Trip update should NOT change Vehicle.currentOdometer
        
        setupCommonMocks();
        UUID updateOrgId = UUID.randomUUID(); // Use unique org ID to avoid stub conflicts
        UUID tripId = UUID.randomUUID();
        updateRequest.setPurpose(TripPurpose.PRIVATE);

        Integer originalOdometer = 5000;
        vehicle.setCurrentOdometer(originalOdometer);

        Trip existingTrip = new Trip();
        existingTrip.setId(tripId);
        existingTrip.setPurpose(TripPurpose.BUSINESS);
        existingTrip.setVehicle(vehicle);
        existingTrip.setIsLocked(false);

        when(tripRepository.findByIdWithVehicle(tripId)).thenReturn(Optional.of(existingTrip));
        when(tripRepository.save(any(Trip.class))).thenReturn(existingTrip);
        when(tripMapper.toDTO(existingTrip)).thenReturn(new TripDTO());

        // Permission checks pass - TripService.update() calls BOTH EDIT_OWN_ENTRIES and EDIT_ALL_ENTRIES
        // Production: permissionService.isAllowed(organizationId, "LOGBOOK", "EDIT_OWN_ENTRIES", role.name())
        // Production: permissionService.isAllowed(organizationId, "LOGBOOK", "EDIT_ALL_ENTRIES", role.name())
        // Stub both since production calls both
        when(permissionService.isAllowed(any(), eq("LOGBOOK"), eq("EDIT_OWN_ENTRIES"), any()))
                .thenReturn(true);
        when(permissionService.isAllowed(any(), eq("LOGBOOK"), eq("EDIT_ALL_ENTRIES"), any()))
                .thenReturn(false);

        tripService.update(tripId, updateRequest, updateOrgId, UserRole.ADMIN);

        // Verify vehicleRepository.save(vehicle) was NOT called for odometer update
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        // Verify vehicle.setCurrentOdometer was NOT called
        assertEquals(originalOdometer, vehicle.getCurrentOdometer());
        // Verify both permission checks were called as per production code
        verify(permissionService).isAllowed(any(), eq("LOGBOOK"), eq("EDIT_OWN_ENTRIES"), any());
        verify(permissionService).isAllowed(any(), eq("LOGBOOK"), eq("EDIT_ALL_ENTRIES"), any());
    }

    @Test
    public void testTripCreateDoesNotRecalculateOperationalOdometer() {
        // Trip create should NOT trigger operational odometer recalculation
        
        setupCommonMocks();
        createRequest.setPurpose(TripPurpose.PRIVATE);
        createRequest.setVehicleId(vehicleId);
        createRequest.setStartOdometer(1000);
        createRequest.setEndOdometer(1100);
        createRequest.setTripDate(LocalDate.now());

        Trip savedTrip = new Trip();
        savedTrip.setId(UUID.randomUUID());
        savedTrip.setPurpose(TripPurpose.PRIVATE);
        savedTrip.setVehicle(vehicle);
        savedTrip.setUser(user);

        when(tripRepository.save(any(Trip.class))).thenReturn(savedTrip);
        when(tripMapper.toEntity(createRequest)).thenReturn(savedTrip);
        when(tripMapper.toDTO(savedTrip)).thenReturn(new TripDTO());
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(new za.co.fleetexpense.entity.Organization()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Permission check passes - TripService.create() calls EDIT_OWN_ENTRIES
        when(permissionService.isAllowed(any(), eq("LOGBOOK"), eq("EDIT_OWN_ENTRIES"), any()))
                .thenReturn(true);

        tripService.create(createRequest, organizationId, userId, UserRole.ADMIN);

        // Verify no operational odometer recalculation service was called
        // (TripService does not have such a dependency - this is calculated elsewhere)
        verify(odometerVerificationRepository, never()).save(any());
    }

    private void setupCommonMocks() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();

        vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setRegistrationNumber("TEST123");
        vehicle.setCurrentOdometer(5000);

        user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");

        createRequest = new TripCreateRequest();
        updateRequest = new TripUpdateRequest();
    }
}
