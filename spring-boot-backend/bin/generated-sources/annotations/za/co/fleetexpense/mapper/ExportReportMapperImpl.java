package za.co.fleetexpense.mapper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.fleetexpense.dto.ExportReportDTO;
import za.co.fleetexpense.entity.ExportReport;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.enums.ExportReportFormat;
import za.co.fleetexpense.enums.ExportReportType;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-21T10:29:39+0200",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class ExportReportMapperImpl implements ExportReportMapper {

    @Override
    public ExportReportDTO toDto(ExportReport entity) {
        if ( entity == null ) {
            return null;
        }

        UUID organizationId = null;
        UUID requestedById = null;
        UUID id = null;
        ExportReportType reportType = null;
        ExportReportFormat reportFormat = null;
        OffsetDateTime dateFrom = null;
        OffsetDateTime dateTo = null;
        String fileName = null;
        Long fileSizeBytes = null;
        String downloadUrl = null;
        String status = null;
        String errorMessage = null;
        OffsetDateTime createdAt = null;
        OffsetDateTime completedAt = null;

        organizationId = entityOrganizationId( entity );
        requestedById = entityRequestedById( entity );
        id = entity.getId();
        reportType = entity.getReportType();
        reportFormat = entity.getReportFormat();
        dateFrom = entity.getDateFrom();
        dateTo = entity.getDateTo();
        fileName = entity.getFileName();
        fileSizeBytes = entity.getFileSizeBytes();
        downloadUrl = entity.getDownloadUrl();
        status = entity.getStatus();
        errorMessage = entity.getErrorMessage();
        createdAt = entity.getCreatedAt();
        completedAt = entity.getCompletedAt();

        String requestedByName = entity.getRequestedBy().getFirstName() + " " + entity.getRequestedBy().getLastName();

        ExportReportDTO exportReportDTO = new ExportReportDTO( id, organizationId, requestedById, requestedByName, reportType, reportFormat, dateFrom, dateTo, fileName, fileSizeBytes, downloadUrl, status, errorMessage, createdAt, completedAt );

        return exportReportDTO;
    }

    @Override
    public List<ExportReportDTO> toDtoList(List<ExportReport> entities) {
        if ( entities == null ) {
            return null;
        }

        List<ExportReportDTO> list = new ArrayList<ExportReportDTO>( entities.size() );
        for ( ExportReport exportReport : entities ) {
            list.add( toDto( exportReport ) );
        }

        return list;
    }

    private UUID entityOrganizationId(ExportReport exportReport) {
        Organization organization = exportReport.getOrganization();
        if ( organization == null ) {
            return null;
        }
        return organization.getId();
    }

    private UUID entityRequestedById(ExportReport exportReport) {
        User requestedBy = exportReport.getRequestedBy();
        if ( requestedBy == null ) {
            return null;
        }
        return requestedBy.getId();
    }
}
