package za.co.fleetexpense.entity.enums;

/**
 * Vehicle arrangement type for actual-cost tax calculation.
 * Represents the ownership/lease arrangement for a specific tax recipient.
 */
public enum VehicleArrangementType {
    /**
     * Vehicle is owned or financed by the tax recipient.
     * Wear-and-tear allowance applies. Finance charges may be limited.
     */
    OWNED,
    
    /**
     * Vehicle is leased by the tax recipient.
     * Lease payments apply instead of wear-and-tear.
     */
    LEASED
}
