package za.co.fleetexpense.mapper;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.OrganizationCreateRequest;
import za.co.fleetexpense.dto.OrganizationDTO;
import za.co.fleetexpense.entity.Organization;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-21T10:29:40+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class OrganizationMapperImpl implements OrganizationMapper {

    @Override
    public OrganizationDTO toDTO(Organization entity) {
        if ( entity == null ) {
            return null;
        }

        OrganizationDTO organizationDTO = new OrganizationDTO();

        organizationDTO.setCity( entity.getCity() );
        organizationDTO.setConditionReportEnabled( entity.getConditionReportEnabled() );
        organizationDTO.setCreatedAt( entity.getCreatedAt() );
        organizationDTO.setDefaultTaxCalculationMethod( entity.getDefaultTaxCalculationMethod() );
        organizationDTO.setId( entity.getId() );
        organizationDTO.setLogoUrl( entity.getLogoUrl() );
        organizationDTO.setMode( entity.getMode() );
        organizationDTO.setName( entity.getName() );
        organizationDTO.setOdometerConfirmationEnabled( entity.getOdometerConfirmationEnabled() );
        organizationDTO.setPostalCode( entity.getPostalCode() );
        organizationDTO.setProvince( entity.getProvince() );
        organizationDTO.setTaxNumber( entity.getTaxNumber() );
        organizationDTO.setUpdatedAt( entity.getUpdatedAt() );
        organizationDTO.setVatNumber( entity.getVatNumber() );

        return organizationDTO;
    }

    @Override
    public Organization toEntity(OrganizationCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Organization.OrganizationBuilder organization = Organization.builder();

        organization.city( request.getCity() );
        organization.defaultTaxCalculationMethod( request.getDefaultTaxCalculationMethod() );
        organization.mode( request.getMode() );
        organization.name( request.getName() );
        organization.postalCode( request.getPostalCode() );
        organization.province( request.getProvince() );
        organization.taxNumber( request.getTaxNumber() );
        organization.vatNumber( request.getVatNumber() );

        return organization.build();
    }

    @Override
    public void updateEntity(Organization entity, OrganizationCreateRequest request) {
        if ( request == null ) {
            return;
        }

        entity.setCity( request.getCity() );
        entity.setDefaultTaxCalculationMethod( request.getDefaultTaxCalculationMethod() );
        entity.setMode( request.getMode() );
        entity.setName( request.getName() );
        entity.setPostalCode( request.getPostalCode() );
        entity.setProvince( request.getProvince() );
        entity.setTaxNumber( request.getTaxNumber() );
        entity.setVatNumber( request.getVatNumber() );
    }
}
