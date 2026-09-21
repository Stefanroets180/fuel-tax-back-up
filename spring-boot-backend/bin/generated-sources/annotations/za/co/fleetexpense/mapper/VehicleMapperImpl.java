package za.co.fleetexpense.mapper;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.VehicleCreateRequest;
import za.co.fleetexpense.dto.VehicleDTO;
import za.co.fleetexpense.dto.VehicleUpdateRequest;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-21T10:29:41+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class VehicleMapperImpl implements VehicleMapper {

    @Override
    public VehicleDTO toDTO(Vehicle entity) {
        if ( entity == null ) {
            return null;
        }

        VehicleDTO vehicleDTO = new VehicleDTO();

        vehicleDTO.setOrganizationId( entityOrganizationId( entity ) );
        vehicleDTO.setAssignedDriverId( entityAssignedDriverId( entity ) );
        vehicleDTO.setAssignedDriverName( formatDriverName( entityAssignedDriverFirstName( entity ) ) );
        vehicleDTO.setStatus( entity.getStatus() );
        vehicleDTO.setRejectionReason( entity.getRejectionReason() );
        vehicleDTO.setRejectedAt( entity.getRejectedAt() );
        vehicleDTO.setRejectedByUserId( entityRejectedByUserId( entity ) );
        vehicleDTO.setRejectedByName( formatRejectedByName( entityRejectedByUserFirstName( entity ) ) );
        vehicleDTO.setComputedOdometer( entity.getComputedOdometer() );
        vehicleDTO.setBrakeOverhaulIntervalKm( entity.getBrakeOverhaulIntervalKm() );
        vehicleDTO.setBrakeOverhaulIntervalMonths( entity.getBrakeOverhaulIntervalMonths() );
        vehicleDTO.setColor( entity.getColor() );
        vehicleDTO.setCompliant( entity.getCompliant() );
        vehicleDTO.setCreatedAt( entity.getCreatedAt() );
        vehicleDTO.setDrivetrainType( entity.getDrivetrainType() );
        vehicleDTO.setFuelType( entity.getFuelType() );
        vehicleDTO.setId( entity.getId() );
        vehicleDTO.setInsurancePolicyNumber( entity.getInsurancePolicyNumber() );
        vehicleDTO.setIsActive( entity.getIsActive() );
        vehicleDTO.setIsLocked( entity.getIsLocked() );
        vehicleDTO.setLicenseExpiry( entity.getLicenseExpiry() );
        vehicleDTO.setLockedAt( entity.getLockedAt() );
        vehicleDTO.setLockedReason( entity.getLockedReason() );
        vehicleDTO.setMajorServiceIntervalKm( entity.getMajorServiceIntervalKm() );
        vehicleDTO.setMajorServiceIntervalMonths( entity.getMajorServiceIntervalMonths() );
        vehicleDTO.setMake( entity.getMake() );
        vehicleDTO.setMinorServiceIntervalKm( entity.getMinorServiceIntervalKm() );
        vehicleDTO.setMinorServiceIntervalMonths( entity.getMinorServiceIntervalMonths() );
        vehicleDTO.setModel( entity.getModel() );
        vehicleDTO.setNickname( entity.getNickname() );
        vehicleDTO.setNotes( entity.getNotes() );
        vehicleDTO.setPurchaseDate( entity.getPurchaseDate() );
        vehicleDTO.setPurchasePrice( entity.getPurchasePrice() );
        vehicleDTO.setRegistrationNumber( entity.getRegistrationNumber() );
        vehicleDTO.setTankCapacityLiters( entity.getTankCapacityLiters() );
        vehicleDTO.setTrackerSerial( entity.getTrackerSerial() );
        vehicleDTO.setUpdatedAt( entity.getUpdatedAt() );
        vehicleDTO.setVin( entity.getVin() );
        vehicleDTO.setYear( entity.getYear() );

        vehicleDTO.setCurrentOdometer( entity.getCurrentOdometer() );

        return vehicleDTO;
    }

    @Override
    public Vehicle toEntity(VehicleCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Vehicle.VehicleBuilder vehicle = Vehicle.builder();

        vehicle.organization( organizationFromId( request.getOrganizationId() ) );
        vehicle.assignedDriver( driverFromId( request.getAssignedDriverId() ) );
        vehicle.fuelType( request.getFuelType() );
        vehicle.drivetrainType( request.getDrivetrainType() );
        vehicle.minorServiceIntervalKm( request.getMinorServiceIntervalKm() );
        vehicle.majorServiceIntervalKm( request.getMajorServiceIntervalKm() );
        vehicle.brakeOverhaulIntervalKm( request.getBrakeOverhaulIntervalKm() );
        vehicle.minorServiceIntervalMonths( request.getMinorServiceIntervalMonths() );
        vehicle.majorServiceIntervalMonths( request.getMajorServiceIntervalMonths() );
        vehicle.brakeOverhaulIntervalMonths( request.getBrakeOverhaulIntervalMonths() );
        vehicle.color( request.getColor() );
        vehicle.currentOdometer( request.getCurrentOdometer() );
        vehicle.insurancePolicyNumber( request.getInsurancePolicyNumber() );
        vehicle.licenseExpiry( request.getLicenseExpiry() );
        vehicle.make( request.getMake() );
        vehicle.model( request.getModel() );
        vehicle.nickname( request.getNickname() );
        vehicle.notes( request.getNotes() );
        vehicle.purchaseDate( request.getPurchaseDate() );
        vehicle.purchasePrice( request.getPurchasePrice() );
        vehicle.registrationNumber( request.getRegistrationNumber() );
        vehicle.tankCapacityLiters( request.getTankCapacityLiters() );
        vehicle.trackerSerial( request.getTrackerSerial() );
        vehicle.vin( request.getVin() );
        vehicle.year( request.getYear() );

        vehicle.isActive( true );
        vehicle.isLocked( false );
        vehicle.compliant( false );

        return vehicle.build();
    }

    @Override
    public void updateEntity(Vehicle entity, VehicleUpdateRequest request) {
        if ( request == null ) {
            return;
        }

        entity.setMinorServiceIntervalKm( request.getMinorServiceIntervalKm() );
        entity.setMajorServiceIntervalKm( request.getMajorServiceIntervalKm() );
        entity.setBrakeOverhaulIntervalKm( request.getBrakeOverhaulIntervalKm() );
        entity.setMinorServiceIntervalMonths( request.getMinorServiceIntervalMonths() );
        entity.setMajorServiceIntervalMonths( request.getMajorServiceIntervalMonths() );
        entity.setBrakeOverhaulIntervalMonths( request.getBrakeOverhaulIntervalMonths() );
        entity.setColor( request.getColor() );
        entity.setDrivetrainType( request.getDrivetrainType() );
        entity.setFuelType( request.getFuelType() );
        entity.setInsurancePolicyNumber( request.getInsurancePolicyNumber() );
        entity.setLicenseExpiry( request.getLicenseExpiry() );
        entity.setMake( request.getMake() );
        entity.setModel( request.getModel() );
        entity.setNickname( request.getNickname() );
        entity.setNotes( request.getNotes() );
        entity.setRegistrationNumber( request.getRegistrationNumber() );
        entity.setTankCapacityLiters( request.getTankCapacityLiters() );
        entity.setTrackerSerial( request.getTrackerSerial() );
        entity.setVin( request.getVin() );
        entity.setYear( request.getYear() );
    }

    private UUID entityOrganizationId(Vehicle vehicle) {
        Organization organization = vehicle.getOrganization();
        if ( organization == null ) {
            return null;
        }
        return organization.getId();
    }

    private UUID entityAssignedDriverId(Vehicle vehicle) {
        User assignedDriver = vehicle.getAssignedDriver();
        if ( assignedDriver == null ) {
            return null;
        }
        return assignedDriver.getId();
    }

    private String entityAssignedDriverFirstName(Vehicle vehicle) {
        User assignedDriver = vehicle.getAssignedDriver();
        if ( assignedDriver == null ) {
            return null;
        }
        return assignedDriver.getFirstName();
    }

    private UUID entityRejectedByUserId(Vehicle vehicle) {
        User rejectedByUser = vehicle.getRejectedByUser();
        if ( rejectedByUser == null ) {
            return null;
        }
        return rejectedByUser.getId();
    }

    private String entityRejectedByUserFirstName(Vehicle vehicle) {
        User rejectedByUser = vehicle.getRejectedByUser();
        if ( rejectedByUser == null ) {
            return null;
        }
        return rejectedByUser.getFirstName();
    }
}
