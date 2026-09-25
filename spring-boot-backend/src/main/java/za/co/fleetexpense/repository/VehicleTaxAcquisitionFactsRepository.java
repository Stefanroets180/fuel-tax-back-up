package za.co.fleetexpense.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.fleetexpense.entity.VehicleTaxAcquisitionFacts;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for VehicleTaxAcquisitionFacts.
 * One record per vehicle + recipient.
 */
@Repository
public interface VehicleTaxAcquisitionFactsRepository extends JpaRepository<VehicleTaxAcquisitionFacts, UUID> {

    /**
     * Find acquisition facts by vehicle and recipient with eager fetch of recipientUser.
     * Returns the unique record for this vehicle + recipient pair.
     */
    @Query("SELECT vtaf FROM VehicleTaxAcquisitionFacts vtaf LEFT JOIN FETCH vtaf.recipientUser WHERE vtaf.vehicle.id = :vehicleId AND vtaf.recipientUser.id = :recipientUserId")
    Optional<VehicleTaxAcquisitionFacts> findByVehicleIdAndRecipientUserId(UUID vehicleId, UUID recipientUserId);
}
