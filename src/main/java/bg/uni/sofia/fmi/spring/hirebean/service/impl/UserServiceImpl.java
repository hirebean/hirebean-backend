package bg.uni.sofia.fmi.spring.hirebean.service.impl;

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
import bg.uni.sofia.fmi.spring.hirebean.service.UserService;
import jakarta.persistence.criteria.Join;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final StorageService storageService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    private String resolvePrimaryRole(Set<Role> roles) {
        return roles.stream()
                .map(role -> role.getName().name())
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(resolvePrimaryRole(user.getRoles()))
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .build();
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        CandidateProfile profile = user.getCandidateProfile();
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(profile != null ? profile.getBio() : null)
                .linkedinUrl(profile != null ? profile.getLinkedinUrl() : null)
                .githubUrl(profile != null ? profile.getGithubUrl() : null)
                .jobTitle(profile != null ? profile.getJobTitle() : null)
                .resumeUrl(profile != null ? storageService.getPresignedUrl(profile.getResumeUrl()) : null)
                .profilePictureUrl(profile != null ? storageService.getPublicUrl(profile.getProfilePictureUrl()) : null)
                .build();
    }

    private CandidateProfile getOrCreateProfile(User user) {
        if (user.getCandidateProfile() == null) {
            CandidateProfile profile = CandidateProfile.builder().user(user).build();
            user.setCandidateProfile(profile);
        }
        return user.getCandidateProfile();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String search, RoleType role, Pageable pageable) {
        return userRepository
                .findAll(buildSpecification(search, role), pageable)
                .map(this::mapToResponse);
    }

    private Specification<User> buildSpecification(String search, RoleType role) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), pattern),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), pattern)));
            }

            if (role != null) {
                query.distinct(true);
                Join<User, Role> rolesJoin = root.join("roles");
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(rolesJoin.get("name"), role));
            }

            return predicate;
        };
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));

        return mapToResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " not found"));
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        CandidateProfile profile = getOrCreateProfile(user);

        if (profile == null) {
            profile = new CandidateProfile();
            profile.setUser(user);
            user.setCandidateProfile(profile);
        }

        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getLinkedInUrl() != null) {
            profile.setLinkedinUrl(request.getLinkedInUrl());
        }
        if (request.getGithubUrl() != null) {
            profile.setGithubUrl(request.getGithubUrl());
        }
        if (request.getJobTitle() != null) {
            profile.setJobTitle(request.getJobTitle());
        }
        userRepository.save(user);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse uploadProfilePicture(Long userId, MultipartFile picture) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));
        // Upload to public Supabase Storage under the "profile-pictures/" folder
        String key = storageService.uploadFile(picture, "profile-pictures");
        CandidateProfile profile = getOrCreateProfile(user);
        profile.setProfilePictureUrl(key);

        userRepository.save(user);
        auditLogService.record("UPLOAD", "UserProfile", userId, "Uploaded profile picture", LogSeverity.INFO);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse uploadResume(Long userId, MultipartFile resume) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));

        String key = storageService.uploadFile(resume, "resumes");
        CandidateProfile profile = getOrCreateProfile(user);
        profile.setResumeUrl(key);

        userRepository.save(user);
        auditLogService.record("UPLOAD", "UserProfile", userId, "Uploaded resume", LogSeverity.INFO);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        // Delete old tokens for this user
        passwordResetTokenRepository.deleteAllByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user.getEmail(), token);
        auditLogService.record(
                "PASSWORD_RESET_REQUEST",
                "User",
                user.getId(),
                user.getId(),
                "Password reset requested",
                LogSeverity.INFO);
    }

    @Override
    @Transactional
    public void resetPassword(String token, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new UnauthorizedException("New password and confirm password do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired password reset token"));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new UnauthorizedException("Invalid or expired password reset token");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        auditLogService.record(
                "PASSWORD_RESET", "User", user.getId(), user.getId(), "Password reset completed", LogSeverity.INFO);
    }

    @Override
    public void deleteUserById(Long id) {

        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
        userRepository.delete(user);
        auditLogService.record("DELETE", "User", id, "Deleted user", LogSeverity.WARN);
    }
}
