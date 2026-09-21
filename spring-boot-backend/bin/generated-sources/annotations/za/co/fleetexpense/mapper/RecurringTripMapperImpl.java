package za.co.fleetexpense.mapper;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.RecurringTripCreateRequest;
import za.co.fleetexpense.dto.RecurringTripDTO;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.RecurringTrip;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.enums.TripPurpose;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-21T10:29:40+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class RecurringTripMapperImpl implements RecurringTripMapper {

    @Override
    public RecurringTripDTO toDTO(RecurringTrip entity) {
        if ( entity == null ) {
            return null;
        }

        RecurringTripDTO.RecurringTripDTOBuilder recurringTripDTO = RecurringTripDTO.builder();

        recurringTripDTO.organizationId( entityOrganizationId( entity ) );
        recurringTripDTO.vehicleId( entityVehicleId( entity ) );
        recurringTripDTO.userId( entityUserId( entity ) );
        if ( entity.getPurpose() != null ) {
            recurringTripDTO.purpose( entity.getPurpose().name() );
        }
        recurringTripDTO.createdAt( entity.getCreatedAt() );
        recurringTripDTO.customerClientName( entity.getCustomerClientName() );
        recurringTripDTO.defaultParkingCostsZar( entity.getDefaultParkingCostsZar() );
        recurringTripDTO.defaultTollCostsZar( entity.getDefaultTollCostsZar() );
        recurringTripDTO.endLocation( entity.getEndLocation() );
        recurringTripDTO.endTime( entity.getEndTime() );
        recurringTripDTO.id( entity.getId() );
        recurringTripDTO.isActive( entity.getIsActive() );
        recurringTripDTO.isRecurring( entity.getIsRecurring() );
        recurringTripDTO.reasonForTrip( entity.getReasonForTrip() );
        recurringTripDTO.recurrenceDays( entity.getRecurrenceDays() );
        recurringTripDTO.recurrenceEndDate( entity.getRecurrenceEndDate() );
        recurringTripDTO.recurrenceStartDate( entity.getRecurrenceStartDate() );
        recurringTripDTO.routeDescription( entity.getRouteDescription() );
        recurringTripDTO.startLocation( entity.getStartLocation() );
        recurringTripDTO.startTime( entity.getStartTime() );
        recurringTripDTO.updatedAt( entity.getUpdatedAt() );

        recurringTripDTO.vehicleName( entity.getVehicle() != null ? entity.getVehicle().getMake() + ' ' + entity.getVehicle().getModel() : null );
        recurringTripDTO.userName( entity.getUser() != null ? entity.getUser().getFirstName() + ' ' + entity.getUser().getLastName() : null );

        return recurringTripDTO.build();
    }

    @Override
    public RecurringTrip toEntity(RecurringTripCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        RecurringTrip.RecurringTripBuilder recurringTrip = RecurringTrip.builder();

        recurringTrip.vehicle( vehicleFromId( request.getVehicleId() ) );
        recurringTrip.user( userFromId( request.getUserId() ) );
        recurringTrip.customerClientName( request.getCustomerClientName() );
        recurringTrip.defaultParkingCostsZar( request.getDefaultParkingCostsZar() );
        recurringTrip.defaultTollCostsZar( request.getDefaultTollCostsZar() );
        recurringTrip.endLocation( request.getEndLocation() );
        recurringTrip.endTime( request.getEndTime() );
        recurringTrip.isRecurring( request.getIsRecurring() );
        if ( request.getPurpose() != null ) {
            recurringTrip.purpose( Enum.valueOf( TripPurpose.class, request.getPurpose() ) );
        }
        recurringTrip.reasonForTrip( request.getReasonForTrip() );
        recurringTrip.recurrenceDays( request.getRecurrenceDays() );
        recurringTrip.recurrenceDaysOfMonth( request.getRecurrenceDaysOfMonth() );
        recurringTrip.recurrenceEndDate( request.getRecurrenceEndDate() );
        recurringTrip.recurrenceStartDate( request.getRecurrenceStartDate() );
        recurringTrip.routeDescription( request.getRouteDescription() );
        recurringTrip.startLocation( request.getStartLocation() );
        recurringTrip.startTime( request.getStartTime() );

        recurringTrip.isActive( true );

        return recurringTrip.build();
    }

    private UUID entityOrganizationId(RecurringTrip recurringTrip) {
        Organization organization = recurringTrip.getOrganization();
        if ( organization == null ) {
            return null;
        }
        return organization.getId();
    }

    private UUID entityVehicleId(RecurringTrip recurringTrip) {
        Vehicle vehicle = recurringTrip.getVehicle();
        if ( vehicle == null ) {
            return null;
        }
        return vehicle.getId();
    }

    private UUID entityUserId(RecurringTrip recurringTrip) {
        User user = recurringTrip.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getId();
    }
}
