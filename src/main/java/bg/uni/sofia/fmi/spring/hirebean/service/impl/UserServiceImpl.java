package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.ChangePasswordRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.UpdateProfileRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.UserProfileResponse;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.UserResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.exception.auth.UnauthorizedException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.CandidateProfile;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.PasswordResetToken;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.repository.PasswordResetTokenRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import bg.uni.sofia.fmi.spring.hirebean.service.EmailService;
import bg.uni.sofia.fmi.spring.hirebean.service.S3Service;
import bg.uni.sofia.fmi.spring.hirebean.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final S3Service s3Service;
    private final PasswordEncoder passwordEncoder;

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
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
                .resumeUrl(profile != null ? s3Service.getPresignedUrl(profile.getResumeUrl()) : null)
                .profilePictureUrl(profile != null ? s3Service.getPublicUrl(profile.getProfilePictureUrl()) : null)
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
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::mapToResponse).toList();
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
        // Upload to S3 under "profile-pictures/" folder - pulic via CDN
        String key = s3Service.uploadFile(picture, "profile-pictures");
        CandidateProfile profile = getOrCreateProfile(user);
        profile.setProfilePictureUrl(key);

        userRepository.save(user);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " not found"));

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
    }

    @Override
    public void deleteUserById(Long id) {

        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
        userRepository.delete(user);
    }
}
