package za.co.fleetexpense.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.fleetexpense.dto.ExpenseCreateRequest;
import za.co.fleetexpense.dto.ExpenseDTO;
import za.co.fleetexpense.dto.ExpenseUpdateRequest;
import za.co.fleetexpense.entity.Expense;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.enums.ExpenseCategory;
import za.co.fleetexpense.enums.TaxExpenseClassification;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for ExpenseService Phase 4C classification hardening.
 * No Spring context, no database, no external dependencies.
 * Tests focus on:
 * - Conservative classification mapping from ExpenseCategory
 * - PERSONAL_LICENSE correction
 * - CAR_WASH/VEHICLE_TRACKING/ROADWORTHY moved to UNCATEGORIZED
 * - ETOLL_SANRAL/OTHER_FIXED remain UNCATEGORIZED
 * - Real create/update flow classification ownership rules
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServicePhase4CTest {

    @Mock
    private za.co.fleetexpense.repository.ExpenseRepository expenseRepository;

    @Mock
    private za.co.fleetexpense.repository.OrganizationRepository organizationRepository;

    @Mock
    private za.co.fleetexpense.repository.VehicleRepository vehicleRepository;

    @Mock
    private za.co.fleetexpense.repository.UserRepository userRepository;

    @Mock
    private za.co.fleetexpense.mapper.ExpenseMapper expenseMapper;

    @Mock
    private za.co.fleetexpense.repository.FuelLogRepository fuelLogRepository;

    @Mock
    private za.co.fleetexpense.repository.CarWashRepository carWashRepository;

    @Mock
    private za.co.fleetexpense.repository.MechanicServiceRepository mechanicServiceRepository;

    @Mock
    private za.co.fleetexpense.repository.MaintenanceTopupRepository maintenanceTopupRepository;

    @Mock
    private za.co.fleetexpense.repository.TyreRepository tyreRepository;

    @Mock
    private TyreRotationService tyreRotationService;

    @Mock
    private za.co.fleetexpense.repository.TyreRotationTrackingRepository tyreRotationTrackingRepository;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Mock
    private OdometerDriftAlertService odometerDriftAlertService;

    @Mock
    private MechanicServiceAlertService mechanicServiceAlertService;

    @Mock
    private za.co.fleetexpense.repository.ExpiryAlertRepository expiryAlertRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private za.co.fleetexpense.repository.UserAssistantRepository userAssistantRepository;

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private ExpenseService expenseService;

    private UUID organizationId;
    private UUID userId;
    private UUID vehicleId;
    private Organization organization;
    private User user;
    private Vehicle vehicle;

    @Test
    void testFuelLog_QualifyingCurrentExpense() throws Exception {
        TaxExpenseClassification result = deriveTaxClassification(ExpenseCategory.FUEL_LOG);
        assertEquals(TaxExpenseClassification.QUALIFYING_CURRENT_EXPENSE, result);
    }

    @Test
    void testMechanicService_QualifyingCurrentExpense() throws Exception {
        TaxExpenseClassification result = deriveTaxClassification(ExpenseCategory.MECHANIC_SERVICE);
        assertEquals(TaxExpenseClassification.QUALIFYING_CURRENT_EXPENSE, result);
    }

    @Test
    void testMaintenanceTopup_QualifyingCurrentExpense() throws Exception {
        TaxExpenseClassification result = deriveTaxClassification(ExpenseCategory.MAINTENANCE_TOPUP);
        assertEquals(TaxExpenseClassification.QUALIFYING_CURRENT_EXPENSE, result);
    }

    @Test
    void testTires_QualifyingCurrentExpense() throws Exception {
        TaxExpenseClassification result = deriveTaxClassification(ExpenseCategory.TIRES);
        assertEquals(TaxExpenseClassification.QUALIFYING_CURRENT_EXPENSE, result);
    }

    @Test
    void testInsurancePremium_QualifyingCurrentExpense() throws Exception {
        TaxExpenseClassification result = deriveTaxClassification(ExpenseCategory.INSURANCE_PREMIUM);
        assertEquals(TaxExpenseClassification.QUALIFYING_CURRENT_EXPENSE, result);
    }

    @Test
    void testLicenseRenewal_QualifyingCurrentExpense() throws Exception {
        TaxExpenseClassification result = deriveTaxClassification(ExpenseCategory.LICENSE_RENEWAL);
        assertEquals(TaxExpenseClassification.QUALIFYING_CURRENT_EXPENSE, result);
    }

    @Test
    void testPersonalLicense_PersonalOrNonQualifying() throws Exception {
        TaxExpenseClassification result = deriveTaxClassification(ExpenseCategory.PERSONAL_LICENSE);
        assertEquals(TaxExpenseClassification.PERSONAL_OR_NON_QUALIFYING, result);
    }

    @Test
    void testCarWash_Uncategorized() throws Exception {
        TaxExpenseClassification result = deriveTaxClassification(ExpenseCategory.CAR_WASH);
        assertEquals(TaxExpenseClassification.UNCATEGORIZED, result);
    }

    @Test
    void testVehicleTracking_Uncategorized() throws Exception {
        TaxExpenseClassification result = deriveTaxClassification(ExpenseCategory.VEHICLE_TRACKING);
        assertEquals(TaxExpenseClassification.UNCATEGORIZED, result);
    }

    @Test
    void testRoadworthy_Uncategorized() throws Exception {
        TaxExpenseClassification result = deriveTaxClassification(ExpenseCategory.ROADWORTHY);
        assertEquals(TaxExpenseClassification.UNCATEGORIZED, result);
    }

    @Test
    void testEtollSanral_Uncategorized() throws Exception {
        TaxExpenseClassification result = deriveTaxClassification(ExpenseCategory.ETOLL_SANRAL);
        assertEquals(TaxExpenseClassification.UNCATEGORIZED, result);
    }

    @Test
    void testOtherFixed_Uncategorized() throws Exception {
        TaxExpenseClassification result = deriveTaxClassification(ExpenseCategory.OTHER_FIXED);
        assertEquals(TaxExpenseClassification.UNCATEGORIZED, result);
    }

    @Test
    void testNullCategory_Uncategorized() throws Exception {
        TaxExpenseClassification result = deriveTaxClassification(null);
        assertEquals(TaxExpenseClassification.UNCATEGORIZED, result);
    }

    // ==================== Real Create Flow Tests ====================

    @Test
    void testRealCreate_FuelLog_DerivesQualifyingCurrentExpense() {
        setupTestData();
        
        when(organizationRepository.findById(organizationId)).thenReturn(java.util.Optional.of(organization));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(vehicleRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));
        
        ExpenseCreateRequest request = new ExpenseCreateRequest();
        request.setVehicleId(vehicleId);
        request.setCategory(ExpenseCategory.FUEL_LOG);
        request.setAmountZar(BigDecimal.valueOf(50.00));
        request.setExpenseDate(LocalDate.now());
        request.setSupplierName("Test Supplier");
        
        when(expenseMapper.toEntity(request)).thenReturn(createMockExpense(ExpenseCategory.FUEL_LOG, null));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(expenseMapper.toDTO(any(Expense.class))).thenReturn(new ExpenseDTO());
        
        expenseService.create(request, organizationId, userId);
        
        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        
        Expense savedExpense = expenseCaptor.getValue();
        assertEquals(TaxExpenseClassification.QUALIFYING_CURRENT_EXPENSE, savedExpense.getTaxExpenseClassification());
        verify(vehicleService).recalculateOperationalOdometer(vehicleId);
    }

    @Test
    void testRealCreate_PersonalLicense_DerivesPersonalOrNonQualifying() {
        setupTestData();
        
        when(organizationRepository.findById(organizationId)).thenReturn(java.util.Optional.of(organization));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        
        ExpenseCreateRequest request = new ExpenseCreateRequest();
        request.setCategory(ExpenseCategory.PERSONAL_LICENSE);
        request.setAmountZar(BigDecimal.valueOf(50.00));
        request.setExpenseDate(LocalDate.now());
        request.setSupplierName("Test Supplier");
        
        when(expenseMapper.toEntity(request)).thenReturn(createMockExpense(ExpenseCategory.PERSONAL_LICENSE, null));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(expenseMapper.toDTO(any(Expense.class))).thenReturn(new ExpenseDTO());
        
        expenseService.create(request, organizationId, userId);
        
        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        
        Expense savedExpense = expenseCaptor.getValue();
        assertEquals(TaxExpenseClassification.PERSONAL_OR_NON_QUALIFYING, savedExpense.getTaxExpenseClassification());
    }

    @Test
    void testRealCreate_CarWash_DerivesUncategorized() {
        setupTestData();
        
        when(organizationRepository.findById(organizationId)).thenReturn(java.util.Optional.of(organization));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(vehicleRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));
        
        ExpenseCreateRequest request = new ExpenseCreateRequest();
        request.setVehicleId(vehicleId);
        request.setCategory(ExpenseCategory.CAR_WASH);
        request.setAmountZar(BigDecimal.valueOf(50.00));
        request.setExpenseDate(LocalDate.now());
        request.setSupplierName("Test Supplier");
        
        when(expenseMapper.toEntity(request)).thenReturn(createMockExpense(ExpenseCategory.CAR_WASH, null));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(expenseMapper.toDTO(any(Expense.class))).thenReturn(new ExpenseDTO());
        
        expenseService.create(request, organizationId, userId);
        
        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        
        Expense savedExpense = expenseCaptor.getValue();
        assertEquals(TaxExpenseClassification.UNCATEGORIZED, savedExpense.getTaxExpenseClassification());
    }

    @Test
    void testRealCreate_VehicleTracking_DerivesUncategorized() {
        setupTestData();
        
        when(organizationRepository.findById(organizationId)).thenReturn(java.util.Optional.of(organization));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(vehicleRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));
        
        ExpenseCreateRequest request = new ExpenseCreateRequest();
        request.setVehicleId(vehicleId);
        request.setCategory(ExpenseCategory.VEHICLE_TRACKING);
        request.setAmountZar(BigDecimal.valueOf(50.00));
        request.setExpenseDate(LocalDate.now());
        request.setSupplierName("Test Supplier");
        
        when(expenseMapper.toEntity(request)).thenReturn(createMockExpense(ExpenseCategory.VEHICLE_TRACKING, null));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(expenseMapper.toDTO(any(Expense.class))).thenReturn(new ExpenseDTO());
        
        expenseService.create(request, organizationId, userId);
        
        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        
        Expense savedExpense = expenseCaptor.getValue();
        assertEquals(TaxExpenseClassification.UNCATEGORIZED, savedExpense.getTaxExpenseClassification());
    }

    @Test
    void testRealCreate_Roadworthy_DerivesUncategorized() {
        setupTestData();
        
        when(organizationRepository.findById(organizationId)).thenReturn(java.util.Optional.of(organization));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(vehicleRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));
        
        ExpenseCreateRequest request = new ExpenseCreateRequest();
        request.setVehicleId(vehicleId);
        request.setCategory(ExpenseCategory.ROADWORTHY);
        request.setAmountZar(BigDecimal.valueOf(50.00));
        request.setExpenseDate(LocalDate.now());
        request.setSupplierName("Test Supplier");
        
        when(expenseMapper.toEntity(request)).thenReturn(createMockExpense(ExpenseCategory.ROADWORTHY, null));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(expenseMapper.toDTO(any(Expense.class))).thenReturn(new ExpenseDTO());
        
        expenseService.create(request, organizationId, userId);
        
        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        
        Expense savedExpense = expenseCaptor.getValue();
        assertEquals(TaxExpenseClassification.UNCATEGORIZED, savedExpense.getTaxExpenseClassification());
    }

    @Test
    void testRealCreate_EtollSanral_DerivesUncategorized() {
        setupTestData();
        
        when(organizationRepository.findById(organizationId)).thenReturn(java.util.Optional.of(organization));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(vehicleRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));
        
        ExpenseCreateRequest request = new ExpenseCreateRequest();
        request.setVehicleId(vehicleId);
        request.setCategory(ExpenseCategory.ETOLL_SANRAL);
        request.setAmountZar(BigDecimal.valueOf(50.00));
        request.setExpenseDate(LocalDate.now());
        request.setSupplierName("Test Supplier");
        
        when(expenseMapper.toEntity(request)).thenReturn(createMockExpense(ExpenseCategory.ETOLL_SANRAL, null));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(expenseMapper.toDTO(any(Expense.class))).thenReturn(new ExpenseDTO());
        
        expenseService.create(request, organizationId, userId);
        
        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        
        Expense savedExpense = expenseCaptor.getValue();
        assertEquals(TaxExpenseClassification.UNCATEGORIZED, savedExpense.getTaxExpenseClassification());
    }

    @Test
    void testRealCreate_OtherFixed_DerivesUncategorized() {
        setupTestData();
        
        when(organizationRepository.findById(organizationId)).thenReturn(java.util.Optional.of(organization));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(vehicleRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));
        
        ExpenseCreateRequest request = new ExpenseCreateRequest();
        request.setVehicleId(vehicleId);
        request.setCategory(ExpenseCategory.OTHER_FIXED);
        request.setAmountZar(BigDecimal.valueOf(50.00));
        request.setExpenseDate(LocalDate.now());
        request.setSupplierName("Test Supplier");
        
        when(expenseMapper.toEntity(request)).thenReturn(createMockExpense(ExpenseCategory.OTHER_FIXED, null));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(expenseMapper.toDTO(any(Expense.class))).thenReturn(new ExpenseDTO());
        
        expenseService.create(request, organizationId, userId);
        
        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        
        Expense savedExpense = expenseCaptor.getValue();
        assertEquals(TaxExpenseClassification.UNCATEGORIZED, savedExpense.getTaxExpenseClassification());
    }

    // ==================== Real Update Flow Tests ====================

    @Test
    void testRealUpdate_ExplicitRequestClassification_Wins() {
        setupTestData();
        
        UUID expenseId = UUID.randomUUID();
        Expense existingExpense = createMockExpense(ExpenseCategory.FUEL_LOG, TaxExpenseClassification.UNCATEGORIZED);
        existingExpense.setId(expenseId);
        existingExpense.setVehicle(vehicle);
        
        ExpenseUpdateRequest request = new ExpenseUpdateRequest();
        request.setTaxExpenseClassification(TaxExpenseClassification.QUALIFYING_CURRENT_EXPENSE);
        request.setCategory(ExpenseCategory.FUEL_LOG);
        
        when(expenseRepository.findById(expenseId)).thenReturn(java.util.Optional.of(existingExpense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(expenseMapper.toDTO(any(Expense.class))).thenReturn(new ExpenseDTO());
        doNothing().when(expenseMapper).updateEntity(any(Expense.class), any(ExpenseUpdateRequest.class));
        doNothing().when(vehicleService).recalculateOperationalOdometers(any(java.util.Set.class));
        
        expenseService.update(expenseId, request);
        
        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        
        Expense savedExpense = expenseCaptor.getValue();
        assertEquals(TaxExpenseClassification.QUALIFYING_CURRENT_EXPENSE, savedExpense.getTaxExpenseClassification());
    }

    @Test
    void testRealUpdate_ExistingNonNullClassification_Preserved() {
        setupTestData();
        
        UUID expenseId = UUID.randomUUID();
        Expense existingExpense = createMockExpense(ExpenseCategory.FUEL_LOG, TaxExpenseClassification.QUALIFYING_CURRENT_EXPENSE);
        existingExpense.setId(expenseId);
        existingExpense.setVehicle(vehicle);
        
        ExpenseUpdateRequest request = new ExpenseUpdateRequest();
        request.setTaxExpenseClassification(null);
        request.setCategory(ExpenseCategory.FUEL_LOG);
        
        when(expenseRepository.findById(expenseId)).thenReturn(java.util.Optional.of(existingExpense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(expenseMapper.toDTO(any(Expense.class))).thenReturn(new ExpenseDTO());
        doNothing().when(expenseMapper).updateEntity(any(Expense.class), any(ExpenseUpdateRequest.class));
        doNothing().when(vehicleService).recalculateOperationalOdometers(any(java.util.Set.class));
        
        expenseService.update(expenseId, request);
        
        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        
        Expense savedExpense = expenseCaptor.getValue();
        assertEquals(TaxExpenseClassification.QUALIFYING_CURRENT_EXPENSE, savedExpense.getTaxExpenseClassification());
    }

    @Test
    void testRealUpdate_CategoryChange_DoesNotOverwriteExistingClassification() {
        setupTestData();
        
        UUID expenseId = UUID.randomUUID();
        Expense existingExpense = createMockExpense(ExpenseCategory.CAR_WASH, TaxExpenseClassification.QUALIFYING_CURRENT_EXPENSE);
        existingExpense.setId(expenseId);
        existingExpense.setVehicle(vehicle);
        
        ExpenseUpdateRequest request = new ExpenseUpdateRequest();
        request.setTaxExpenseClassification(null);
        request.setCategory(ExpenseCategory.ROADWORTHY);
        
        when(expenseRepository.findById(expenseId)).thenReturn(java.util.Optional.of(existingExpense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(expenseMapper.toDTO(any(Expense.class))).thenReturn(new ExpenseDTO());
        doAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            expense.setCategory(request.getCategory());
            return null;
        }).when(expenseMapper).updateEntity(any(Expense.class), any(ExpenseUpdateRequest.class));
        doNothing().when(vehicleService).recalculateOperationalOdometers(any(java.util.Set.class));
        
        expenseService.update(expenseId, request);
        
        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        
        Expense savedExpense = expenseCaptor.getValue();
        assertEquals(TaxExpenseClassification.QUALIFYING_CURRENT_EXPENSE, savedExpense.getTaxExpenseClassification());
        assertEquals(ExpenseCategory.ROADWORTHY, savedExpense.getCategory());
    }

    @Test
    void testRealUpdate_NullExistingClassification_DerivesFromRequestCategory() {
        setupTestData();
        
        UUID expenseId = UUID.randomUUID();
        Expense existingExpense = createMockExpense(ExpenseCategory.FUEL_LOG, null);
        existingExpense.setId(expenseId);
        existingExpense.setVehicle(vehicle);
        
        ExpenseUpdateRequest request = new ExpenseUpdateRequest();
        request.setTaxExpenseClassification(null);
        request.setCategory(ExpenseCategory.PERSONAL_LICENSE);
        
        when(expenseRepository.findById(expenseId)).thenReturn(java.util.Optional.of(existingExpense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(expenseMapper.toDTO(any(Expense.class))).thenReturn(new ExpenseDTO());
        doNothing().when(expenseMapper).updateEntity(any(Expense.class), any(ExpenseUpdateRequest.class));
        doNothing().when(vehicleService).recalculateOperationalOdometers(any(java.util.Set.class));
        
        expenseService.update(expenseId, request);
        
        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        
        Expense savedExpense = expenseCaptor.getValue();
        assertEquals(TaxExpenseClassification.PERSONAL_OR_NON_QUALIFYING, savedExpense.getTaxExpenseClassification());
    }

    // ==================== Helper Methods ====================

    private void setupTestData() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        
        organization = Organization.builder()
                .id(organizationId)
                .name("Test Org")
                .mode(za.co.fleetexpense.entity.enums.OrganizationMode.SOLO)
                .build();
        
        user = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("hashed")
                .role(za.co.fleetexpense.entity.enums.UserRole.ADMIN)
                .organization(organization)
                .build();
        
        vehicle = Vehicle.builder()
                .id(vehicleId)
                .organization(organization)
                .registrationNumber("ABC123")
                .make("Toyota")
                .model("Corolla")
                .year(2020)
                .fuelType(za.co.fleetexpense.entity.enums.FuelType.PETROL_UNLEADED_95)
                .build();
    }

    private Expense createMockExpense(ExpenseCategory category, TaxExpenseClassification classification) {
        return Expense.builder()
                .category(category)
                .taxExpenseClassification(classification)
                .amountZar(BigDecimal.valueOf(50.00))
                .expenseDate(LocalDate.now())
                .odometerReading(10000)
                .vehicle(vehicle)
                .build();
    }

    /**
     * Helper method to invoke private deriveTaxClassification using reflection
     */
    private TaxExpenseClassification deriveTaxClassification(ExpenseCategory category) throws Exception {
        Method method = ExpenseService.class.getDeclaredMethod("deriveTaxClassification", ExpenseCategory.class);
        method.setAccessible(true);
        return (TaxExpenseClassification) method.invoke(expenseService, category);
    }
}
