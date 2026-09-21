package za.co.fleetexpense.mapper;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.TaxYearSummaryDTO;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.TaxYearSummary;
import za.co.fleetexpense.entity.Vehicle;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-21T10:29:40+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class TaxYearSummaryMapperImpl implements TaxYearSummaryMapper {

    @Override
    public TaxYearSummaryDTO toDTO(TaxYearSummary entity) {
        if ( entity == null ) {
            return null;
        }

        TaxYearSummaryDTO taxYearSummaryDTO = new TaxYearSummaryDTO();

        taxYearSummaryDTO.setOrganizationId( entityOrganizationId( entity ) );
        taxYearSummaryDTO.setVehicleId( entityVehicleId( entity ) );
        taxYearSummaryDTO.setVehicleRegistration( entityVehicleRegistrationNumber( entity ) );
        taxYearSummaryDTO.setBusinessKm( entity.getBusinessKm() );
        taxYearSummaryDTO.setBusinessPercentage( entity.getBusinessPercentage() );
        taxYearSummaryDTO.setClosingOdometer( entity.getClosingOdometer() );
        taxYearSummaryDTO.setCreatedAt( entity.getCreatedAt() );
        taxYearSummaryDTO.setFixedExpensesZar( entity.getFixedExpensesZar() );
        taxYearSummaryDTO.setFuelExpensesZar( entity.getFuelExpensesZar() );
        taxYearSummaryDTO.setId( entity.getId() );
        taxYearSummaryDTO.setLastCalculated( entity.getLastCalculated() );
        taxYearSummaryDTO.setMaintenanceExpensesZar( entity.getMaintenanceExpensesZar() );
        taxYearSummaryDTO.setOpeningOdometer( entity.getOpeningOdometer() );
        taxYearSummaryDTO.setPrivateKm( entity.getPrivateKm() );
        taxYearSummaryDTO.setTaxYear( entity.getTaxYear() );
        taxYearSummaryDTO.setTotalExpensesZar( entity.getTotalExpensesZar() );
        taxYearSummaryDTO.setTotalKm( entity.getTotalKm() );

        return taxYearSummaryDTO;
    }

    private UUID entityOrganizationId(TaxYearSummary taxYearSummary) {
        Organization organization = taxYearSummary.getOrganization();
        if ( organization == null ) {
            return null;
        }
        return organization.getId();
    }

    private UUID entityVehicleId(TaxYearSummary taxYearSummary) {
        Vehicle vehicle = taxYearSummary.getVehicle();
        if ( vehicle == null ) {
            return null;
        }
        return vehicle.getId();
    }

    private String entityVehicleRegistrationNumber(TaxYearSummary taxYearSummary) {
        Vehicle vehicle = taxYearSummary.getVehicle();
        if ( vehicle == null ) {
            return null;
        }
        return vehicle.getRegistrationNumber();
    }
}
