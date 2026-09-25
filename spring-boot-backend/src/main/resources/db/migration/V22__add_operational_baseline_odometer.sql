-- Add operational baseline odometer for Phase 1 operational odometer foundation.
-- This represents the vehicle's explicitly captured operational odometer when it entered the application's operational tracking.
-- It is separate from tax-year opening odometer and fleet handover odometer.
-- No backfill from current_odometer, Tax Readiness, OdometerConfirmation, or Trip Log.

ALTER TABLE vehicles
    ADD COLUMN operational_baseline_odometer INTEGER NULL;
