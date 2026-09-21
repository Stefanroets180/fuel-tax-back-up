package za.co.fleetexpense.mapper;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.TripCreateRequest;
import za.co.fleetexpense.dto.TripDTO;
import za.co.fleetexpense.dto.TripUpdateRequest;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.Trip;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-21T10:29:41+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class TripMapperImpl implements TripMapper {

    @Override
    public TripDTO toDTO(Trip entity) {
        if ( entity == null ) {
            return null;
        }

        TripDTO tripDTO = new TripDTO();

        tripDTO.setOrganizationId( entityOrganizationId( entity ) );
        tripDTO.setVehicleId( entityVehicleId( entity ) );
        tripDTO.setVehicleRegistration( entityVehicleRegistrationNumber( entity ) );
        tripDTO.setUserId( entityUserId( entity ) );
        tripDTO.setDriverName( formatDriverName( entityUserFirstName( entity ) ) );
        tripDTO.setCreatedAt( entity.getCreatedAt() );
        tripDTO.setCustomerClientName( entity.getCustomerClientName() );
        tripDTO.setEndLocation( entity.getEndLocation() );
        tripDTO.setEndOdometer( entity.getEndOdometer() );
        tripDTO.setEndTime( entity.getEndTime() );
        tripDTO.setId( entity.getId() );
        tripDTO.setIsLocked( entity.getIsLocked() );
        tripDTO.setLockedAt( entity.getLockedAt() );
        tripDTO.setLockedReason( entity.getLockedReason() );
        tripDTO.setParkingCostsZar( entity.getParkingCostsZar() );
        tripDTO.setPurpose( entity.getPurpose() );
        tripDTO.setReasonForTrip( entity.getReasonForTrip() );
        tripDTO.setRouteDescription( entity.getRouteDescription() );
        tripDTO.setStartLocation( entity.getStartLocation() );
        tripDTO.setStartOdometer( entity.getStartOdometer() );
        tripDTO.setStartTime( entity.getStartTime() );
        tripDTO.setTollCostsZar( entity.getTollCostsZar() );
        tripDTO.setTripDate( entity.getTripDate() );
        tripDTO.setUpdatedAt( entity.getUpdatedAt() );

        tripDTO.setLockedByUserId( extractLockedByUserIdSafely(entity) );
        tripDTO.setDistanceKm( calculateDistance(entity) );
        tripDTO.setTotalTripCosts( calculateTotalCosts(entity) );
        tripDTO.setIsBusinessTrip( isBusinessTrip(entity) );
        tripDTO.setBusinessKm( calculateBusinessKm(entity) );
        tripDTO.setPrivateKm( calculatePrivateKm(entity) );

        return tripDTO;
    }

    @Override
    public Trip toEntity(TripCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Trip.TripBuilder trip = Trip.builder();

        trip.vehicle( vehicleFromId( request.getVehicleId() ) );
        trip.customerClientName( request.getCustomerClientName() );
        trip.endLocation( request.getEndLocation() );
        trip.endOdometer( request.getEndOdometer() );
        trip.endTime( request.getEndTime() );
        trip.parkingCostsZar( request.getParkingCostsZar() );
        trip.purpose( request.getPurpose() );
        trip.reasonForTrip( request.getReasonForTrip() );
        trip.routeDescription( request.getRouteDescription() );
        trip.startLocation( request.getStartLocation() );
        trip.startOdometer( request.getStartOdometer() );
        trip.startTime( request.getStartTime() );
        trip.tollCostsZar( request.getTollCostsZar() );
        trip.tripDate( request.getTripDate() );

        trip.isLocked( false );

        return trip.build();
    }

    @Override
    public void updateEntity(Trip entity, TripUpdateRequest request) {
        if ( request == null ) {
            return;
        }

        entity.setVehicle( vehicleFromId( request.getVehicleId() ) );
        entity.setCustomerClientName( request.getCustomerClientName() );
        entity.setEndLocation( request.getEndLocation() );
        entity.setEndOdometer( request.getEndOdometer() );
        entity.setEndTime( request.getEndTime() );
        entity.setParkingCostsZar( request.getParkingCostsZar() );
        entity.setPurpose( request.getPurpose() );
        entity.setReasonForTrip( request.getReasonForTrip() );
        entity.setRouteDescription( request.getRouteDescription() );
        entity.setStartLocation( request.getStartLocation() );
        entity.setStartOdometer( request.getStartOdometer() );
        entity.setStartTime( request.getStartTime() );
        entity.setTollCostsZar( request.getTollCostsZar() );
        entity.setTripDate( request.getTripDate() );
    }

    private UUID entityOrganizationId(Trip trip) {
        Organization organization = trip.getOrganization();
        if ( organization == null ) {
            return null;
        }
        return organization.getId();
    }

    private UUID entityVehicleId(Trip trip) {
        Vehicle vehicle = trip.getVehicle();
        if ( vehicle == null ) {
            return null;
        }
        return vehicle.getId();
    }

    private String entityVehicleRegistrationNumber(Trip trip) {
        Vehicle vehicle = trip.getVehicle();
        if ( vehicle == null ) {
            return null;
        }
        return vehicle.getRegistrationNumber();
    }

    private UUID entityUserId(Trip trip) {
        User user = trip.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getId();
    }

    private String entityUserFirstName(Trip trip) {
        User user = trip.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getFirstName();
    }
}
