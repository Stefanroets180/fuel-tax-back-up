package za.co.fleetexpense.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import za.co.fleetexpense.entity.enums.VehicleArrangementType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Vehicle tax acquisition facts for a specific tax recipient.
 * Represents "this SARS recipient's acquisition/arrangement facts for this vehicle".
 * One record per vehicle + recipient, shared across historical tax profiles.
 */
@Entity
@Table(name = "vehicle_tax_acquisition_facts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"vehicle_id", "recipient_user_id"})
}, indexes = {
    @Index(name = "idx_acquisition_facts_vehicle", columnList = "vehicle_id"),
    @Index(name = "idx_acquisition_facts_recipient", columnList = "recipient_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleTaxAcquisitionFacts {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipientUser;

    /**
     * Date this tax recipient acquired the vehicle.
     * Required for OWNED arrangement for future actual-cost W&T logic.
     */
    @Column(name = "recipient_acquisition_date")
    private LocalDate recipientAcquisitionDate;

    /**
     * Recipient-specific acquisition amount in cents.
     * Required for OWNED arrangement for future actual-cost W&T logic.
     * Stored as cents to avoid BigDecimal-rand ambiguity.
     */
    @Column(name = "recipient_acquisition_cost_cents")
    private Long recipientAcquisitionCostCents;

    /**
     * Original debt incurred in respect of acquisition.
     * Nullable when not financed. Represents acquisition financing only,
     * not a later refinance balance.
     */
    @Column(name = "original_purchase_debt_cents")
    private Long originalPurchaseDebtCents;

    /**
     * Vehicle arrangement type for actual-cost tax calculation.
     * OWNED: Vehicle is owned or financed (W&T applies, finance charges may be limited)
     * LEASED: Vehicle is leased (lease payments apply instead of W&T)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_arrangement_type", nullable = false)
    private VehicleArrangementType vehicleArrangementType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
