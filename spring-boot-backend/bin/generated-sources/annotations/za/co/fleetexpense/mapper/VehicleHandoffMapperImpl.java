package za.co.fleetexpense.mapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.VehicleHandoffDTO;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.VehicleAssignment;
import za.co.fleetexpense.entity.VehicleHandoff;
import za.co.fleetexpense.enums.HandoffState;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-21T10:29:41+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class VehicleHandoffMapperImpl implements VehicleHandoffMapper {

    @Override
    public VehicleHandoffDTO toDto(VehicleHandoff handoff) {
        if ( handoff == null ) {
            return null;
        }

        UUID vehicleId = null;
        String vehicleRegistration = null;
        UUID oldAssignmentId = null;
        UUID oldDriverId = null;
        UUID newAssignmentId = null;
        UUID newDriverId = null;
        UUID initiatedById = null;
        UUID cancelledById = null;
        UUID id = null;
        HandoffState state = null;
        Instant managerApprovedAt = null;
        Instant completedAt = null;
        Instant cancelledAt = null;
        String cancellationReason = null;
        String overrideReason = null;
        Instant createdAt = null;

        vehicleId = handoffVehicleId( handoff );
        vehicleRegistration = handoffVehicleRegistrationNumber( handoff );
        oldAssignmentId = handoffOldAssignmentId( handoff );
        oldDriverId = handoffOldAssignmentAssignedDriverId( handoff );
        newAssignmentId = handoffNewAssignmentId( handoff );
        newDriverId = handoffNewAssignmentAssignedDriverId( handoff );
        initiatedById = handoffInitiatedById( handoff );
        cancelledById = handoffCancelledById( handoff );
        id = handoff.getId();
        state = handoff.getState();
        managerApprovedAt = handoff.getManagerApprovedAt();
        completedAt = handoff.getCompletedAt();
        cancelledAt = handoff.getCancelledAt();
        cancellationReason = handoff.getCancellationReason();
        overrideReason = handoff.getOverrideReason();
        createdAt = handoff.getCreatedAt();

        String oldDriverName = handoff.getOldAssignment() != null ? handoff.getOldAssignment().getAssignedDriver().getFirstName() + " " + handoff.getOldAssignment().getAssignedDriver().getLastName() : null;
        String newDriverName = handoff.getNewAssignment().getAssignedDriver().getFirstName() + " " + handoff.getNewAssignment().getAssignedDriver().getLastName();
        String initiatedByName = handoff.getInitiatedBy().getFirstName() + " " + handoff.getInitiatedBy().getLastName();
        String cancelledByName = handoff.getCancelledBy() != null ? handoff.getCancelledBy().getFirstName() + " " + handoff.getCancelledBy().getLastName() : null;

        VehicleHandoffDTO vehicleHandoffDTO = new VehicleHandoffDTO( id, vehicleId, vehicleRegistration, oldAssignmentId, oldDriverId, oldDriverName, newAssignmentId, newDriverId, newDriverName, initiatedById, initiatedByName, state, managerApprovedAt, completedAt, cancelledAt, cancelledById, cancelledByName, cancellationReason, overrideReason, createdAt );

        return vehicleHandoffDTO;
    }

    @Override
    public List<VehicleHandoffDTO> toDtoList(List<VehicleHandoff> handoffs) {
        if ( handoffs == null ) {
            return null;
        }

        List<VehicleHandoffDTO> list = new ArrayList<VehicleHandoffDTO>( handoffs.size() );
        for ( VehicleHandoff vehicleHandoff : handoffs ) {
            list.add( toDto( vehicleHandoff ) );
        }

        return list;
    }

    private UUID handoffVehicleId(VehicleHandoff vehicleHandoff) {
        Vehicle vehicle = vehicleHandoff.getVehicle();
        if ( vehicle == null ) {
            return null;
        }
        return vehicle.getId();
    }

    private String handoffVehicleRegistrationNumber(VehicleHandoff vehicleHandoff) {
        Vehicle vehicle = vehicleHandoff.getVehicle();
        if ( vehicle == null ) {
            return null;
        }
        return vehicle.getRegistrationNumber();
    }

    private UUID handoffOldAssignmentId(VehicleHandoff vehicleHandoff) {
        VehicleAssignment oldAssignment = vehicleHandoff.getOldAssignment();
        if ( oldAssignment == null ) {
            return null;
        }
        return oldAssignment.getId();
    }

    private UUID handoffOldAssignmentAssignedDriverId(VehicleHandoff vehicleHandoff) {
        VehicleAssignment oldAssignment = vehicleHandoff.getOldAssignment();
        if ( oldAssignment == null ) {
            return null;
        }
        User assignedDriver = oldAssignment.getAssignedDriver();
        if ( assignedDriver == null ) {
            return null;
        }
        return assignedDriver.getId();
    }

    private UUID handoffNewAssignmentId(VehicleHandoff vehicleHandoff) {
        VehicleAssignment newAssignment = vehicleHandoff.getNewAssignment();
        if ( newAssignment == null ) {
            return null;
        }
        return newAssignment.getId();
    }

    private UUID handoffNewAssignmentAssignedDriverId(VehicleHandoff vehicleHandoff) {
        VehicleAssignment newAssignment = vehicleHandoff.getNewAssignment();
        if ( newAssignment == null ) {
            return null;
        }
        User assignedDriver = newAssignment.getAssignedDriver();
        if ( assignedDriver == null ) {
            return null;
        }
        return assignedDriver.getId();
    }

    private UUID handoffInitiatedById(VehicleHandoff vehicleHandoff) {
        User initiatedBy = vehicleHandoff.getInitiatedBy();
        if ( initiatedBy == null ) {
            return null;
        }
        return initiatedBy.getId();
    }

    private UUID handoffCancelledById(VehicleHandoff vehicleHandoff) {
        User cancelledBy = vehicleHandoff.getCancelledBy();
        if ( cancelledBy == null ) {
            return null;
        }
        return cancelledBy.getId();
    }
}
