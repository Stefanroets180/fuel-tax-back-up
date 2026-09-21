package za.co.fleetexpense.mapper;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.VehicleFuelConsumptionStatsDTO;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.VehicleFuelConsumptionStats;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-21T10:29:40+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class VehicleFuelConsumptionStatsMapperImpl implements VehicleFuelConsumptionStatsMapper {

    @Override
    public VehicleFuelConsumptionStatsDTO toDTO(VehicleFuelConsumptionStats entity) {
        if ( entity == null ) {
            return null;
        }

        VehicleFuelConsumptionStatsDTO.VehicleFuelConsumptionStatsDTOBuilder vehicleFuelConsumptionStatsDTO = VehicleFuelConsumptionStatsDTO.builder();

        vehicleFuelConsumptionStatsDTO.organizationId( entityOrganizationId( entity ) );
        vehicleFuelConsumptionStatsDTO.vehicleId( entityVehicleId( entity ) );
        vehicleFuelConsumptionStatsDTO.vehicleRegistration( entityVehicleRegistrationNumber( entity ) );
        vehicleFuelConsumptionStatsDTO.averageConsumptionLPer100km( entity.getAverageConsumptionLPer100km() );
        vehicleFuelConsumptionStatsDTO.bestConsumptionLPer100km( entity.getBestConsumptionLPer100km() );
        vehicleFuelConsumptionStatsDTO.createdAt( entity.getCreatedAt() );
        vehicleFuelConsumptionStatsDTO.id( entity.getId() );
        vehicleFuelConsumptionStatsDTO.lastFillDate( entity.getLastFillDate() );
        vehicleFuelConsumptionStatsDTO.lastFillFullTank( entity.getLastFillFullTank() );
        vehicleFuelConsumptionStatsDTO.lastFillOdometer( entity.getLastFillOdometer() );
        vehicleFuelConsumptionStatsDTO.recentAverageConsumptionLPer100km( entity.getRecentAverageConsumptionLPer100km() );
        vehicleFuelConsumptionStatsDTO.totalDistanceKm( entity.getTotalDistanceKm() );
        vehicleFuelConsumptionStatsDTO.totalFuelCostZar( entity.getTotalFuelCostZar() );
        vehicleFuelConsumptionStatsDTO.totalFuelLogs( entity.getTotalFuelLogs() );
        vehicleFuelConsumptionStatsDTO.totalFullTankFills( entity.getTotalFullTankFills() );
        vehicleFuelConsumptionStatsDTO.totalLiters( entity.getTotalLiters() );
        vehicleFuelConsumptionStatsDTO.updatedAt( entity.getUpdatedAt() );
        vehicleFuelConsumptionStatsDTO.worstConsumptionLPer100km( entity.getWorstConsumptionLPer100km() );

        vehicleFuelConsumptionStatsDTO.vehicleName( entity.getVehicle() != null ? entity.getVehicle().getMake() + ' ' + entity.getVehicle().getModel() : null );

        return vehicleFuelConsumptionStatsDTO.build();
    }

    private UUID entityOrganizationId(VehicleFuelConsumptionStats vehicleFuelConsumptionStats) {
        Organization organization = vehicleFuelConsumptionStats.getOrganization();
        if ( organization == null ) {
            return null;
        }
        return organization.getId();
    }

    private UUID entityVehicleId(VehicleFuelConsumptionStats vehicleFuelConsumptionStats) {
        Vehicle vehicle = vehicleFuelConsumptionStats.getVehicle();
        if ( vehicle == null ) {
            return null;
        }
        return vehicle.getId();
    }

    private String entityVehicleRegistrationNumber(VehicleFuelConsumptionStats vehicleFuelConsumptionStats) {
        Vehicle vehicle = vehicleFuelConsumptionStats.getVehicle();
        if ( vehicle == null ) {
            return null;
        }
        return vehicle.getRegistrationNumber();
    }
}
