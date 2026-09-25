package za.co.fleetexpense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.fleetexpense.entity.enums.VehicleArrangementType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTaxAcquisitionFactsDTO {
    private UUID id;
    private UUID vehicleId;
    private UUID recipientUserId;
    private String recipientUserName;
    private String recipientUserEmail;
    private LocalDate recipientAcquisitionDate;
    private Long recipientAcquisitionCostCents;
    private Long originalPurchaseDebtCents;
    private VehicleArrangementType vehicleArrangementType;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
