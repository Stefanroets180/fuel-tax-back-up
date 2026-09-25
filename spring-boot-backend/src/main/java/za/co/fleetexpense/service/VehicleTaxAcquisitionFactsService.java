package za.co.fleetexpense.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.VehicleTaxAcquisitionFacts;
import za.co.fleetexpense.entity.enums.VehicleArrangementType;
import za.co.fleetexpense.exception.ValidationException;
import za.co.fleetexpense.repository.UserRepository;
import za.co.fleetexpense.repository.VehicleRepository;
import za.co.fleetexpense.repository.VehicleTaxAcquisitionFactsRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleTaxAcquisitionFactsService {

    private final VehicleTaxAcquisitionFactsRepository acquisitionFactsRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    /**
     * Find acquisition facts by vehicle and recipient.
     */
    public Optional<VehicleTaxAcquisitionFacts> findByVehicleAndRecipient(UUID vehicleId, UUID recipientUserId) {
        return acquisitionFactsRepository.findByVehicleIdAndRecipientUserId(vehicleId, recipientUserId);
    }

    /**
     * Create or update acquisition facts for a vehicle + recipient pair (upsert semantics).
     * Validates same-organization rule and field constraints.
     */
    @Transactional
    public VehicleTaxAcquisitionFacts upsertAcquisitionFacts(
            UUID vehicleId,
            UUID recipientUserId,
            UUID organizationId,
            VehicleTaxAcquisitionFacts facts) {

        // Validate vehicle exists and belongs to organization
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ValidationException("Vehicle not found"));

        if (!vehicle.getOrganization().getId().equals(organizationId)) {
            throw new ValidationException("Vehicle does not belong to this organization");
        }

        // Validate recipient exists and belongs to same organization
        User recipient = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new ValidationException("Recipient user not found"));

        if (!recipient.getOrganization().getId().equals(organizationId)) {
            throw new ValidationException("Recipient does not belong to the same organization as the vehicle");
        }

        // Validate arrangement type
        if (facts.getVehicleArrangementType() == null) {
            throw new ValidationException("Vehicle arrangement type is required");
        }

        // Validate OWNED-specific fields
        if (facts.getVehicleArrangementType() == VehicleArrangementType.OWNED) {
            if (facts.getRecipientAcquisitionDate() == null) {
                throw new ValidationException("Acquisition date is required for owned vehicles");
            }
            if (facts.getRecipientAcquisitionCostCents() == null) {
                throw new ValidationException("Acquisition cost is required for owned vehicles");
            }
            if (facts.getRecipientAcquisitionCostCents() < 0) {
                throw new ValidationException("Acquisition cost cannot be negative");
            }
        }

        // Validate original purchase debt (optional for all arrangements)
        if (facts.getOriginalPurchaseDebtCents() != null && facts.getOriginalPurchaseDebtCents() < 0) {
            throw new ValidationException("Original purchase debt cannot be negative");
        }

        // Check if record already exists (upsert)
        Optional<VehicleTaxAcquisitionFacts> existing = acquisitionFactsRepository
                .findByVehicleIdAndRecipientUserId(vehicleId, recipientUserId);

        VehicleTaxAcquisitionFacts toSave;
        if (existing.isPresent()) {
            // Update existing record
            toSave = existing.get();
            toSave.setRecipientAcquisitionDate(facts.getRecipientAcquisitionDate());
            toSave.setRecipientAcquisitionCostCents(facts.getRecipientAcquisitionCostCents());
            toSave.setOriginalPurchaseDebtCents(facts.getOriginalPurchaseDebtCents());
            toSave.setVehicleArrangementType(facts.getVehicleArrangementType());
            log.info("Updated acquisition facts for vehicle {} and recipient {}", vehicleId, recipientUserId);
        } else {
            // Create new record
            toSave = VehicleTaxAcquisitionFacts.builder()
                    .vehicle(vehicle)
                    .recipientUser(recipient)
                    .recipientAcquisitionDate(facts.getRecipientAcquisitionDate())
                    .recipientAcquisitionCostCents(facts.getRecipientAcquisitionCostCents())
                    .originalPurchaseDebtCents(facts.getOriginalPurchaseDebtCents())
                    .vehicleArrangementType(facts.getVehicleArrangementType())
                    .build();
            log.info("Created acquisition facts for vehicle {} and recipient {}", vehicleId, recipientUserId);
        }

        return acquisitionFactsRepository.save(toSave);
    }
}
