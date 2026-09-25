package za.co.fleetexpense.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.VehicleTaxAcquisitionFacts;
import za.co.fleetexpense.entity.enums.VehicleArrangementType;
import za.co.fleetexpense.exception.ValidationException;
import za.co.fleetexpense.repository.UserRepository;
import za.co.fleetexpense.repository.VehicleRepository;
import za.co.fleetexpense.repository.VehicleTaxAcquisitionFactsRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleTaxAcquisitionFactsServicePhase4G3ATest {

    @Mock
    private VehicleTaxAcquisitionFactsRepository acquisitionFactsRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VehicleTaxAcquisitionFactsService service;

    private UUID organizationId;
    private UUID vehicleId;
    private UUID recipientUserId;
    private UUID crossOrgRecipientUserId;
    private Vehicle vehicle;
    private User recipientUser;
    private User crossOrgRecipient;
    private Organization organization;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        recipientUserId = UUID.randomUUID();
        crossOrgRecipientUserId = UUID.randomUUID();

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

        Organization crossOrg = Organization.builder()
                .id(UUID.randomUUID())
                .name("Cross Org")
                .build();

        crossOrgRecipient = User.builder()
                .id(crossOrgRecipientUserId)
                .organization(crossOrg)
                .firstName("Cross")
                .lastName("Org")
                .email("cross@other.com")
                .build();

        vehicle = Vehicle.builder()
                .id(vehicleId)
                .organization(organization)
                .registrationNumber("ABC123")
                .build();
    }

    // Test I: acquisition facts OWNED requires acquisition date + acquisition cost
    @Test
    void testI_OwnedRequiresAcquisitionDateAndCost() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(recipientUserId)).thenReturn(Optional.of(recipientUser));

        VehicleTaxAcquisitionFacts facts = VehicleTaxAcquisitionFacts.builder()
                .vehicleArrangementType(VehicleArrangementType.OWNED)
                .recipientAcquisitionDate(null)  // Missing
                .recipientAcquisitionCostCents(null)  // Missing
                .build();

        assertThrows(ValidationException.class, () ->
                service.upsertAcquisitionFacts(vehicleId, recipientUserId, organizationId, facts));
    }

    // Test J: originalPurchaseDebtCents optional
    @Test
    void testJ_OriginalPurchaseDebtCentsOptional() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(recipientUserId)).thenReturn(Optional.of(recipientUser));
        when(acquisitionFactsRepository.findByVehicleIdAndRecipientUserId(vehicleId, recipientUserId))
                .thenReturn(Optional.empty());
        when(acquisitionFactsRepository.save(any(VehicleTaxAcquisitionFacts.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleTaxAcquisitionFacts facts = VehicleTaxAcquisitionFacts.builder()
                .vehicleArrangementType(VehicleArrangementType.OWNED)
                .recipientAcquisitionDate(LocalDate.of(2020, 1, 1))
                .recipientAcquisitionCostCents(50000000L)
                .originalPurchaseDebtCents(null)  // Optional, should succeed
                .build();

        VehicleTaxAcquisitionFacts result = service.upsertAcquisitionFacts(
                vehicleId, recipientUserId, organizationId, facts);

        assertNotNull(result);
        assertNull(result.getOriginalPurchaseDebtCents());
    }

    // Test K: negative acquisition cost rejected
    @Test
    void testK_NegativeAcquisitionCostRejected() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(recipientUserId)).thenReturn(Optional.of(recipientUser));

        VehicleTaxAcquisitionFacts facts = VehicleTaxAcquisitionFacts.builder()
                .vehicleArrangementType(VehicleArrangementType.OWNED)
                .recipientAcquisitionDate(LocalDate.of(2020, 1, 1))
                .recipientAcquisitionCostCents(-100L)  // Negative
                .build();

        assertThrows(ValidationException.class, () ->
                service.upsertAcquisitionFacts(vehicleId, recipientUserId, organizationId, facts));
    }

    // Test L: negative original debt rejected
    @Test
    void testL_NegativeOriginalDebtRejected() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(recipientUserId)).thenReturn(Optional.of(recipientUser));

        VehicleTaxAcquisitionFacts facts = VehicleTaxAcquisitionFacts.builder()
                .vehicleArrangementType(VehicleArrangementType.OWNED)
                .recipientAcquisitionDate(LocalDate.of(2020, 1, 1))
                .recipientAcquisitionCostCents(50000000L)
                .originalPurchaseDebtCents(-100L)  // Negative
                .build();

        assertThrows(ValidationException.class, () ->
                service.upsertAcquisitionFacts(vehicleId, recipientUserId, organizationId, facts));
    }

    // Test M: LEASED does not require owned W&T inputs
    @Test
    void testM_LeasedDoesNotRequireOwnedWAndTInputs() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(recipientUserId)).thenReturn(Optional.of(recipientUser));
        when(acquisitionFactsRepository.findByVehicleIdAndRecipientUserId(vehicleId, recipientUserId))
                .thenReturn(Optional.empty());
        when(acquisitionFactsRepository.save(any(VehicleTaxAcquisitionFacts.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleTaxAcquisitionFacts facts = VehicleTaxAcquisitionFacts.builder()
                .vehicleArrangementType(VehicleArrangementType.LEASED)
                .recipientAcquisitionDate(null)  // Not required for LEASED
                .recipientAcquisitionCostCents(null)  // Not required for LEASED
                .originalPurchaseDebtCents(null)
                .build();

        VehicleTaxAcquisitionFacts result = service.upsertAcquisitionFacts(
                vehicleId, recipientUserId, organizationId, facts);

        assertNotNull(result);
        assertEquals(VehicleArrangementType.LEASED, result.getVehicleArrangementType());
    }

    // Test N: repeated PUT/upsert for same vehicle+recipient updates rather than creates duplicate
    @Test
    void testN_RepeatedUpsertUpdatesRatherThanCreatesDuplicate() {
        VehicleTaxAcquisitionFacts existing = VehicleTaxAcquisitionFacts.builder()
                .id(UUID.randomUUID())
                .vehicle(vehicle)
                .recipientUser(recipientUser)
                .recipientAcquisitionDate(LocalDate.of(2020, 1, 1))
                .recipientAcquisitionCostCents(50000000L)
                .vehicleArrangementType(VehicleArrangementType.OWNED)
                .build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(recipientUserId)).thenReturn(Optional.of(recipientUser));
        when(acquisitionFactsRepository.findByVehicleIdAndRecipientUserId(vehicleId, recipientUserId))
                .thenReturn(Optional.of(existing));  // Existing record found
        when(acquisitionFactsRepository.save(any(VehicleTaxAcquisitionFacts.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleTaxAcquisitionFacts facts = VehicleTaxAcquisitionFacts.builder()
                .vehicleArrangementType(VehicleArrangementType.OWNED)
                .recipientAcquisitionDate(LocalDate.of(2020, 1, 1))
                .recipientAcquisitionCostCents(60000000L)  // Updated cost
                .originalPurchaseDebtCents(null)
                .build();

        VehicleTaxAcquisitionFacts result = service.upsertAcquisitionFacts(
                vehicleId, recipientUserId, organizationId, facts);

        assertNotNull(result);
        assertEquals(existing.getId(), result.getId());  // Same ID (updated, not created)
        assertEquals(60000000L, result.getRecipientAcquisitionCostCents());  // Updated value

        verify(acquisitionFactsRepository, times(1)).save(existing);  // Saved existing, not new
    }

    // Test O: cross-org acquisition-fact access rejected
    @Test
    void testO_CrossOrgAcquisitionFactAccessRejected() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(crossOrgRecipientUserId)).thenReturn(Optional.of(crossOrgRecipient));

        VehicleTaxAcquisitionFacts facts = VehicleTaxAcquisitionFacts.builder()
                .vehicleArrangementType(VehicleArrangementType.OWNED)
                .recipientAcquisitionDate(LocalDate.of(2020, 1, 1))
                .recipientAcquisitionCostCents(50000000L)
                .build();

        assertThrows(ValidationException.class, () ->
                service.upsertAcquisitionFacts(vehicleId, crossOrgRecipientUserId, organizationId, facts));
    }

    // Test P: tax-profile/acquisition operations have no operational-odometer effect
    @Test
    void testP_AcquisitionOperationsHaveNoOperationalOdometerEffect() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(recipientUserId)).thenReturn(Optional.of(recipientUser));
        when(acquisitionFactsRepository.findByVehicleIdAndRecipientUserId(vehicleId, recipientUserId))
                .thenReturn(Optional.empty());
        when(acquisitionFactsRepository.save(any(VehicleTaxAcquisitionFacts.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleTaxAcquisitionFacts facts = VehicleTaxAcquisitionFacts.builder()
                .vehicleArrangementType(VehicleArrangementType.OWNED)
                .recipientAcquisitionDate(LocalDate.of(2020, 1, 1))
                .recipientAcquisitionCostCents(50000000L)
                .build();

        VehicleTaxAcquisitionFacts result = service.upsertAcquisitionFacts(
                vehicleId, recipientUserId, organizationId, facts);

        assertNotNull(result);
        
        // Verify vehicle's operational odometer fields were not touched
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        verify(vehicleRepository, times(1)).findById(vehicleId);  // Only for lookup
    }
}
