package za.co.fleetexpense.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.co.fleetexpense.dto.VehicleTaxAcquisitionFactsDTO;
import za.co.fleetexpense.dto.VehicleTaxAcquisitionFactsRequest;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.VehicleTaxAcquisitionFacts;
import za.co.fleetexpense.repository.UserRepository;
import za.co.fleetexpense.repository.VehicleRepository;
import za.co.fleetexpense.security.UserPrincipal;
import za.co.fleetexpense.service.VehicleTaxAcquisitionFactsService;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/acquisition-facts")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class VehicleTaxAcquisitionFactsController {

    private final VehicleTaxAcquisitionFactsService acquisitionFactsService;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    /**
     * Get acquisition facts for a specific vehicle and recipient.
     */
    @GetMapping
    public ResponseEntity<VehicleTaxAcquisitionFactsDTO> getAcquisitionFacts(
            @PathVariable UUID vehicleId,
            @RequestParam UUID recipientUserId,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Verify vehicle belongs to user's organization
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (!vehicle.getOrganization().getId().equals(principal.getOrganizationId())) {
            return ResponseEntity.status(403).build();
        }

        // Verify recipient belongs to same organization
        User recipient = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new RuntimeException("Recipient user not found"));

        if (!recipient.getOrganization().getId().equals(principal.getOrganizationId())) {
            return ResponseEntity.status(403).build();
        }

        Optional<VehicleTaxAcquisitionFacts> facts = acquisitionFactsService
                .findByVehicleAndRecipient(vehicleId, recipientUserId);

        return facts.map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create or update acquisition facts for a vehicle and recipient (upsert).
     * Uses same permission as tax profile management.
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseEntity<VehicleTaxAcquisitionFactsDTO> upsertAcquisitionFacts(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody VehicleTaxAcquisitionFactsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Verify vehicle belongs to user's organization
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (!requireSameOrganization(vehicle, principal)) {
            return ResponseEntity.status(403).build();
        }

        // Build facts entity from request
        VehicleTaxAcquisitionFacts facts = VehicleTaxAcquisitionFacts.builder()
                .recipientAcquisitionDate(request.getRecipientAcquisitionDate())
                .recipientAcquisitionCostCents(request.getRecipientAcquisitionCostCents())
                .originalPurchaseDebtCents(request.getOriginalPurchaseDebtCents())
                .vehicleArrangementType(request.getVehicleArrangementType())
                .build();

        VehicleTaxAcquisitionFacts saved = acquisitionFactsService.upsertAcquisitionFacts(
                vehicleId,
                request.getRecipientUserId(),
                principal.getOrganizationId(),
                facts
        );

        return ResponseEntity.ok(toDTO(saved));
    }

    private VehicleTaxAcquisitionFactsDTO toDTO(VehicleTaxAcquisitionFacts facts) {
        return VehicleTaxAcquisitionFactsDTO.builder()
                .id(facts.getId())
                .vehicleId(facts.getVehicle().getId())
                .recipientUserId(facts.getRecipientUser().getId())
                .recipientUserName(facts.getRecipientUser().getFirstName() + " " + facts.getRecipientUser().getLastName())
                .recipientUserEmail(facts.getRecipientUser().getEmail())
                .recipientAcquisitionDate(facts.getRecipientAcquisitionDate())
                .recipientAcquisitionCostCents(facts.getRecipientAcquisitionCostCents())
                .originalPurchaseDebtCents(facts.getOriginalPurchaseDebtCents())
                .vehicleArrangementType(facts.getVehicleArrangementType())
                .createdAt(facts.getCreatedAt())
                .updatedAt(facts.getUpdatedAt())
                .build();
    }

    /**
     * Shared authorization helper for acquisition facts operations.
     * Enforces same-organization rule.
     * Package-private for controller-internal use and unit testing in same package.
     */
    boolean requireSameOrganization(Vehicle vehicle, UserPrincipal principal) {
        return vehicle.getOrganization().getId().equals(principal.getOrganizationId());
    }
}
