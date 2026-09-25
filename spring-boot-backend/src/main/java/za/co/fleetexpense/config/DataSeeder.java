package za.co.fleetexpense.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.VehicleTaxProfile;
import za.co.fleetexpense.entity.enums.OrganizationMode;
import za.co.fleetexpense.entity.enums.TaxCalculationMethod;
import za.co.fleetexpense.entity.enums.UserRole;
import za.co.fleetexpense.entity.enums.TaxpayerType;
import za.co.fleetexpense.repository.OrganizationRepository;
import za.co.fleetexpense.repository.UserRepository;
import za.co.fleetexpense.repository.VehicleRepository;
import za.co.fleetexpense.repository.VehicleTaxProfileRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
public class DataSeeder implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleTaxProfileRepository vehicleTaxProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Seeding database with initial data...");
        seedData();
        log.info("Database seeding completed.");
    }

    private void seedData() {
        // Create Organization
        Organization org = organizationRepository.findById(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .orElseGet(() -> {
                    Organization newOrg = Organization.builder()
                            .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                            .name("Test Organization")
                            .mode(OrganizationMode.FLEET)
                            .defaultTaxCalculationMethod(TaxCalculationMethod.ACTUAL_COSTS)
                            .build();
                    return organizationRepository.save(newOrg);
                });

        // Create Users
        if (userRepository.findByEmail("admin@test.com").isEmpty()) {
            User admin = User.builder()
                    .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"))
                    .email("admin@test.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .firstName("Admin")
                    .lastName("User")
                    .role(UserRole.ADMIN)
                    .organization(org)
                    .emailVerified(true)
                    .passwordChanged(true)
                    .build();
            userRepository.save(admin);
        }

        if (userRepository.findByEmail("employee1@test.com").isEmpty()) {
            User employee1 = User.builder()
                    .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440002"))
                    .email("employee1@test.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .firstName("John")
                    .lastName("Employee")
                    .role(UserRole.DRIVER)
                    .organization(org)
                    .emailVerified(true)
                    .passwordChanged(true)
                    .build();
            userRepository.save(employee1);
        }

        if (userRepository.findByEmail("employee2@test.com").isEmpty()) {
            User employee2 = User.builder()
                    .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440003"))
                    .email("employee2@test.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .firstName("Jane")
                    .lastName("Driver")
                    .role(UserRole.DRIVER)
                    .organization(org)
                    .emailVerified(true)
                    .passwordChanged(true)
                    .build();
            userRepository.save(employee2);
        }

        // Create Vehicles
        Vehicle vehicle1 = vehicleRepository.findById(UUID.fromString("550e8400-e29b-41d4-a716-446655440010"))
                .orElseGet(() -> {
                    Vehicle newVehicle = Vehicle.builder()
                            .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440010"))
                            .organization(org)
                            .make("Toyota")
                            .model("Hilux")
                            .year(2022)
                            .registrationNumber("CA 123 456")
                            .currentOdometer(50000)
                            .build();
                    return vehicleRepository.save(newVehicle);
                });

        Vehicle vehicle2 = vehicleRepository.findById(UUID.fromString("550e8400-e29b-41d4-a716-446655440011"))
                .orElseGet(() -> {
                    Vehicle newVehicle = Vehicle.builder()
                            .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440011"))
                            .organization(org)
                            .make("Ford")
                            .model("Ranger")
                            .year(2023)
                            .registrationNumber("CA 789 012")
                            .currentOdometer(30000)
                            .build();
                    return vehicleRepository.save(newVehicle);
                });

        // Create Vehicle Tax Profiles
        if (!vehicleTaxProfileRepository.existsById(UUID.fromString("550e8400-e29b-41d4-a716-446655440020"))) {
            VehicleTaxProfile taxProfile1 = VehicleTaxProfile.builder()
                    .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440020"))
                    .vehicle(vehicle1)
                    .taxpayerType(TaxpayerType.EMPLOYEE)
                    .taxpayerVatRegistered(false)
                    .datePlacedInBusinessUse(java.time.LocalDate.of(2023, 1, 1))
                    .defaultCalculationMethod(TaxCalculationMethod.ACTUAL_COSTS)
                    .isCompanyProvidedVehicle(true)
                    .build();
            vehicleTaxProfileRepository.save(taxProfile1);
        }

        if (!vehicleTaxProfileRepository.existsById(UUID.fromString("550e8400-e29b-41d4-a716-446655440021"))) {
            VehicleTaxProfile taxProfile2 = VehicleTaxProfile.builder()
                    .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440021"))
                    .vehicle(vehicle2)
                    .taxpayerType(TaxpayerType.SOLE_PROPRIETOR)
                    .taxpayerVatRegistered(true)
                    .datePlacedInBusinessUse(java.time.LocalDate.of(2023, 6, 1))
                    .defaultCalculationMethod(TaxCalculationMethod.ACTUAL_COSTS)
                    .isCompanyProvidedVehicle(false)
                    .build();
            vehicleTaxProfileRepository.save(taxProfile2);
        }

        log.info("Seeding completed. Test data added if not already present.");
        log.info("Test login credentials: admin@test.com / password123");
    }
}
