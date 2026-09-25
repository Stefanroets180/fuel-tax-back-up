CREATE TABLE IF NOT EXISTS vehicle_tax_acquisition_facts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL,
    recipient_user_id UUID NOT NULL,
    recipient_acquisition_date DATE,
    recipient_acquisition_cost_cents BIGINT,
    original_purchase_debt_cents BIGINT,
    vehicle_arrangement_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_acquisition_facts_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    CONSTRAINT fk_acquisition_facts_recipient FOREIGN KEY (recipient_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_acquisition_facts_vehicle_recipient UNIQUE (vehicle_id, recipient_user_id)
);

CREATE INDEX IF NOT EXISTS idx_acquisition_facts_vehicle ON vehicle_tax_acquisition_facts(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_acquisition_facts_recipient ON vehicle_tax_acquisition_facts(recipient_user_id);
