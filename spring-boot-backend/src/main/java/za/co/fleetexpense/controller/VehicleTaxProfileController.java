package za.co.fleetexpense.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import za.co.fleetexpense.dto.TaxCalculationRequest;
import za.co.fleetexpense.dto.TaxCalculationResult;
import za.co.fleetexpense.dto.VehicleTaxProfileCreateRequest;
import za.co.fleetexpense.dto.VehicleTaxProfileDTO;
import za.co.fleetexpense.dto.VehicleTaxProfileUpdateRequest;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.VehicleTaxProfile;
import za.co.fleetexpense.repository.OrganizationRepository;
import za.co.fleetexpense.repository.UserRepository;
import za.co.fleetexpense.repository.VehicleRepository;
import za.co.fleetexpense.repository.VehicleTaxProfileRepository;
import za.co.fleetexpense.security.UserPrincipal;
import za.co.fleetexpense.service.PermissionService;
import za.co.fleetexpense.service.TaxCalculationService;
import za.co.fleetexpense.service.TaxYearSummaryService;
import za.co.fleetexpense.util.TaxYearHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/tax-profiles")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class VehicleTaxProfileController {

    private final VehicleTaxProfileRepository vehicleTaxProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TaxCalculationService taxCalculationService;
    private final TaxYearSummaryService taxYearSummaryService;
    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<VehicleTaxProfileDTO>> getTaxProfiles(
            @PathVariable UUID vehicleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify vehicle belongs to user's organization
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        
        if (!vehicle.getOrganization().getId().equals(principal.getOrganizationId())) {
            return ResponseEntity.status(403).build();
        }

        List<VehicleTaxProfile> profiles = vehicleTaxProfileRepository.findByVehicleId(vehicleId);
        return ResponseEntity.ok(profiles.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/active")
    public ResponseEntity<VehicleTaxProfileDTO> getActiveTaxProfile(
            @PathVariable UUID vehicleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify vehicle belongs to user's organization
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        
        if (!vehicle.getOrganization().getId().equals(principal.getOrganizationId())) {
            return ResponseEntity.status(403).build();
        }

        return vehicleTaxProfileRepository.findByVehicleIdAndEffectiveToIsNull(vehicleId)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseEntity<VehicleTaxProfileDTO> createTaxProfile(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody VehicleTaxProfileCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify vehicle belongs to user's organization
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        
        if (!requireTaxProfileManagementPermission(vehicle, principal)) {
            return ResponseEntity.status(403).build();
        }

        // Overlap protection: close any existing active profile for this vehicle
        vehicleTaxProfileRepository.findByVehicleIdAndEffectiveToIsNull(vehicleId)
                .ifPresent(existingProfile -> {
                    existingProfile.setEffectiveTo(request.getEffectiveFrom());
                    vehicleTaxProfileRepository.save(existingProfile);
                });

        Organization organization = organizationRepository.findById(principal.getOrganizationId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        User createdBy = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate recipient user for EMPLOYEE and SOLE_PROPRIETOR
        User recipientUser = null;
        if (request.getTaxpayerType() != null && 
            (request.getTaxpayerType() == za.co.fleetexpense.entity.enums.TaxpayerType.EMPLOYEE ||
             request.getTaxpayerType() == za.co.fleetexpense.entity.enums.TaxpayerType.SOLE_PROPRIETOR)) {
            if (request.getRecipientUserId() == null) {
                throw new RuntimeException("Recipient user is required for EMPLOYEE and SOLE_PROPRIETOR taxpayer types");
            }
            recipientUser = userRepository.findById(request.getRecipientUserId())
                    .orElseThrow(() -> new RuntimeException("Recipient user not found"));
            if (!recipientUser.getOrganization().getId().equals(principal.getOrganizationId())) {
                throw new RuntimeException("Recipient does not belong to the same organization");
            }
        }

        VehicleTaxProfile profile = VehicleTaxProfile.builder()
                .organization(organization)
                .vehicle(vehicle)
                .recipientUser(recipientUser)
                .vehicleCostCents(request.getVehicleCostCents())
                .datePlacedInBusinessUse(request.getDatePlacedInBusinessUse())
                .taxpayerVatRegistered(request.getTaxpayerVatRegistered())
                .taxpayerType(request.getTaxpayerType())
                .compensationType(request.getCompensationType())
                .fuelBorneBy(request.getFuelBorneBy())
                .maintenanceBorneBy(request.getMaintenanceBorneBy())
                .coveredByMaintenancePlan(request.getCoveredByMaintenancePlan())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .defaultCalculationMethod(request.getDefaultCalculationMethod())
                .isCompanyProvidedVehicle(request.getIsCompanyProvidedVehicle() != null ? request.getIsCompanyProvidedVehicle() : false)
                .createdBy(createdBy)
                .build();

        VehicleTaxProfile saved = vehicleTaxProfileRepository.save(profile);
        return ResponseEntity.ok(toDTO(saved));
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseEntity<VehicleTaxProfileDTO> updateTaxProfile(
            @PathVariable UUID vehicleId,
            @PathVariable UUID id,
            @Valid @RequestBody VehicleTaxProfileUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify vehicle belongs to user's organization
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        
        if (!requireTaxProfileManagementPermission(vehicle, principal)) {
            return ResponseEntity.status(403).build();
        }

        return vehicleTaxProfileRepository.findById(id)
                .filter(profile -> profile.getVehicle().getId().equals(vehicleId))
                .map(profile -> {
                    if (request.getVehicleCostCents() != null) {
                        profile.setVehicleCostCents(request.getVehicleCostCents());
                    }
                    if (request.getDatePlacedInBusinessUse() != null) {
                        profile.setDatePlacedInBusinessUse(request.getDatePlacedInBusinessUse());
                    }
                    if (request.getTaxpayerVatRegistered() != null) {
                        profile.setTaxpayerVatRegistered(request.getTaxpayerVatRegistered());
                    }
                    if (request.getTaxpayerType() != null) {
                        profile.setTaxpayerType(request.getTaxpayerType());
                    }
                    if (request.getCompensationType() != null) {
                        profile.setCompensationType(request.getCompensationType());
                    }
                    if (request.getFuelBorneBy() != null) {
                        profile.setFuelBorneBy(request.getFuelBorneBy());
                    }
                    if (request.getMaintenanceBorneBy() != null) {
                        profile.setMaintenanceBorneBy(request.getMaintenanceBorneBy());
                    }
                    if (request.getCoveredByMaintenancePlan() != null) {
                        profile.setCoveredByMaintenancePlan(request.getCoveredByMaintenancePlan());
                    }
                    if (request.getEffectiveFrom() != null) {
                        profile.setEffectiveFrom(request.getEffectiveFrom());
                    }
                    if (request.getEffectiveTo() != null) {
                        profile.setEffectiveTo(request.getEffectiveTo());
                    }
                    if (request.getDefaultCalculationMethod() != null) {
                        profile.setDefaultCalculationMethod(request.getDefaultCalculationMethod());
                    }
                    if (request.getIsCompanyProvidedVehicle() != null) {
                        profile.setIsCompanyProvidedVehicle(request.getIsCompanyProvidedVehicle());
                    }
                    if (request.getRecipientUserId() != null) {
                        User recipientUser = userRepository.findById(request.getRecipientUserId())
                                .orElseThrow(() -> new RuntimeException("Recipient user not found"));
                        if (!recipientUser.getOrganization().getId().equals(principal.getOrganizationId())) {
                            throw new RuntimeException("Recipient does not belong to the same organization");
                        }
                        profile.setRecipientUser(recipientUser);
                    }
                    // Validate recipient when changing to EMPLOYEE or SOLE_PROPRIETOR
                    if (request.getTaxpayerType() != null &&
                        (request.getTaxpayerType() == za.co.fleetexpense.entity.enums.TaxpayerType.EMPLOYEE ||
                         request.getTaxpayerType() == za.co.fleetexpense.entity.enums.TaxpayerType.SOLE_PROPRIETOR)) {
                        if (profile.getRecipientUser() == null && request.getRecipientUserId() == null) {
                            throw new RuntimeException("Recipient user is required when changing to EMPLOYEE or SOLE_PROPRIETOR");
                        }
                    }
                    return ResponseEntity.ok(toDTO(vehicleTaxProfileRepository.save(profile)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteTaxProfile(
            @PathVariable UUID vehicleId,
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify vehicle belongs to user's organization
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        
        if (!requireTaxProfileManagementPermission(vehicle, principal)) {
            return ResponseEntity.status(403).build();
        }

        if (vehicleTaxProfileRepository.existsById(id)) {
            vehicleTaxProfileRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/tax-calculations")
    public ResponseEntity<Map<String, Object>> calculateTaxComparison(
            @PathVariable UUID vehicleId,
            @RequestParam Integer taxYear,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify vehicle belongs to user's organization
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        
        if (!vehicle.getOrganization().getId().equals(principal.getOrganizationId())) {
            return ResponseEntity.status(403).build();
        }

        // Resolve tax profile server-side for the given tax year
        VehicleTaxProfile profile = taxCalculationService.findProfileForTaxYear(vehicleId, taxYear);
        
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }

        // Get km and classified expense totals server-side from TaxYearSummaryService
        za.co.fleetexpense.dto.TaxYearSummaryDTO taxYearSummary = taxYearSummaryService.getByVehicleAndYear(vehicleId, taxYear);
        
        if (taxYearSummary == null) {
            return ResponseEntity.notFound().build();
        }

        // Extract data from TaxYearSummaryService (includes data-quality warnings)
        BigDecimal businessKm = BigDecimal.valueOf(taxYearSummary.getBusinessKm() != null ? taxYearSummary.getBusinessKm() : 0L);
        BigDecimal totalKm = BigDecimal.valueOf(taxYearSummary.getTotalKm() != null ? taxYearSummary.getTotalKm() : 0L);
        BigDecimal qualifyingCurrentExpenseCents = BigDecimal.valueOf(taxYearSummary.getQualifyingCurrentExpenseCents() != null ? taxYearSummary.getQualifyingCurrentExpenseCents() : 0L);
        List<String> dataQualityWarnings = taxYearSummary.getDataQualityWarnings();

        // Calculate tax year boundaries using canonical helper
        LocalDate taxYearStart = TaxYearHelper.getAssessmentYearStart(taxYear);
        LocalDate taxYearEndExcl = TaxYearHelper.getAssessmentYearEndExclusive(taxYear);
        LocalDate taxYearEnd = taxYearEndExcl.minusDays(1);  // Feb 28 or 29, leap-aware

        // Calculate using all three methods, applying eligibility gating
        TaxCalculationResult actualCostsResult;
        if (taxCalculationService.isEligibleForMethod(
                za.co.fleetexpense.entity.enums.TaxCalculationMethod.ACTUAL_COSTS,
                profile,
                businessKm,
                totalKm)) {
            actualCostsResult = taxCalculationService.calculateActualCosts(
                    profile,
                    qualifyingCurrentExpenseCents,
                    businessKm,
                    totalKm,
                    taxYearStart,
                    taxYearEnd
            );
        } else {
            actualCostsResult = TaxCalculationResult.builder()
                    .method("ACTUAL_COSTS")
                    .eligible(false)
                    .ineligibilityReason(taxCalculationService.getIneligibilityReason(
                            za.co.fleetexpense.entity.enums.TaxCalculationMethod.ACTUAL_COSTS,
                            profile))
                    .build();
        }

        TaxCalculationResult sarsCostScaleResult;
        if (taxCalculationService.isEligibleForMethod(
                za.co.fleetexpense.entity.enums.TaxCalculationMethod.SARS_COST_SCALE,
                profile,
                businessKm,
                totalKm)) {
            sarsCostScaleResult = taxCalculationService.calculateSarsCostScale(
                    profile,
                    taxYearStart,
                    taxYearEnd,
                    businessKm,
                    totalKm
            );
        } else {
            sarsCostScaleResult = TaxCalculationResult.builder()
                    .method("SARS_COST_SCALE")
                    .eligible(false)
                    .ineligibilityReason(taxCalculationService.getIneligibilityReason(
                            za.co.fleetexpense.entity.enums.TaxCalculationMethod.SARS_COST_SCALE,
                            profile))
                    .build();
        }

        TaxCalculationResult simplifiedReimbursiveResult;
        if (taxCalculationService.isEligibleForMethod(
                za.co.fleetexpense.entity.enums.TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE,
                profile,
                businessKm,
                totalKm)) {
            simplifiedReimbursiveResult = taxCalculationService.calculateSimplifiedReimbursive(
                    profile,
                    taxYearStart,
                    taxYearEnd,
                    businessKm
            );
        } else {
            simplifiedReimbursiveResult = TaxCalculationResult.builder()
                    .method("SIMPLIFIED_REIMBURSIVE")
                    .eligible(false)
                    .ineligibilityReason(taxCalculationService.getIneligibilityReason(
                            za.co.fleetexpense.entity.enums.TaxCalculationMethod.SIMPLIFIED_REIMBURSIVE,
                            profile))
                    .build();
        }

        Map<String, TaxCalculationResult> results = new HashMap<>();
        results.put("actualCosts", actualCostsResult);
        results.put("sarsCostScale", sarsCostScaleResult);
        results.put("simplifiedReimbursement", simplifiedReimbursiveResult);

        // Include data quality warnings in response
        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        response.put("dataQualityWarnings", dataQualityWarnings);

        return ResponseEntity.ok(response);
    }

    private VehicleTaxProfileDTO toDTO(VehicleTaxProfile profile) {
        return VehicleTaxProfileDTO.builder()
                .id(profile.getId())
                .organizationId(profile.getOrganization().getId())
                .vehicleId(profile.getVehicle().getId())
                .vehicleRegistration(profile.getVehicle().getRegistrationNumber())
                .vehicleMake(profile.getVehicle().getMake())
                .vehicleModel(profile.getVehicle().getModel())
                .vehicleCostCents(profile.getVehicleCostCents())
                .datePlacedInBusinessUse(profile.getDatePlacedInBusinessUse())
                .taxpayerVatRegistered(profile.getTaxpayerVatRegistered())
                .taxpayerType(profile.getTaxpayerType())
                .compensationType(profile.getCompensationType())
                .fuelBorneBy(profile.getFuelBorneBy())
                .maintenanceBorneBy(profile.getMaintenanceBorneBy())
                .coveredByMaintenancePlan(profile.getCoveredByMaintenancePlan())
                .effectiveFrom(profile.getEffectiveFrom())
                .effectiveTo(profile.getEffectiveTo())
                .defaultCalculationMethod(profile.getDefaultCalculationMethod())
                .isCompanyProvidedVehicle(profile.getIsCompanyProvidedVehicle())
                .createdBy(profile.getCreatedBy() != null ? profile.getCreatedBy().getId() : null)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .recipientUserId(profile.getRecipientUser() != null ? profile.getRecipientUser().getId() : null)
                .recipientUserName(profile.getRecipientUser() != null ? 
                    profile.getRecipientUser().getFirstName() + " " + profile.getRecipientUser().getLastName() : null)
                .recipientUserEmail(profile.getRecipientUser() != null ? profile.getRecipientUser().getEmail() : null)
                .build();
    }

    /**
     * Shared authorization helper for tax profile management operations.
     * Enforces same-organization rule and PermissionService TAX_PROFILE.MANAGE_PROFILES check.
     * Does NOT check assigned-driver status (removed in Phase 3A).
     * Package-private for controller-internal use and unit testing in same package.
     */
    boolean requireTaxProfileManagementPermission(Vehicle vehicle, UserPrincipal principal) {
        // Same-organization check
        if (!vehicle.getOrganization().getId().equals(principal.getOrganizationId())) {
            return false;
        }

        // PermissionService TAX_PROFILE.MANAGE_PROFILES check
        return permissionService.isAllowed(
                principal.getOrganizationId(),
                "TAX_PROFILE",
                "MANAGE_PROFILES",
                principal.getRole().name()
        );
    }

}
