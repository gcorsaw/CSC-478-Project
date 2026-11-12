package csc478project.restaurantapp;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "restaurant.auth.accounts.manager.username=test-manager",
    "restaurant.auth.accounts.manager.password=test-password"
})
class SecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StaffAccountService staffAccountService;

    @Test
    void allowsCsrfBootstrapWithoutSigningIn() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk());
    }

    @Test
    void exposesOwnerSetupAvailabilityWithoutSigningIn() throws Exception {
        mockMvc.perform(get("/api/auth/owner-setup-available"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void allowsFirstOwnerSetupWithoutAnExistingSession() throws Exception {
        when(staffAccountService.createFirstOwner(
            "First Owner", "owner", "temporary-owner-password"
        )).thenReturn(Map.of("username", "owner", "role", "OWNER"));

        mockMvc.perform(post("/api/auth/owner-setup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"First Owner\",\"username\":\"owner\","
                    + "\"password\":\"temporary-owner-password\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void rejectsAnonymousApiRequests() throws Exception {
        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void redirectsProtectedPagesToSignIn() throws Exception {
        mockMvc.perform(get("/manager.html"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login.html"));
    }

    @Test
    void acceptsConfiguredUsernameAndPassword() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "test-manager")
                .param("password", "test-password")
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void allowsManagerRolePastDashboardSummaryAuthorization() throws Exception {
        mockMvc.perform(get("/api/summary").with(user("test-manager").roles("MANAGER")))
            .andExpect(status().isNotFound());
    }

    @Test
    void rejectsStaffRoleFromAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/health").with(user("test-manager").roles("MANAGER")))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error")
                .value("Your account does not have permission to access this resource."));
    }

    @Test
    void permitsAdminRolePastAdminApiAuthorization() throws Exception {
        mockMvc.perform(get("/api/admin/health").with(user("test-admin").roles("ADMIN")))
            .andExpect(status().isNotFound());
    }

    @Test
    void deniesStaffAccountCreationToNonManagers() throws Exception {
        mockMvc.perform(get("/api/staff/accounts").with(user("test-chef").roles("CHEF")))
            .andExpect(status().isForbidden());
    }

    @Test
    void allowsManagersPastStaffAccountAuthorization() throws Exception {
        mockMvc.perform(get("/api/staff/accounts").with(user("test-manager").roles("MANAGER")))
            .andExpect(status().isNotFound());
    }

    @Test
    void allowsSignupWithoutAnExistingSession() throws Exception {
        when(staffAccountService.signUp(
            "Jamie Chef", "jamie-chef", "a-long-password", "CHEF"
        )).thenReturn(Map.of("username", "jamie-chef", "role", "CHEF"));

        mockMvc.perform(post("/api/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Jamie Chef\",\"username\":\"jamie-chef\","
                    + "\"password\":\"a-long-password\",\"role\":\"CHEF\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("jamie-chef"));
    }
}