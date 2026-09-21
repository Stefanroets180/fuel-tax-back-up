package za.co.fleetexpense.mapper;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.FuelLogCreateRequest;
import za.co.fleetexpense.dto.FuelLogDTO;
import za.co.fleetexpense.dto.FuelLogUpdateRequest;
import za.co.fleetexpense.entity.Expense;
import za.co.fleetexpense.entity.FuelLog;
import za.co.fleetexpense.entity.Vehicle;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-21T10:29:41+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class FuelLogMapperImpl implements FuelLogMapper {

    @Override
    public FuelLogDTO toDTO(FuelLog entity) {
        if ( entity == null ) {
            return null;
        }

        FuelLogDTO fuelLogDTO = new FuelLogDTO();

        fuelLogDTO.setExpenseId( entityExpenseId( entity ) );
        fuelLogDTO.setVehicleId( entityExpenseVehicleId( entity ) );
        fuelLogDTO.setPreviousOdometer( entity.getPreviousOdometer() );
        fuelLogDTO.setConsumptionCalculatedAt( entity.getConsumptionCalculatedAt() );
        fuelLogDTO.setConsumptionLPer100km( entity.getConsumptionLPer100km() );
        fuelLogDTO.setConsumptionNotes( entity.getConsumptionNotes() );
        fuelLogDTO.setCreatedAt( entity.getCreatedAt() );
        fuelLogDTO.setEfficiencyKmPerLiter( entity.getEfficiencyKmPerLiter() );
        fuelLogDTO.setFuelType( entity.getFuelType() );
        fuelLogDTO.setFullTank( entity.getFullTank() );
        fuelLogDTO.setGpsAccuracyMeters( entity.getGpsAccuracyMeters() );
        fuelLogDTO.setGpsLatitude( entity.getGpsLatitude() );
        fuelLogDTO.setGpsLongitude( entity.getGpsLongitude() );
        fuelLogDTO.setId( entity.getId() );
        fuelLogDTO.setIsBaselineFill( entity.getIsBaselineFill() );
        fuelLogDTO.setKmSinceLastFill( entity.getKmSinceLastFill() );
        fuelLogDTO.setLiters( entity.getLiters() );
        fuelLogDTO.setPricePerLiter( entity.getPricePerLiter() );
        fuelLogDTO.setStationLocation( entity.getStationLocation() );
        fuelLogDTO.setStationName( entity.getStationName() );

        fuelLogDTO.setCurrentOdometer( calculateCurrentOdometer(entity) );
        fuelLogDTO.setTotalCost( calculateTotalCost(entity) );

        return fuelLogDTO;
    }

    @Override
    public FuelLog toEntity(FuelLogCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        FuelLog.FuelLogBuilder fuelLog = FuelLog.builder();

        fuelLog.expense( expenseFromId( request.getExpenseId() ) );
        fuelLog.fuelType( request.getFuelType() );
        fuelLog.fullTank( request.getFullTank() );
        fuelLog.gpsAccuracyMeters( request.getGpsAccuracyMeters() );
        fuelLog.gpsLatitude( request.getGpsLatitude() );
        fuelLog.gpsLongitude( request.getGpsLongitude() );
        fuelLog.isBaselineFill( request.getIsBaselineFill() );
        fuelLog.liters( request.getLiters() );
        fuelLog.pricePerLiter( request.getPricePerLiter() );
        fuelLog.stationLocation( request.getStationLocation() );
        fuelLog.stationName( request.getStationName() );

        return fuelLog.build();
    }

    @Override
    public void updateEntity(FuelLog entity, FuelLogUpdateRequest request) {
        if ( request == null ) {
            return;
        }

        entity.setGpsLatitude( request.getGpsLatitude() );
        entity.setGpsLongitude( request.getGpsLongitude() );
        entity.setGpsAccuracyMeters( request.getGpsAccuracyMeters() );
        entity.setConsumptionNotes( request.getConsumptionNotes() );
        entity.setFuelType( request.getFuelType() );
        entity.setLiters( request.getLiters() );
        entity.setPricePerLiter( request.getPricePerLiter() );
        entity.setStationLocation( request.getStationLocation() );
        entity.setStationName( request.getStationName() );
    }

    private UUID entityExpenseId(FuelLog fuelLog) {
        Expense expense = fuelLog.getExpense();
        if ( expense == null ) {
            return null;
        }
        return expense.getId();
    }

    private UUID entityExpenseVehicleId(FuelLog fuelLog) {
        Expense expense = fuelLog.getExpense();
        if ( expense == null ) {
            return null;
        }
        Vehicle vehicle = expense.getVehicle();
        if ( vehicle == null ) {
            return null;
        }
        return vehicle.getId();
    }
}
