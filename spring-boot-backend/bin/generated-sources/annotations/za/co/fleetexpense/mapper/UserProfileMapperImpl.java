package za.co.fleetexpense.mapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.AddressDTO;
import za.co.fleetexpense.dto.UserProfileDTO;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.UserProfile;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-21T10:29:41+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class UserProfileMapperImpl implements UserProfileMapper {

    @Override
    public UserProfileDTO toDto(UserProfile profile) {
        if ( profile == null ) {
            return null;
        }

        UUID userId = null;
        String driversLicenseNumber = null;
        String driversLicenseFrontUrl = null;
        String driversLicenseBackUrl = null;
        LocalDate driversLicenseExpiry = null;
        UUID id = null;
        String idNumber = null;
        String homePhone = null;
        String mobilePhone = null;
        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;

        userId = profileUserId( profile );
        driversLicenseNumber = profile.getDriversLicenseNumber();
        driversLicenseFrontUrl = profile.getDriverLicenseFrontUrl();
        driversLicenseBackUrl = profile.getDriverLicenseBackUrl();
        driversLicenseExpiry = profile.getDriverLicenseExpiry();
        id = profile.getId();
        idNumber = profile.getIdNumber();
        homePhone = profile.getHomePhone();
        mobilePhone = profile.getMobilePhone();
        createdAt = profile.getCreatedAt();
        updatedAt = profile.getUpdatedAt();

        AddressDTO address = null;

        UserProfileDTO userProfileDTO = new UserProfileDTO( id, userId, idNumber, homePhone, mobilePhone, driversLicenseNumber, driversLicenseFrontUrl, driversLicenseBackUrl, driversLicenseExpiry, address, createdAt, updatedAt );

        return userProfileDTO;
    }

    private UUID profileUserId(UserProfile userProfile) {
        User user = userProfile.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getId();
    }
}
