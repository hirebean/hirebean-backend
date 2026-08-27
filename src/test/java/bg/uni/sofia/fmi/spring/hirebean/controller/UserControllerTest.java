package bg.uni.sofia.fmi.spring.hirebean.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.ChangePasswordRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.UpdateProfileRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.UserProfileResponse;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.UserResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.RoleType;
import bg.uni.sofia.fmi.spring.hirebean.security.JwtAuthenticationFilter;
import bg.uni.sofia.fmi.spring.hirebean.security.OwnershipAuthorizationService;
import bg.uni.sofia.fmi.spring.hirebean.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class UserControllerTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "test@test.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean(name = "ownership")
    private OwnershipAuthorizationService ownership;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_admin_returnsPagedUsers() throws Exception {
        when(userService.getAllUsers(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(userResponse())));

        mockMvc.perform(get("/api/users").param("search", "foo").param("role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value(EMAIL))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(userService).getAllUsers(eq("foo"), eq(RoleType.CANDIDATE), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void getAllUsers_nonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());

        verify(userService, never()).getAllUsers(any(), any(), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_unknownRoleValue_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/users").param("role", "NOT_A_ROLE")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getUserById_ownershipAllows_returnsUser() throws Exception {
        when(ownership.isSelfOrAdmin(any(), eq(USER_ID))).thenReturn(true);
        when(userService.getUserById(USER_ID)).thenReturn(userResponse());

        mockMvc.perform(get("/api/users/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    @WithMockUser
    void getUserById_ownershipDenies_returnsForbidden() throws Exception {
        when(ownership.isSelfOrAdmin(any(), eq(USER_ID))).thenReturn(false);

        mockMvc.perform(get("/api/users/{id}", USER_ID)).andExpect(status().isForbidden());

        verify(userService, never()).getUserById(any());
    }

    @Test
    @WithMockUser
    void getUserById_nonNumericId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/users/{id}", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getUserById_serviceThrowsNotFound_returnsNotFound() throws Exception {
        when(ownership.isSelfOrAdmin(any(), eq(USER_ID))).thenReturn(true);
        when(userService.getUserById(USER_ID)).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/{id}", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/users/1"));
    }

    @Test
    @WithMockUser
    void getUserProfile_ownershipAllows_returnsProfile() throws Exception {
        when(ownership.isSelfOrAdmin(any(), eq(USER_ID))).thenReturn(true);
        when(userService.getProfile(USER_ID)).thenReturn(profileResponse());

        mockMvc.perform(get("/api/users/{id}/profile", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("test-bio"));
    }

    @Test
    @WithMockUser
    void updateUserProfile_ownershipAllows_returnsUpdatedProfile() throws Exception {
        when(ownership.isSelfOrAdmin(any(), eq(USER_ID))).thenReturn(true);
        when(userService.updateProfile(eq(USER_ID), any(UpdateProfileRequest.class)))
                .thenReturn(profileResponse());

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("baz");

        mockMvc.perform(patch("/api/users/{id}/profile", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void updateUserProfile_ownershipDenies_returnsForbidden() throws Exception {
        when(ownership.isSelfOrAdmin(any(), eq(USER_ID))).thenReturn(false);

        mockMvc.perform(patch("/api/users/{id}/profile", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void updateUserProfile_malformedJson_returnsBadRequest() throws Exception {
        when(ownership.isSelfOrAdmin(any(), eq(USER_ID))).thenReturn(true);

        String invalidRequestBody = "{\"firstName\":";
        mockMvc.perform(patch("/api/users/{id}/profile", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void uploadProfilePicture_ownershipAllows_returnsProfile() throws Exception {
        when(ownership.isSelfOrAdmin(any(), eq(USER_ID))).thenReturn(true);
        when(userService.uploadProfilePicture(eq(USER_ID), any())).thenReturn(profileResponse());

        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "bytes".getBytes());

        mockMvc.perform(multipart("/api/users/{id}/profile-picture", USER_ID)
                        .file(file)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void uploadResume_ownershipAllows_returnsProfile() throws Exception {
        when(ownership.isSelfOrAdmin(any(), eq(USER_ID))).thenReturn(true);
        when(userService.uploadResume(eq(USER_ID), any())).thenReturn(profileResponse());

        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "bytes".getBytes());

        mockMvc.perform(multipart("/api/users/{id}/resume", USER_ID).file(file).with(request -> {
                    request.setMethod("PATCH");
                    return request;
                }))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void requestPasswordReset_anyEmail_returnsOk() throws Exception {
        mockMvc.perform(post("/api/users/password/reset-request").param("email", EMAIL))
                .andExpect(status().isOk());

        verify(userService).requestPasswordReset(EMAIL);
    }

    @Test
    @WithAnonymousUser
    void requestPasswordReset_missingEmailParam_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/users/password/reset-request")).andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void confirmResetToken_anyToken_redirectsToFrontend() throws Exception {
        mockMvc.perform(get("/api/users/password/reset-confirm").param("token", "test-token"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:3000/reset-password?token=test-token"));
    }

    @Test
    @WithAnonymousUser
    void resetPassword_validRequest_returnsOk() throws Exception {
        mockMvc.perform(post("/api/users/password/reset")
                        .param("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest("password", "password"))))
                .andExpect(status().isOk());

        verify(userService).resetPassword(eq("test-token"), any(ChangePasswordRequest.class));
    }

    @Test
    @WithAnonymousUser
    void resetPassword_passwordTooShort_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/users/password/reset")
                        .param("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest("short", "short"))))
                .andExpect(status().isBadRequest());

        verify(userService, never()).resetPassword(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_admin_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", USER_ID)).andExpect(status().isNoContent());

        verify(userService).deleteUserById(USER_ID);
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void deleteUser_nonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", USER_ID)).andExpect(status().isForbidden());

        verify(userService, never()).deleteUserById(any());
    }

    private UserResponse userResponse() {
        return UserResponse.builder()
                .id(USER_ID)
                .email(EMAIL)
                .firstName("foo")
                .lastName("bar")
                .role(RoleType.CANDIDATE.name())
                .build();
    }

    private UserProfileResponse profileResponse() {
        return UserProfileResponse.builder()
                .id(USER_ID)
                .email(EMAIL)
                .firstName("foo")
                .lastName("bar")
                .bio("test-bio")
                .build();
    }

    private ChangePasswordRequest changePasswordRequest(String newPassword, String confirmPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword(newPassword);
        request.setConfirmPassword(confirmPassword);
        return request;
    }
}
