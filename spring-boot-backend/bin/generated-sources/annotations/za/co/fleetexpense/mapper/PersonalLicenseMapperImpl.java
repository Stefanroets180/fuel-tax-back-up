package za.co.fleetexpense.mapper;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.PersonalLicenseCreateRequest;
import za.co.fleetexpense.dto.PersonalLicenseDTO;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.PersonalLicense;
import za.co.fleetexpense.entity.User;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-30T12:40:48+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class PersonalLicenseMapperImpl implements PersonalLicenseMapper {

    @Override
    public PersonalLicenseDTO toDTO(PersonalLicense entity) {
        if ( entity == null ) {
            return null;
        }

        PersonalLicenseDTO personalLicenseDTO = new PersonalLicenseDTO();

        personalLicenseDTO.setOrganizationId( entityOrganizationId( entity ) );
        personalLicenseDTO.setUserId( entityUserId( entity ) );
        personalLicenseDTO.setCreatedAt( entity.getCreatedAt() );
        personalLicenseDTO.setExpiryDate( entity.getExpiryDate() );
        personalLicenseDTO.setId( entity.getId() );
        personalLicenseDTO.setIssueDate( entity.getIssueDate() );
        personalLicenseDTO.setLicenseNumber( entity.getLicenseNumber() );
        if ( entity.getLicenseType() != null ) {
            personalLicenseDTO.setLicenseType( entity.getLicenseType().name() );
        }
        personalLicenseDTO.setUpdatedAt( entity.getUpdatedAt() );

        return personalLicenseDTO;
    }

    @Override
    public PersonalLicense toEntity(PersonalLicenseCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        PersonalLicense.PersonalLicenseBuilder personalLicense = PersonalLicense.builder();

        personalLicense.applicationNumber( request.getApplicationNumber() );
        personalLicense.collectionDate( request.getCollectionDate() );
        personalLicense.dlcAddress( request.getDlcAddress() );
        personalLicense.dlcName( request.getDlcName() );
        personalLicense.expiryDate( request.getExpiryDate() );
        personalLicense.eyeTestDate( request.getEyeTestDate() );
        personalLicense.homeAffairsOffice( request.getHomeAffairsOffice() );
        personalLicense.idNumber( request.getIdNumber() );
        personalLicense.issueDate( request.getIssueDate() );
        personalLicense.licenseCode( request.getLicenseCode() );
        personalLicense.licenseNumber( request.getLicenseNumber() );
        personalLicense.licenseType( request.getLicenseType() );
        personalLicense.medicalExamDate( request.getMedicalExamDate() );
        personalLicense.medicalExamValid( request.getMedicalExamValid() );
        personalLicense.notes( request.getNotes() );
        personalLicense.pdpCategory( request.getPdpCategory() );
        personalLicense.pdpNumber( request.getPdpNumber() );
        personalLicense.penaltiesZar( request.getPenaltiesZar() );
        personalLicense.renewalFeeZar( request.getRenewalFeeZar() );
        personalLicense.renewalMethod( request.getRenewalMethod() );
        personalLicense.trainingProvider( request.getTrainingProvider() );

        return personalLicense.build();
    }

    @Override
    public void updateEntity(PersonalLicense entity, PersonalLicenseCreateRequest request) {
        if ( request == null ) {
            return;
        }

        entity.setApplicationNumber( request.getApplicationNumber() );
        entity.setCollectionDate( request.getCollectionDate() );
        entity.setDlcAddress( request.getDlcAddress() );
        entity.setDlcName( request.getDlcName() );
        entity.setExpiryDate( request.getExpiryDate() );
        entity.setEyeTestDate( request.getEyeTestDate() );
        entity.setHomeAffairsOffice( request.getHomeAffairsOffice() );
        entity.setIdNumber( request.getIdNumber() );
        entity.setIssueDate( request.getIssueDate() );
        entity.setLicenseCode( request.getLicenseCode() );
        entity.setLicenseNumber( request.getLicenseNumber() );
        entity.setLicenseType( request.getLicenseType() );
        entity.setMedicalExamDate( request.getMedicalExamDate() );
        entity.setMedicalExamValid( request.getMedicalExamValid() );
        entity.setNotes( request.getNotes() );
        entity.setPdpCategory( request.getPdpCategory() );
        entity.setPdpNumber( request.getPdpNumber() );
        entity.setPenaltiesZar( request.getPenaltiesZar() );
        entity.setRenewalFeeZar( request.getRenewalFeeZar() );
        entity.setRenewalMethod( request.getRenewalMethod() );
        entity.setTrainingProvider( request.getTrainingProvider() );
    }

    private UUID entityOrganizationId(PersonalLicense personalLicense) {
        Organization organization = personalLicense.getOrganization();
        if ( organization == null ) {
            return null;
        }
        return organization.getId();
    }

    private UUID entityUserId(PersonalLicense personalLicense) {
        User user = personalLicense.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getId();
    }
}
