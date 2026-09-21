package za.co.fleetexpense.mapper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.VehicleAssignmentDTO;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.VehicleAssignment;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-21T10:29:41+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class VehicleAssignmentMapperImpl implements VehicleAssignmentMapper {

    @Override
    public VehicleAssignmentDTO toDto(VehicleAssignment assignment) {
        if ( assignment == null ) {
            return null;
        }

        UUID vehicleId = null;
        String vehicleRegistration = null;
        UUID assignedDriverId = null;
        UUID assignedById = null;
        UUID unassignedById = null;
        UUID id = null;
        OffsetDateTime assignedAt = null;
        OffsetDateTime unassignedAt = null;
        String status = null;
        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;

        vehicleId = assignmentVehicleId( assignment );
        vehicleRegistration = assignmentVehicleRegistrationNumber( assignment );
        assignedDriverId = assignmentAssignedDriverId( assignment );
        assignedById = assignmentAssignedById( assignment );
        unassignedById = assignmentUnassignedById( assignment );
        id = assignment.getId();
        assignedAt = assignment.getAssignedAt();
        unassignedAt = assignment.getUnassignedAt();
        if ( assignment.getStatus() != null ) {
            status = assignment.getStatus().name();
        }
        createdAt = assignment.getCreatedAt();
        updatedAt = assignment.getUpdatedAt();

        String assignedDriverName = null;
        String assignedByName = null;
        String unassignedByName = null;

        VehicleAssignmentDTO vehicleAssignmentDTO = new VehicleAssignmentDTO( id, vehicleId, vehicleRegistration, assignedDriverId, assignedDriverName, assignedById, assignedByName, assignedAt, unassignedAt, unassignedById, unassignedByName, status, createdAt, updatedAt );

        return vehicleAssignmentDTO;
    }

    @Override
    public List<VehicleAssignmentDTO> toDtoList(List<VehicleAssignment> assignments) {
        if ( assignments == null ) {
            return null;
        }

        List<VehicleAssignmentDTO> list = new ArrayList<VehicleAssignmentDTO>( assignments.size() );
        for ( VehicleAssignment vehicleAssignment : assignments ) {
            list.add( toDto( vehicleAssignment ) );
        }

        return list;
    }

    private UUID assignmentVehicleId(VehicleAssignment vehicleAssignment) {
        Vehicle vehicle = vehicleAssignment.getVehicle();
        if ( vehicle == null ) {
            return null;
        }
        return vehicle.getId();
    }

    private String assignmentVehicleRegistrationNumber(VehicleAssignment vehicleAssignment) {
        Vehicle vehicle = vehicleAssignment.getVehicle();
        if ( vehicle == null ) {
            return null;
        }
        return vehicle.getRegistrationNumber();
    }

    private UUID assignmentAssignedDriverId(VehicleAssignment vehicleAssignment) {
        User assignedDriver = vehicleAssignment.getAssignedDriver();
        if ( assignedDriver == null ) {
            return null;
        }
        return assignedDriver.getId();
    }

    private UUID assignmentAssignedById(VehicleAssignment vehicleAssignment) {
        User assignedBy = vehicleAssignment.getAssignedBy();
        if ( assignedBy == null ) {
            return null;
        }
        return assignedBy.getId();
    }

    private UUID assignmentUnassignedById(VehicleAssignment vehicleAssignment) {
        User unassignedBy = vehicleAssignment.getUnassignedBy();
        if ( unassignedBy == null ) {
            return null;
        }
        return unassignedBy.getId();
    }
}
