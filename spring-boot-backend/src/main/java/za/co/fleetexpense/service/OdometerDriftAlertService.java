package za.co.fleetexpense.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.fleetexpense.entity.OdometerDriftAlert;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.exception.ResourceNotFoundException;
import za.co.fleetexpense.repository.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OdometerDriftAlertService {

    private final OdometerDriftAlertRepository odometerDriftAlertRepository;
    private final VehicleRepository vehicleRepository;
    private final ExpenseRepository expenseRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @Value("${odometer.auto-recalculate:false}")
    private boolean autoRecalculate;

    public List<OdometerDriftAlert> getByOrganization(UUID organizationId) {
        return odometerDriftAlertRepository.findByOrganizationId(organizationId);
    }

    public List<OdometerDriftAlert> getActiveAlerts(UUID organizationId) {
        return odometerDriftAlertRepository.findByOrganizationIdAndIsDismissedFalse(organizationId);
    }

    public int checkAllVehiclesInOrganization(UUID organizationId) {
        List<za.co.fleetexpense.entity.Vehicle> vehicles = vehicleRepository.findByOrganizationIdAndIsActiveTrue(organizationId);
        int alertsCreated = 0;

        for (za.co.fleetexpense.entity.Vehicle vehicle : vehicles) {
            try {
                OdometerDriftAlert alert = checkAndCreateAlert(vehicle.getId());
                if (alert != null) {
                    alertsCreated++;
                }
            } catch (Exception e) {
                log.error("Error checking vehicle {} for odometer drift", vehicle.getId(), e);
            }
        }

        log.info("Checked {} vehicles for odometer drift, created {} alerts", vehicles.size(), alertsCreated);
        return alertsCreated;
    }

    public OdometerDriftAlert getByVehicle(UUID vehicleId) {
        return odometerDriftAlertRepository.findByVehicleId(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Odometer drift alert not found for vehicle: " + vehicleId));
    }

    /**
     * Phase 1: Detection-only drift check.
     * Calculates EXPECTED operational value using canonical active qualifying semantics.
     * Compares expected vs stored current and creates/updates/dismisses drift alerts.
     * NEVER writes Vehicle.currentOdometer - that is the responsibility of VehicleService.recalculateOperationalOdometer().
     */
    @Transactional
    public OdometerDriftAlert checkAndCreateAlert(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));

        Integer storedOdometer = vehicle.getCurrentOdometerStored();
        Integer baseline = vehicle.getOperationalBaselineOdometer();
        Integer qualifyingMax = expenseRepository.findMaxOdometerByVehicleId(vehicleId).orElse(null);

        // Canonical expected operational current formula
        Integer expectedOdometer;
        if (baseline != null && qualifyingMax != null) {
            expectedOdometer = Math.max(baseline, qualifyingMax);
        } else if (baseline != null) {
            expectedOdometer = baseline;
        } else if (qualifyingMax != null) {
            expectedOdometer = qualifyingMax;
        } else {
            // Phase 1: BASELINE_UNKNOWN state - no trustworthy derived expected value
            // Stored currentOdometer is preserved for continuity but its provenance is UNKNOWN
            // Normal drift comparison is not meaningful - do not generate false "synced" conclusion
            // Dismiss any existing alert and return null
            odometerDriftAlertRepository.findByVehicleIdAndIsDismissedFalse(vehicleId)
                    .ifPresent(alert -> {
                        alert.setIsDismissed(true);
                        alert.setDismissedAt(OffsetDateTime.now());
                        odometerDriftAlertRepository.save(alert);
                        log.info("Dismissed odometer drift alert for vehicle {} - BASELINE_UNKNOWN state, no trustworthy expected value", vehicleId);
                    });
            return null;
        }

        // Get individual expense type MAX values for enhanced drift detection
        Integer fuelMax = expenseRepository.findMaxFuelOdometerByVehicleId(vehicleId).orElse(0);
        Integer mechanicMax = expenseRepository.findMaxMechanicOdometerByVehicleId(vehicleId).orElse(0);
        Integer tyreMax = expenseRepository.findMaxTyreOdometerByVehicleId(vehicleId).orElse(0);

        // Drift Type 1: Stored baseline is wrong (stored != expected)
        int baselineThreshold = 100;
        boolean baselineDrift = storedOdometer != null && expectedOdometer != null &&
                Math.abs(storedOdometer - expectedOdometer) >= baselineThreshold;

        // Drift Type 2: Fuel chain is behind other expenses (fuel MAX < expected by >1,000 km)
        int fuelChainThreshold = 1000;
        boolean fuelChainDrift = fuelMax > 0 && expectedOdometer > fuelMax + fuelChainThreshold;

        // Drift Type 3: Mechanic service logged ahead of fuel (suspicious)
        int mechanicAheadThreshold = 2000;
        boolean mechanicAheadDrift = mechanicMax > fuelMax + mechanicAheadThreshold;

        // Drift Type 4: Below-baseline anomaly (qualifying evidence < known baseline)
        // This is a data quality anomaly, not a normal drift condition
        boolean belowBaselineAnomaly = baseline != null &&
                qualifyingMax != null &&
                qualifyingMax < baseline;

        // Create alert if any drift condition is met
        if (baselineDrift || fuelChainDrift || mechanicAheadDrift || belowBaselineAnomaly) {
            // Phase 1: Detection only - do NOT auto-recalculate
            // Manual recalculation must call VehicleService.recalculateOperationalOdometer() directly

            // Manual fallback: create alert with detailed drift info
            return odometerDriftAlertRepository.findByVehicleIdAndIsDismissedFalse(vehicleId)
                    .map(existingAlert -> {
                        // Update existing alert if values changed
                        if (!existingAlert.getStoredOdometer().equals(storedOdometer) ||
                            !existingAlert.getComputedOdometer().equals(expectedOdometer)) {
                            existingAlert.setStoredOdometer(storedOdometer);
                            existingAlert.setComputedOdometer(expectedOdometer);
                            log.info("Updated odometer drift alert for vehicle {} - stored: {}, expected: {}, fuelMax: {}, mechanicMax: {}, tyreMax: {}",
                                    vehicleId, storedOdometer, expectedOdometer, fuelMax, mechanicMax, tyreMax);
                            return odometerDriftAlertRepository.save(existingAlert);
                        }
                        return existingAlert;
                    })
                    .orElseGet(() -> {
                        // Create new alert
                        OdometerDriftAlert alert = OdometerDriftAlert.builder()
                                .organization(vehicle.getOrganization())
                                .vehicle(vehicle)
                                .vehicleRegistration(vehicle.getRegistrationNumber())
                                .storedOdometer(storedOdometer)
                                .computedOdometer(expectedOdometer)
                                .isDismissed(false)
                                .build();
                        OdometerDriftAlert saved = odometerDriftAlertRepository.save(alert);

                        String driftReason = belowBaselineAnomaly ? "below baseline anomaly" :
                                          baselineDrift ? "baseline drift" :
                                          fuelChainDrift ? "fuel chain lag" : "mechanic ahead of fuel";
                        log.info("Created odometer drift alert for vehicle {} - reason: {}, stored: {}, expected: {}, fuelMax: {}, mechanicMax: {}, tyreMax: {}",
                                vehicleId, driftReason, storedOdometer, expectedOdometer, fuelMax, mechanicMax, tyreMax);
                        return saved;
                    });
        } else {
            // Difference below threshold - dismiss any existing alert
            odometerDriftAlertRepository.findByVehicleIdAndIsDismissedFalse(vehicleId)
                    .ifPresent(alert -> {
                        alert.setIsDismissed(true);
                        alert.setDismissedAt(OffsetDateTime.now());
                        odometerDriftAlertRepository.save(alert);
                        log.info("Auto-dismissed odometer drift alert for vehicle {} - difference below threshold", vehicleId);
                    });
            return null;
        }
    }

    @Transactional
    public OdometerDriftAlert dismissAlert(UUID vehicleId, UUID userId) {
        OdometerDriftAlert alert = getByVehicle(vehicleId);
        alert.setIsDismissed(true);
        alert.setDismissedAt(OffsetDateTime.now());
        alert.setDismissedByUser(userRepository.findById(userId).orElse(null));
        OdometerDriftAlert saved = odometerDriftAlertRepository.save(alert);
        log.info("Dismissed odometer drift alert for vehicle {}", vehicleId);
        return saved;
    }

    @Transactional
    public void deleteAlert(UUID vehicleId) {
        odometerDriftAlertRepository.deleteByVehicleId(vehicleId);
        log.info("Deleted odometer drift alert for vehicle {}", vehicleId);
    }

    public String getAlertMessage(OdometerDriftAlert alert) {
        return String.format("Vehicle %s odometer needs recalculating - stored: %d km, computed: %d km",
                alert.getVehicleRegistration(), alert.getStoredOdometer(), alert.getComputedOdometer());
    }
}
