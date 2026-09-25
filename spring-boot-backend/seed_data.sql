-- Seed data for expense_tracking_backup database
-- This script adds sample data to test Slice A functionality (recipient selection and acquisition facts)

-- Insert Organization
INSERT INTO organizations (id, name, mode, default_tax_calculation_method, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'Test Organization',
    'FLEET',
    'ACTUAL_COST',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- Insert Users (for recipient selection testing)
INSERT INTO users (id, email, password, first_name, last_name, role, organization_id, email_verified, password_changed, created_at, updated_at)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440001', 'admin@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin', 'User', 'ADMIN', '550e8400-e29b-41d4-a716-446655440000', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('550e8400-e29b-41d4-a716-446655440002', 'employee1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'John', 'Employee', 'USER', '550e8400-e29b-41d4-a716-446655440000', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('550e8400-e29b-41d4-a716-446655440003', 'employee2@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Jane', 'Driver', 'USER', '550e8400-e29b-41d4-a716-446655440000', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Insert Vehicles
INSERT INTO vehicles (id, organization_id, make, model, year, registration_number, current_odometer, created_at, updated_at)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440010', '550e8400-e29b-41d4-a716-446655440000', 'Toyota', 'Hilux', 2022, 'CA 123 456', 50000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('550e8400-e29b-41d4-a716-446655440011', '550e8400-e29b-41d4-a716-446655440000', 'Ford', 'Ranger', 2023, 'CA 789 012', 30000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Insert Vehicle Tax Profiles (with different taxpayer types for testing)
INSERT INTO vehicle_tax_profiles (
    id, vehicle_id, taxpayer_type, taxpayer_vat_registered, date_placed_in_business_use, 
    default_calculation_method, is_company_provided_vehicle, created_at, updated_at
)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440020', '550e8400-e29b-41d4-a716-446655440010', 'EMPLOYEE', false, '2023-01-01', 'ACTUAL_COST', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('550e8400-e29b-41d4-a716-446655440021', '550e8400-e29b-41d4-a716-446655440011', 'SOLE_PROPRIETOR', true, '2023-06-01', 'ACTUAL_COST', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Note: recipient_user_id and acquisition facts will be added through the UI after V23 migration runs
