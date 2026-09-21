package za.co.fleetexpense.mapper;

import java.time.OffsetDateTime;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.AddressDTO;
import za.co.fleetexpense.entity.Address;
import za.co.fleetexpense.entity.UserProfile;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-21T10:29:39+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class AddressMapperImpl implements AddressMapper {

    @Override
    public AddressDTO toDto(Address address) {
        if ( address == null ) {
            return null;
        }

        UUID userProfileId = null;
        UUID id = null;
        String recipientFullName = null;
        String streetNumber = null;
        String streetName = null;
        String flatUnitNumber = null;
        String buildingName = null;
        String suburb = null;
        String cityTown = null;
        String province = null;
        String postalCode = null;
        String country = null;
        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;

        userProfileId = addressUserProfileId( address );
        id = address.getId();
        recipientFullName = address.getRecipientFullName();
        streetNumber = address.getStreetNumber();
        streetName = address.getStreetName();
        flatUnitNumber = address.getFlatUnitNumber();
        buildingName = address.getBuildingName();
        suburb = address.getSuburb();
        cityTown = address.getCityTown();
        province = address.getProvince();
        postalCode = address.getPostalCode();
        country = address.getCountry();
        createdAt = address.getCreatedAt();
        updatedAt = address.getUpdatedAt();

        AddressDTO addressDTO = new AddressDTO( id, userProfileId, recipientFullName, streetNumber, streetName, flatUnitNumber, buildingName, suburb, cityTown, province, postalCode, country, createdAt, updatedAt );

        return addressDTO;
    }

    @Override
    public Address toEntity(AddressDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Address.AddressBuilder address = Address.builder();

        address.buildingName( dto.buildingName() );
        address.cityTown( dto.cityTown() );
        address.country( dto.country() );
        address.createdAt( dto.createdAt() );
        address.flatUnitNumber( dto.flatUnitNumber() );
        address.id( dto.id() );
        address.postalCode( dto.postalCode() );
        address.province( dto.province() );
        address.recipientFullName( dto.recipientFullName() );
        address.streetName( dto.streetName() );
        address.streetNumber( dto.streetNumber() );
        address.suburb( dto.suburb() );
        address.updatedAt( dto.updatedAt() );

        return address.build();
    }

    private UUID addressUserProfileId(Address address) {
        UserProfile userProfile = address.getUserProfile();
        if ( userProfile == null ) {
            return null;
        }
        return userProfile.getId();
    }
}
