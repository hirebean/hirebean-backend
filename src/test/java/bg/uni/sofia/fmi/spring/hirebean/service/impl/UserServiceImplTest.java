package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.ChangePasswordRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.UpdateProfileRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.UserProfileResponse;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.UserResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.exception.auth.UnauthorizedException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.CandidateProfile;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.PasswordResetToken;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Role;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.LogSeverity;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.RoleType;
import bg.uni.sofia.fmi.spring.hirebean.repository.PasswordResetTokenRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import bg.uni.sofia.fmi.spring.hirebean.service.AuditLogService;
import bg.uni.sofia.fmi.spring.hirebean.service.EmailService;
import bg.uni.sofia.fmi.spring.hirebean.service.StorageService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "test@test.com";
    private static final String UNKNOWN_EMAIL = "unknown@test.com";
    private static final String FIRST_NAME = "foo";
    private static final String LAST_NAME = "bar";
    private static final String RAW_PASSWORD = "password";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String OLD_PASSWORD_HASH = "old-password-hash";

    private static final String PICTURE_FOLDER = "profile-pictures";
    private static final String RESUME_FOLDER = "resumes";
    private static final String PICTURE_KEY = "profile-pictures/abc.png";
    private static final String RESUME_KEY = "resumes/abc.pdf";
    private static final String PUBLIC_URL = "https://storage.test/public/abc.png";
    private static final String SIGNED_URL = "https://storage.test/signed/abc.pdf";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private StorageService storageService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private MultipartFile file;

    private final User user = createUser();
    private final ChangePasswordRequest validPasswordRequest = createChangePasswordRequest(RAW_PASSWORD, RAW_PASSWORD);

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository,
                passwordResetTokenRepository,
                emailService,
                storageService,
                passwordEncoder,
                auditLogService);
    }

    @Test
    void getAllUsers_matchingUsers_returnsMappedPage() {
        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<UserResponse> result = userService.getAllUsers(null, null, Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
        UserResponse response = result.getContent().get(0);
        assertThat(response.getId()).isEqualTo(USER_ID);
        assertThat(response.getEmail()).isEqualTo(EMAIL);
        assertThat(response.getRole()).isEqualTo(RoleType.CANDIDATE.name());
        assertThat(response.getCompanyId()).isNull();
    }

    @Test
    void getUserById_existingUser_returnsMappedUser() {
        stubExistingUser();

        UserResponse response = userService.getUserById(USER_ID);

        assertThat(response.getId()).isEqualTo(USER_ID);
        assertThat(response.getEmail()).isEqualTo(EMAIL);
        assertThat(response.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(response.getLastName()).isEqualTo(LAST_NAME);
    }

    @Test
    void getUserById_userDoesNotExist_throwsResourceNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(USER_ID));
    }

    @Test
    void getUserByEmail_existingUser_returnsMappedUser() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserByEmail(EMAIL);

        assertThat(response.getId()).isEqualTo(USER_ID);
        assertThat(response.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void getUserByEmail_unknownEmail_throwsResourceNotFound() {
        when(userRepository.findByEmail(UNKNOWN_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail(UNKNOWN_EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(UNKNOWN_EMAIL);
    }

    @Test
    void getProfile_userWithProfile_returnsProfileWithStorageUrls() {
        stubExistingUser();
        CandidateProfile profile = createProfile("test-bio");
        profile.setProfilePictureUrl(PICTURE_KEY);
        profile.setResumeUrl(RESUME_KEY);
        user.setCandidateProfile(profile);

        when(storageService.getPublicUrl(PICTURE_KEY)).thenReturn(PUBLIC_URL);
        when(storageService.getPresignedUrl(RESUME_KEY)).thenReturn(SIGNED_URL);

        UserProfileResponse response = userService.getProfile(USER_ID);

        assertThat(response.getBio()).isEqualTo("test-bio");
        assertThat(response.getProfilePictureUrl()).isEqualTo(PUBLIC_URL);
        assertThat(response.getResumeUrl()).isEqualTo(SIGNED_URL);
    }

    @Test
    void getProfile_userWithoutProfile_returnsNullProfileFields() {
        stubExistingUser();

        UserProfileResponse response = userService.getProfile(USER_ID);

        assertThat(response.getEmail()).isEqualTo(EMAIL);
        assertThat(response.getBio()).isNull();
        assertThat(response.getResumeUrl()).isNull();
        assertThat(response.getProfilePictureUrl()).isNull();
        verifyNoInteractions(storageService);
    }

    @Test
    void getProfile_userDoesNotExist_throwsResourceNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(USER_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_allFieldsProvided_updatesUserAndProfile() {
        stubExistingUser();
        user.setCandidateProfile(createProfile("old-bio"));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("baz");
        request.setLastName("qux");
        request.setBio("new-bio");
        request.setLinkedInUrl("https://linkedin.test/in/test");
        request.setGithubUrl("https://github.test/test");
        request.setJobTitle("test-title");

        userService.updateProfile(USER_ID, request);

        assertThat(user.getFirstName()).isEqualTo("baz");
        assertThat(user.getLastName()).isEqualTo("qux");
        CandidateProfile profile = user.getCandidateProfile();
        assertThat(profile.getBio()).isEqualTo("new-bio");
        assertThat(profile.getLinkedinUrl()).isEqualTo("https://linkedin.test/in/test");
        assertThat(profile.getGithubUrl()).isEqualTo("https://github.test/test");
        assertThat(profile.getJobTitle()).isEqualTo("test-title");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_omittedFields_leavesThemUnchanged() {
        stubExistingUser();
        user.setCandidateProfile(createProfile("original-bio"));

        String newName = "updatedName";
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName(newName);

        UserProfileResponse response = userService.updateProfile(USER_ID, request);

        assertThat(user.getFirstName()).isEqualTo(newName);
        assertThat(user.getLastName()).isEqualTo(LAST_NAME);
        assertThat(user.getCandidateProfile().getBio()).isEqualTo("original-bio");
        assertThat(response.getFirstName()).isEqualTo(newName);
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_userWithoutProfile_createsProfile() {
        stubExistingUser();

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setBio("first-bio");

        userService.updateProfile(USER_ID, request);

        assertThat(user.getCandidateProfile()).isNotNull();
        assertThat(user.getCandidateProfile().getBio()).isEqualTo("first-bio");
        assertThat(user.getCandidateProfile().getUser()).isSameAs(user);
    }

    @Test
    void updateProfile_userDoesNotExist_throwsResourceNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(USER_ID, new UpdateProfileRequest()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void uploadProfilePicture_existingUser_storesKeyAndAudits() {
        stubExistingUser();
        when(storageService.uploadFile(file, PICTURE_FOLDER)).thenReturn(PICTURE_KEY);
        when(storageService.getPublicUrl(PICTURE_KEY)).thenReturn(PUBLIC_URL);

        UserProfileResponse response = userService.uploadProfilePicture(USER_ID, file);

        assertThat(user.getCandidateProfile().getProfilePictureUrl()).isEqualTo(PICTURE_KEY);
        assertThat(response.getProfilePictureUrl()).isEqualTo(PUBLIC_URL);
        verify(userRepository).save(user);
        verify(auditLogService).record("UPLOAD", "UserProfile", USER_ID, "Uploaded profile picture", LogSeverity.INFO);
    }

    @Test
    void uploadProfilePicture_userDoesNotExist_throwsResourceNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.uploadProfilePicture(USER_ID, file))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void uploadResume_existingUser_storesKeyAndAudits() {
        stubExistingUser();
        when(storageService.uploadFile(file, RESUME_FOLDER)).thenReturn(RESUME_KEY);
        when(storageService.getPresignedUrl(RESUME_KEY)).thenReturn(SIGNED_URL);

        UserProfileResponse response = userService.uploadResume(USER_ID, file);

        assertThat(user.getCandidateProfile().getResumeUrl()).isEqualTo(RESUME_KEY);
        assertThat(response.getResumeUrl()).isEqualTo(SIGNED_URL);
        verify(userRepository).save(user);
        verify(auditLogService).record("UPLOAD", "UserProfile", USER_ID, "Uploaded resume", LogSeverity.INFO);
    }

    @Test
    void uploadResume_userDoesNotExist_throwsResourceNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.uploadResume(USER_ID, file)).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void requestPasswordReset_unknownEmail_doesNothing() {
        when(userRepository.findByEmail(UNKNOWN_EMAIL)).thenReturn(Optional.empty());

        userService.requestPasswordReset(UNKNOWN_EMAIL);

        verifyNoInteractions(passwordResetTokenRepository, emailService, auditLogService);
    }

    @Test
    void requestPasswordReset_existingUser_replacesTokenAndSendsEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        userService.requestPasswordReset(EMAIL);

        verify(passwordResetTokenRepository).deleteAllByUserId(USER_ID);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq(EMAIL), anyString());
        verify(auditLogService)
                .record(
                        "PASSWORD_RESET_REQUEST",
                        "User",
                        USER_ID,
                        USER_ID,
                        "Password reset requested",
                        LogSeverity.INFO);
    }

    @Test
    void resetPassword_mismatchedConfirmation_throwsUnauthorized() {
        ChangePasswordRequest request = createChangePasswordRequest(RAW_PASSWORD, "other-password");

        assertThatThrownBy(() -> userService.resetPassword("any-token", request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("do not match");

        verifyNoInteractions(passwordResetTokenRepository, userRepository);
    }

    @Test
    void resetPassword_unknownToken_throwsUnauthorized() {
        when(passwordResetTokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.resetPassword("missing-token", validPasswordRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid or expired");
    }

    @Test
    void resetPassword_usedToken_throwsUnauthorized() {
        stubResetToken("used-token", true, LocalDateTime.now().plusMinutes(30));

        assertThatThrownBy(() -> userService.resetPassword("used-token", validPasswordRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid or expired");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_expiredToken_throwsUnauthorized() {
        stubResetToken("expired-token", false, LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> userService.resetPassword("expired-token", validPasswordRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid or expired");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_validToken_encodesPasswordAndMarksTokenUsed() {
        PasswordResetToken token =
                stubResetToken("valid-token", false, LocalDateTime.now().plusMinutes(30));
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        userService.resetPassword("valid-token", validPasswordRequest);

        assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
        verify(auditLogService)
                .record("PASSWORD_RESET", "User", USER_ID, USER_ID, "Password reset completed", LogSeverity.INFO);
    }

    @Test
    void deleteUserById_existingUser_deletesAndAuditsAtWarnLevel() {
        stubExistingUser();

        userService.deleteUserById(USER_ID);

        verify(userRepository).delete(user);
        verify(auditLogService).record("DELETE", "User", USER_ID, "Deleted user", LogSeverity.WARN);
    }

    @Test
    void deleteUserById_userDoesNotExist_throwsResourceNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUserById(USER_ID)).isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).delete(any(User.class));
    }

    private User createUser() {
        User created = User.builder()
                .email(EMAIL)
                .password(OLD_PASSWORD_HASH)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .roles(new HashSet<>(Set.of(new Role(1L, RoleType.CANDIDATE))))
                .build();
        created.setId(USER_ID);
        return created;
    }

    private CandidateProfile createProfile(String bio) {
        return CandidateProfile.builder().user(user).bio(bio).build();
    }

    private ChangePasswordRequest createChangePasswordRequest(String newPassword, String confirmPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword(newPassword);
        request.setConfirmPassword(confirmPassword);
        return request;
    }

    private void stubExistingUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    private PasswordResetToken stubResetToken(String token, boolean used, LocalDateTime expiresAt) {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(expiresAt)
                .used(used)
                .build();
        when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));
        return resetToken;
    }
}
