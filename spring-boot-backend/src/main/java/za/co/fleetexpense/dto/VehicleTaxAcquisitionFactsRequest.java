package za.co.fleetexpense.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.fleetexpense.entity.enums.VehicleArrangementType;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTaxAcquisitionFactsRequest {
    @NotNull
    private UUID recipientUserId;

    private LocalDate recipientAcquisitionDate;

    private Long recipientAcquisitionCostCents;

    private Long originalPurchaseDebtCents;

    @NotNull
    private VehicleArrangementType vehicleArrangementType;
}
