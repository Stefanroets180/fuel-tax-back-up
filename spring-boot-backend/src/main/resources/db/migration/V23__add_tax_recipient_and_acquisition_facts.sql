-- Add explicit SARS recipient to vehicle tax profiles
-- Create vehicle tax acquisition facts table for recipient-specific acquisition data

-- Part A: Add recipient_user_id to vehicle_tax_profiles
ALTER TABLE vehicle_tax_profiles
ADD COLUMN recipient_user_id UUID;

-- Add foreign key constraint
ALTER TABLE vehicle_tax_profiles
ADD CONSTRAINT fk_tax_profile_recipient_user
FOREIGN KEY (recipient_user_id) REFERENCES users(id);

-- Add index for recipient_user_id
CREATE INDEX idx_vehicle_tax_profiles_recipient_user
ON vehicle_tax_profiles(recipient_user_id);

-- Add comment for documentation
COMMENT ON COLUMN vehicle_tax_profiles.recipient_user_id IS 'Explicit SARS natural-person recipient for individual travel-tax purposes. Nullable for COMPANY taxpayer type and legacy profiles.';

-- Part B: Create vehicle_tax_acquisition_facts table
CREATE TABLE vehicle_tax_acquisition_facts (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    recipient_user_id UUID NOT NULL,
    recipient_acquisition_date DATE,
    recipient_acquisition_cost_cents BIGINT,
    original_purchase_debt_cents BIGINT,
    vehicle_arrangement_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_acquisition_facts_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT fk_acquisition_facts_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES users(id),
    CONSTRAINT uk_acquisition_facts_vehicle_recipient
        UNIQUE (vehicle_id, recipient_user_id),
    CONSTRAINT chk_arrangement_type
        CHECK (vehicle_arrangement_type IN ('OWNED', 'LEASED')),
    CONSTRAINT chk_acquisition_cost_non_negative
        CHECK (recipient_acquisition_cost_cents IS NULL OR recipient_acquisition_cost_cents >= 0),
    CONSTRAINT chk_purchase_debt_non_negative
        CHECK (original_purchase_debt_cents IS NULL OR original_purchase_debt_cents >= 0)
);

-- Add indexes for vehicle and recipient
CREATE INDEX idx_acquisition_facts_vehicle ON vehicle_tax_acquisition_facts(vehicle_id);
CREATE INDEX idx_acquisition_facts_recipient ON vehicle_tax_acquisition_facts(recipient_user_id);

-- Add comments for documentation
COMMENT ON TABLE vehicle_tax_acquisition_facts IS 'Recipient-specific vehicle acquisition/arrangement facts for actual-cost tax calculation. One record per vehicle + recipient, shared across historical tax profiles.';
COMMENT ON COLUMN vehicle_tax_acquisition_facts.recipient_acquisition_date IS 'Date this tax recipient acquired the vehicle. Required for OWNED arrangement.';
COMMENT ON COLUMN vehicle_tax_acquisition_facts.recipient_acquisition_cost_cents IS 'Recipient-specific acquisition amount in cents for actual-cost W&T logic. Required for OWNED arrangement.';
COMMENT ON COLUMN vehicle_tax_acquisition_facts.original_purchase_debt_cents IS 'Original debt incurred in respect of acquisition. Nullable when not financed. Represents acquisition financing only, not later refinance balance.';
COMMENT ON COLUMN vehicle_tax_acquisition_facts.vehicle_arrangement_type IS 'Vehicle arrangement type: OWNED (owned/financed, W&T applies) or LEASED (lease payments apply instead of W&T).';
