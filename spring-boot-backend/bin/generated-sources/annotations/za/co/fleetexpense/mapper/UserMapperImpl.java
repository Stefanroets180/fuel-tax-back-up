package za.co.fleetexpense.mapper;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.UserCreateRequest;
import za.co.fleetexpense.dto.UserDTO;
import za.co.fleetexpense.dto.UserUpdateRequest;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.User;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-21T10:29:41+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserDTO toDTO(User entity) {
        if ( entity == null ) {
            return null;
        }

        UserDTO userDTO = new UserDTO();

        userDTO.setOrganizationId( entityOrganizationId( entity ) );
        userDTO.setOrganizationName( entityOrganizationName( entity ) );
        userDTO.setRole( entity.getRole() );
        userDTO.setCreatedAt( entity.getCreatedAt() );
        userDTO.setEmail( entity.getEmail() );
        userDTO.setEmailVerified( entity.getEmailVerified() );
        userDTO.setFirstName( entity.getFirstName() );
        userDTO.setId( entity.getId() );
        userDTO.setIsActive( entity.getIsActive() );
        userDTO.setLastName( entity.getLastName() );
        userDTO.setPasswordChanged( entity.getPasswordChanged() );
        userDTO.setPhone( entity.getPhone() );
        userDTO.setProfilePhotoUrl( entity.getProfilePhotoUrl() );
        userDTO.setUpdatedAt( entity.getUpdatedAt() );

        return userDTO;
    }

    @Override
    public User toEntity(UserCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.organization( organizationFromId( request.getOrganizationId() ) );
        user.email( request.getEmail() );
        user.firstName( request.getFirstName() );
        user.lastName( request.getLastName() );
        user.phone( request.getPhone() );
        user.role( request.getRole() );

        user.emailVerified( false );
        user.isActive( true );

        return user.build();
    }

    @Override
    public void updateEntity(User entity, UserUpdateRequest request) {
        if ( request == null ) {
            return;
        }

        entity.setEmail( request.getEmail() );
        entity.setFirstName( request.getFirstName() );
        entity.setIsActive( request.getIsActive() );
        entity.setLastName( request.getLastName() );
        entity.setPhone( request.getPhone() );
        entity.setRole( request.getRole() );
    }

    private UUID entityOrganizationId(User user) {
        Organization organization = user.getOrganization();
        if ( organization == null ) {
            return null;
        }
        return organization.getId();
    }

    private String entityOrganizationName(User user) {
        Organization organization = user.getOrganization();
        if ( organization == null ) {
            return null;
        }
        return organization.getName();
    }
}
