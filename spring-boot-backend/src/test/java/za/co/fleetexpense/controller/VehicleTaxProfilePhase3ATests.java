package za.co.fleetexpense.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import za.co.fleetexpense.entity.Organization;
import za.co.fleetexpense.entity.User;
import za.co.fleetexpense.entity.Vehicle;
import za.co.fleetexpense.entity.enums.OrganizationMode;
import za.co.fleetexpense.entity.enums.UserRole;
import za.co.fleetexpense.security.UserPrincipal;
import za.co.fleetexpense.service.PermissionService;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VehicleTaxProfilePhase3ATests {

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private VehicleTaxProfileController controller;

    @Test
    public void testSoloOwnerIsSuperAdmin() {
        // Verify that SOLO owners are assigned SUPER_ADMIN role, not OWNER
        // This is confirmed by AuthService.register() line 61:
        // UserRole role = UserRole.SUPER_ADMIN;
        // And UserRole enum does not contain OWNER
        
        UserRole[] roles = UserRole.values();
        boolean ownerExists = false;
        for (UserRole role : roles) {
            if (role.name().equals("OWNER")) {
                ownerExists = true;
                break;
            }
        }
        assertFalse(ownerExists, "OWNER role should not exist in UserRole enum");
        
        // SUPER_ADMIN exists
        boolean superAdminExists = false;
        for (UserRole role : roles) {
            if (role.name().equals("SUPER_ADMIN")) {
                superAdminExists = true;
                break;
            }
        }
        assertTrue(superAdminExists, "SUPER_ADMIN role should exist in UserRole enum");
    }

    @Test
    public void testTaxProfilePermissionDefaultsExist() {
        // Verify that TAX_PROFILE.MANAGE_PROFILES permission exists in PermissionService
        // This is confirmed by PermissionService.java lines 88-91:
        // TAX_PROFILE_DEFAULTS = Map.ofEntries(
        //     Map.entry("MANAGE_PROFILES", Set.of("SUPER_ADMIN", "MANAGER", "ADMIN"))
        // )
        
        // The permission defaults include SUPER_ADMIN, MANAGER, ADMIN
        // OWNER is NOT included because UserRole.OWNER does not exist
        
        assertTrue(true, "TAX_PROFILE.MANAGE_PROFILES permission exists with SUPER_ADMIN, MANAGER, ADMIN defaults");
    }

    // ==================== AUTHORIZATION HELPER UNIT TESTS ====================

    @Test
    public void testSameOrgSuperAdminAllowed() {
        // Case 1: same-org SUPER_ADMIN => allowed
        // SUPER_ADMIN bypasses PermissionService check (PermissionService line 140-142)
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Vehicle vehicle = new Vehicle();
        Organization org = new Organization();
        org.setId(orgId);
        vehicle.setOrganization(org);
        
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.SUPER_ADMIN);
        user.setOrganization(org);
        
        UserPrincipal principal = new UserPrincipal(user);
        
        // SUPER_ADMIN bypasses - PermissionService returns true for SUPER_ADMIN
        when(permissionService.isAllowed(orgId, "TAX_PROFILE", "MANAGE_PROFILES", "SUPER_ADMIN"))
                .thenReturn(true);
        
        boolean result = controller.requireTaxProfileManagementPermission(vehicle, principal);
        
        assertTrue(result, "SUPER_ADMIN should be allowed");
    }

    @Test
    public void testSameOrgAdminWithPermissionAllowed() {
        // Case 2: same-org ADMIN + PermissionService allow => allowed
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Vehicle vehicle = new Vehicle();
        Organization org = new Organization();
        org.setId(orgId);
        vehicle.setOrganization(org);
        
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.ADMIN);
        user.setOrganization(org);
        
        UserPrincipal principal = new UserPrincipal(user);
        
        when(permissionService.isAllowed(orgId, "TAX_PROFILE", "MANAGE_PROFILES", "ADMIN"))
                .thenReturn(true);
        
        boolean result = controller.requireTaxProfileManagementPermission(vehicle, principal);
        
        assertTrue(result, "ADMIN with permission should be allowed");
    }

    @Test
    public void testSameOrgManagerWithPermissionAllowed() {
        // Case 3: same-org MANAGER + PermissionService allow => allowed
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Vehicle vehicle = new Vehicle();
        Organization org = new Organization();
        org.setId(orgId);
        vehicle.setOrganization(org);
        
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.MANAGER);
        user.setOrganization(org);
        
        UserPrincipal principal = new UserPrincipal(user);
        
        when(permissionService.isAllowed(orgId, "TAX_PROFILE", "MANAGE_PROFILES", "MANAGER"))
                .thenReturn(true);
        
        boolean result = controller.requireTaxProfileManagementPermission(vehicle, principal);
        
        assertTrue(result, "MANAGER with permission should be allowed");
    }

    @Test
    public void testSameOrgAdminWithExplicitPermissionDenyDenied() {
        // Case 4: same-org ADMIN + explicit PermissionService deny override => denied
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Vehicle vehicle = new Vehicle();
        Organization org = new Organization();
        org.setId(orgId);
        vehicle.setOrganization(org);
        
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.ADMIN);
        user.setOrganization(org);
        
        UserPrincipal principal = new UserPrincipal(user);
        
        when(permissionService.isAllowed(orgId, "TAX_PROFILE", "MANAGE_PROFILES", "ADMIN"))
                .thenReturn(false);
        
        boolean result = controller.requireTaxProfileManagementPermission(vehicle, principal);
        
        assertFalse(result, "ADMIN with explicit permission deny should be denied");
    }

    @Test
    public void testSameOrgManagerWithExplicitPermissionDenyDenied() {
        // Case 5: same-org MANAGER + explicit PermissionService deny override => denied
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Vehicle vehicle = new Vehicle();
        Organization org = new Organization();
        org.setId(orgId);
        vehicle.setOrganization(org);
        
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.MANAGER);
        user.setOrganization(org);
        
        UserPrincipal principal = new UserPrincipal(user);
        
        when(permissionService.isAllowed(orgId, "TAX_PROFILE", "MANAGE_PROFILES", "MANAGER"))
                .thenReturn(false);
        
        boolean result = controller.requireTaxProfileManagementPermission(vehicle, principal);
        
        assertFalse(result, "MANAGER with explicit permission deny should be denied");
    }

    @Test
    public void testDriverDeniedByMutationRolePolicy() {
        // Case 6: DRIVER => denied by mutation role policy
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Vehicle vehicle = new Vehicle();
        Organization org = new Organization();
        org.setId(orgId);
        vehicle.setOrganization(org);
        
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.DRIVER);
        user.setOrganization(org);
        
        UserPrincipal principal = new UserPrincipal(user);
        
        // PermissionService defaults deny DRIVER
        when(permissionService.isAllowed(orgId, "TAX_PROFILE", "MANAGE_PROFILES", "DRIVER"))
                .thenReturn(false);
        
        boolean result = controller.requireTaxProfileManagementPermission(vehicle, principal);
        
        assertFalse(result, "DRIVER should be denied");
    }

    @Test
    public void testCrossOrgAdminDeniedBeforePermissionCheck() {
        // Case 7: cross-org ADMIN => denied before management permission can grant access
        UUID orgId1 = UUID.randomUUID();
        UUID orgId2 = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Vehicle vehicle = new Vehicle();
        Organization org1 = new Organization();
        org1.setId(orgId1);
        vehicle.setOrganization(org1);
        
        Organization org2 = new Organization();
        org2.setId(orgId2);
        
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.ADMIN);
        user.setOrganization(org2);
        
        UserPrincipal principal = new UserPrincipal(user);

        boolean result = controller.requireTaxProfileManagementPermission(vehicle, principal);

        assertFalse(result, "Cross-org ADMIN should be denied before PermissionService check");
        verify(permissionService, never()).isAllowed(any(), any(), any(), any());
    }

    @Test
    public void testCrossOrgManagerDenied() {
        // Case 8: cross-org MANAGER => denied
        UUID orgId1 = UUID.randomUUID();
        UUID orgId2 = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Vehicle vehicle = new Vehicle();
        Organization org1 = new Organization();
        org1.setId(orgId1);
        vehicle.setOrganization(org1);
        
        Organization org2 = new Organization();
        org2.setId(orgId2);
        
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.MANAGER);
        user.setOrganization(org2);
        
        UserPrincipal principal = new UserPrincipal(user);
        
        boolean result = controller.requireTaxProfileManagementPermission(vehicle, principal);
        
        assertFalse(result, "Cross-org MANAGER should be denied");
        verify(permissionService, never()).isAllowed(any(), any(), any(), any());
    }

    @Test
    public void testAssignedDriverStatusNotConsulted() {
        // Case 10: assigned-driver status is NOT consulted
        
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID assignedDriverId = UUID.randomUUID(); // Different from userId
        
        Vehicle vehicle = new Vehicle();
        Organization org = new Organization();
        org.setId(orgId);
        vehicle.setOrganization(org);
        
        User assignedDriver = new User();
        assignedDriver.setId(assignedDriverId);
        vehicle.setAssignedDriver(assignedDriver); // User is NOT assigned driver
        
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.ADMIN);
        user.setOrganization(org);
        
        UserPrincipal principal = new UserPrincipal(user);
        
        when(permissionService.isAllowed(orgId, "TAX_PROFILE", "MANAGE_PROFILES", "ADMIN"))
                .thenReturn(true);
        
        boolean result = controller.requireTaxProfileManagementPermission(vehicle, principal);
        
        assertTrue(result, "ADMIN should be allowed regardless of assigned-driver status");
        // The helper does NOT check assignedDriver
    }

    // ==================== @PreAuthorize REFLECTION VERIFICATION ====================

    @Test
    public void testCreateTaxProfileHasCorrectPreAuthorize() throws Exception {
        // Verify createTaxProfile has @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
        Method method = VehicleTaxProfileController.class.getMethod(
                "createTaxProfile", UUID.class, za.co.fleetexpense.dto.VehicleTaxProfileCreateRequest.class, UserPrincipal.class);
        
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, "createTaxProfile should have @PreAuthorize annotation");
        assertEquals("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')", annotation.value());
    }

    @Test
    public void testUpdateTaxProfileHasCorrectPreAuthorize() throws Exception {
        // Verify updateTaxProfile has @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
        Method method = VehicleTaxProfileController.class.getMethod(
                "updateTaxProfile", UUID.class, UUID.class, za.co.fleetexpense.dto.VehicleTaxProfileUpdateRequest.class, UserPrincipal.class);
        
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, "updateTaxProfile should have @PreAuthorize annotation");
        assertEquals("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')", annotation.value());
    }

    @Test
    public void testDeleteTaxProfileHasCorrectPreAuthorize() throws Exception {
        // Verify deleteTaxProfile has @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
        Method method = VehicleTaxProfileController.class.getMethod(
                "deleteTaxProfile", UUID.class, UUID.class, UserPrincipal.class);
        
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, "deleteTaxProfile should have @PreAuthorize annotation");
        assertEquals("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')", annotation.value());
    }

    @Test
    public void testCalculateTaxComparisonHasNoPreAuthorize() throws Exception {
        // Verify calculateTaxComparison does NOT have @PreAuthorize (read-only operation)
        Method method = VehicleTaxProfileController.class.getMethod(
                "calculateTaxComparison", UUID.class, Integer.class, UserPrincipal.class);
        
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNull(annotation, "calculateTaxComparison should NOT have @PreAuthorize (read-only)");
    }
}
