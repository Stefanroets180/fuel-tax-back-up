package za.co.fleetexpense.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import za.co.fleetexpense.dto.VehicleTaxProfileCreateRequest;
import za.co.fleetexpense.dto.VehicleTaxProfileDTO;
import za.co.fleetexpense.dto.VehicleTaxProfileUpdateRequest;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.VehicleTaxProfile;
import za.co.fleetexpense.entity.enums.CompensationType;
import za.co.fleetexpense.entity.enums.TaxCalculationMethod;
import za.co.fleetexpense.entity.enums.TaxpayerType;
import za.co.fleetexpense.entity.enums.UserRole;
import za.co.fleetexpense.repository.OrganizationRepository;
import za.co.fleetexpense.repository.UserRepository;
import za.co.fleetexpense.repository.VehicleRepository;
import za.co.fleetexpense.repository.VehicleTaxProfileRepository;
import za.co.fleetexpense.security.UserPrincipal;
import za.co.fleetexpense.service.PermissionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleTaxProfileControllerPhase4G3ATest {

    @Mock
    private VehicleTaxProfileRepository vehicleTaxProfileRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private VehicleTaxProfileController controller;

    private UUID organizationId;
    private UUID vehicleId;
    private UUID recipientUserId;
    private UUID otherRecipientUserId;
    private UserPrincipal principal;
    private Vehicle vehicle;
    private Organization organization;
    private User recipientUser;
    private User otherRecipientUser;
    private User authenticatedUser;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        recipientUserId = UUID.randomUUID();
        otherRecipientUserId = UUID.randomUUID();

        organization = Organization.builder()
                .id(organizationId)
                .name("Test Org")
                .build();

        recipientUser = User.builder()
                .id(recipientUserId)
                .organization(organization)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();

        otherRecipientUser = User.builder()
                .id(otherRecipientUserId)
                .organization(organization)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .build();

        authenticatedUser = User.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .email("test@example.com")
                .passwordHash("password")
                .role(UserRole.ADMIN)
                .build();

        principal = new UserPrincipal(authenticatedUser);

        vehicle = Vehicle.builder()
                .id(vehicleId)
                .organization(organization)
                .registrationNumber("ABC123")
                .build();
    }

    // Test A: EMPLOYEE profile create with valid same-org recipient succeeds
    @Test
    void testA_EmployeeProfileCreateWithValidSameOrgRecipientSucceeds() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(userRepository.findById(principal.getUserId())).thenReturn(Optional.of(recipientUser));
        when(userRepository.findById(recipientUserId)).thenReturn(Optional.of(recipientUser));
        when(permissionService.isAllowed(any(), any(), any(), any())).thenReturn(true);
        when(vehicleTaxProfileRepository.findByVehicleIdAndEffectiveToIsNull(vehicleId)).thenReturn(Optional.empty());
        when(vehicleTaxProfileRepository.save(any(VehicleTaxProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleTaxProfileCreateRequest request = VehicleTaxProfileCreateRequest.builder()
                .vehicleId(vehicleId)
                .vehicleCostCents(50000000L)
                .datePlacedInBusinessUse(LocalDate.of(2025, 1, 1))
                .taxpayerVatRegistered(false)
                .taxpayerType(TaxpayerType.EMPLOYEE)
                .compensationType(CompensationType.TRAVEL_ALLOWANCE)
                .fuelBorneBy("EMPLOYEE")
                .maintenanceBorneBy("EMPLOYEE")
                .coveredByMaintenancePlan(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .recipientUserId(recipientUserId)
                .build();

        ResponseEntity<VehicleTaxProfileDTO> response = controller.createTaxProfile(vehicleId, request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(recipientUserId, response.getBody().getRecipientUserId());
    }

    // Test B: SOLE_PROPRIETOR profile create with valid same-org recipient succeeds
    @Test
    void testB_SoleProprietorProfileCreateWithValidSameOrgRecipientSucceeds() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(userRepository.findById(principal.getUserId())).thenReturn(Optional.of(recipientUser));
        when(userRepository.findById(recipientUserId)).thenReturn(Optional.of(recipientUser));
        when(permissionService.isAllowed(any(), any(), any(), any())).thenReturn(true);
        when(vehicleTaxProfileRepository.findByVehicleIdAndEffectiveToIsNull(vehicleId)).thenReturn(Optional.empty());
        when(vehicleTaxProfileRepository.save(any(VehicleTaxProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleTaxProfileCreateRequest request = VehicleTaxProfileCreateRequest.builder()
                .vehicleId(vehicleId)
                .vehicleCostCents(50000000L)
                .datePlacedInBusinessUse(LocalDate.of(2025, 1, 1))
                .taxpayerVatRegistered(false)
                .taxpayerType(TaxpayerType.SOLE_PROPRIETOR)
                .compensationType(CompensationType.REIMBURSEMENT)
                .fuelBorneBy("SELF")
                .maintenanceBorneBy("SELF")
                .coveredByMaintenancePlan(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .recipientUserId(recipientUserId)
                .build();

        ResponseEntity<VehicleTaxProfileDTO> response = controller.createTaxProfile(vehicleId, request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(recipientUserId, response.getBody().getRecipientUserId());
    }

    // Test C: EMPLOYEE/SOLE_PROP create without recipient fails explicitly
    @Test
    void testC_EmployeeOrSolePropCreateWithoutRecipientFails() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(userRepository.findById(principal.getUserId())).thenReturn(Optional.of(recipientUser));
        when(permissionService.isAllowed(any(), any(), any(), any())).thenReturn(true);
        when(vehicleTaxProfileRepository.findByVehicleIdAndEffectiveToIsNull(vehicleId)).thenReturn(Optional.empty());

        VehicleTaxProfileCreateRequest request = VehicleTaxProfileCreateRequest.builder()
                .vehicleId(vehicleId)
                .vehicleCostCents(50000000L)
                .datePlacedInBusinessUse(LocalDate.of(2025, 1, 1))
                .taxpayerVatRegistered(false)
                .taxpayerType(TaxpayerType.EMPLOYEE)
                .compensationType(CompensationType.TRAVEL_ALLOWANCE)
                .fuelBorneBy("EMPLOYEE")
                .maintenanceBorneBy("EMPLOYEE")
                .coveredByMaintenancePlan(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .recipientUserId(null)  // Missing recipient
                .build();

        assertThrows(RuntimeException.class, () -> controller.createTaxProfile(vehicleId, request, principal));
    }

    // Test D: COMPANY profile may remain without recipient
    @Test
    void testD_CompanyProfileMayRemainWithoutRecipient() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(userRepository.findById(principal.getUserId())).thenReturn(Optional.of(recipientUser));
        when(permissionService.isAllowed(any(), any(), any(), any())).thenReturn(true);
        when(vehicleTaxProfileRepository.findByVehicleIdAndEffectiveToIsNull(vehicleId)).thenReturn(Optional.empty());
        when(vehicleTaxProfileRepository.save(any(VehicleTaxProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleTaxProfileCreateRequest request = VehicleTaxProfileCreateRequest.builder()
                .vehicleId(vehicleId)
                .vehicleCostCents(50000000L)
                .datePlacedInBusinessUse(LocalDate.of(2025, 1, 1))
                .taxpayerVatRegistered(true)
                .taxpayerType(TaxpayerType.COMPANY)
                .compensationType(CompensationType.REIMBURSEMENT)
                .fuelBorneBy("EMPLOYER")
                .maintenanceBorneBy("EMPLOYER")
                .coveredByMaintenancePlan(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .recipientUserId(null)  // COMPANY can have null recipient
                .build();

        ResponseEntity<VehicleTaxProfileDTO> response = controller.createTaxProfile(vehicleId, request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getRecipientUserId());
    }

    // Test E: cross-org recipient rejected
    @Test
    void testE_CrossOrgRecipientRejected() {
        Organization otherOrg = Organization.builder()
                .id(UUID.randomUUID())
                .name("Other Org")
                .build();

        User crossOrgRecipient = User.builder()
                .id(UUID.randomUUID())
                .organization(otherOrg)
                .firstName("Cross")
                .lastName("Org")
                .email("cross@other.com")
                .build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(userRepository.findById(principal.getUserId())).thenReturn(Optional.of(recipientUser));
        when(userRepository.findById(crossOrgRecipient.getId())).thenReturn(Optional.of(crossOrgRecipient));
        when(permissionService.isAllowed(any(), any(), any(), any())).thenReturn(true);
        when(vehicleTaxProfileRepository.findByVehicleIdAndEffectiveToIsNull(vehicleId)).thenReturn(Optional.empty());

        VehicleTaxProfileCreateRequest request = VehicleTaxProfileCreateRequest.builder()
                .vehicleId(vehicleId)
                .vehicleCostCents(50000000L)
                .datePlacedInBusinessUse(LocalDate.of(2025, 1, 1))
                .taxpayerVatRegistered(false)
                .taxpayerType(TaxpayerType.EMPLOYEE)
                .compensationType(CompensationType.TRAVEL_ALLOWANCE)
                .fuelBorneBy("EMPLOYEE")
                .maintenanceBorneBy("EMPLOYEE")
                .coveredByMaintenancePlan(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .recipientUserId(crossOrgRecipient.getId())
                .build();

        assertThrows(RuntimeException.class, () -> controller.createTaxProfile(vehicleId, request, principal));
    }

    // Test F: update omitted recipient preserves existing recipient
    @Test
    void testF_UpdateOmittedRecipientPreservesExistingRecipient() {
        VehicleTaxProfile existingProfile = VehicleTaxProfile.builder()
                .id(UUID.randomUUID())
                .vehicle(vehicle)
                .organization(organization)
                .recipientUser(recipientUser)
                .vehicleCostCents(50000000L)
                .datePlacedInBusinessUse(LocalDate.of(2025, 1, 1))
                .taxpayerType(TaxpayerType.EMPLOYEE)
                .compensationType(CompensationType.TRAVEL_ALLOWANCE)
                .fuelBorneBy("EMPLOYEE")
                .maintenanceBorneBy("EMPLOYEE")
                .coveredByMaintenancePlan(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicleTaxProfileRepository.findById(existingProfile.getId())).thenReturn(Optional.of(existingProfile));
        when(permissionService.isAllowed(any(), any(), any(), any())).thenReturn(true);
        when(vehicleTaxProfileRepository.save(any(VehicleTaxProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleTaxProfileUpdateRequest request = VehicleTaxProfileUpdateRequest.builder()
                .vehicleCostCents(60000000L)
                .recipientUserId(null)  // Omitted
                .build();

        ResponseEntity<VehicleTaxProfileDTO> response = controller.updateTaxProfile(
                vehicleId, existingProfile.getId(), request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(recipientUserId, response.getBody().getRecipientUserId());  // Preserved
        assertEquals(60000000L, response.getBody().getVehicleCostCents());  // Updated
    }

    // Test G: update explicit recipient changes to valid same-org recipient
    @Test
    void testG_UpdateExplicitRecipientChangesToValidSameOrgRecipient() {
        VehicleTaxProfile existingProfile = VehicleTaxProfile.builder()
                .id(UUID.randomUUID())
                .vehicle(vehicle)
                .organization(organization)
                .recipientUser(recipientUser)
                .vehicleCostCents(50000000L)
                .datePlacedInBusinessUse(LocalDate.of(2025, 1, 1))
                .taxpayerType(TaxpayerType.EMPLOYEE)
                .compensationType(CompensationType.TRAVEL_ALLOWANCE)
                .fuelBorneBy("EMPLOYEE")
                .maintenanceBorneBy("EMPLOYEE")
                .coveredByMaintenancePlan(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicleTaxProfileRepository.findById(existingProfile.getId())).thenReturn(Optional.of(existingProfile));
        when(userRepository.findById(otherRecipientUserId)).thenReturn(Optional.of(otherRecipientUser));
        when(permissionService.isAllowed(any(), any(), any(), any())).thenReturn(true);
        when(vehicleTaxProfileRepository.save(any(VehicleTaxProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleTaxProfileUpdateRequest request = VehicleTaxProfileUpdateRequest.builder()
                .recipientUserId(otherRecipientUserId)  // Explicit change
                .build();

        ResponseEntity<VehicleTaxProfileDTO> response = controller.updateTaxProfile(
                vehicleId, existingProfile.getId(), request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(otherRecipientUserId, response.getBody().getRecipientUserId());  // Changed
    }

    // Test H: changing COMPANY -> EMPLOYEE without recipient fails
    @Test
    void testH_ChangingCompanyToEmployeeWithoutRecipientFails() {
        VehicleTaxProfile existingProfile = VehicleTaxProfile.builder()
                .id(UUID.randomUUID())
                .vehicle(vehicle)
                .organization(organization)
                .recipientUser(null)  // COMPANY has no recipient
                .vehicleCostCents(50000000L)
                .datePlacedInBusinessUse(LocalDate.of(2025, 1, 1))
                .taxpayerType(TaxpayerType.COMPANY)
                .compensationType(CompensationType.REIMBURSEMENT)
                .fuelBorneBy("EMPLOYER")
                .maintenanceBorneBy("EMPLOYER")
                .coveredByMaintenancePlan(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicleTaxProfileRepository.findById(existingProfile.getId())).thenReturn(Optional.of(existingProfile));
        when(permissionService.isAllowed(any(), any(), any(), any())).thenReturn(true);

        VehicleTaxProfileUpdateRequest request = VehicleTaxProfileUpdateRequest.builder()
                .taxpayerType(TaxpayerType.EMPLOYEE)  // Changing to EMPLOYEE
                .recipientUserId(null)  // But no recipient provided
                .build();

        assertThrows(RuntimeException.class, () -> controller.updateTaxProfile(
                vehicleId, existingProfile.getId(), request, principal));
    }
}
