-- Preserve trip and expense history when users delete records.
-- Normal application queries will exclude rows where is_deleted = true;
-- historical odometer/audit queries may intentionally include them.

ALTER TABLE trips
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE expenses
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_trips_active_vehicle_date
    ON trips (vehicle_id, trip_date)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_expenses_active_vehicle_date
    ON expenses (vehicle_id, expense_date)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_trips_deleted_vehicle_date
    ON trips (vehicle_id, trip_date, is_deleted);

CREATE INDEX idx_expenses_deleted_vehicle_date
    ON expenses (vehicle_id, expense_date, is_deleted);
