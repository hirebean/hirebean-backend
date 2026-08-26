package bg.uni.sofia.fmi.spring.hirebean.controller;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.ChangePasswordRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.UpdateProfileRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.UserProfileResponse;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.UserResponse;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.RoleType;
import bg.uni.sofia.fmi.spring.hirebean.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RoleType role,
            Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(search, role, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ownership.isSelfOrAdmin(authentication, #id)")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/{id}/profile")
    @PreAuthorize("@ownership.isSelfOrAdmin(authentication, #id)")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getProfile(id));
    }

    @PatchMapping("/{id}/profile")
    @PreAuthorize("@ownership.isSelfOrAdmin(authentication, #id)")
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            @PathVariable Long id, @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(id, request));
    }

    @PatchMapping(value = "/{id}/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ownership.isSelfOrAdmin(authentication, #id)")
    public ResponseEntity<UserProfileResponse> uploadProfilePicture(
            @PathVariable Long id, @RequestPart("file") MultipartFile picture) {
        return ResponseEntity.ok(userService.uploadProfilePicture(id, picture));
    }

    @PatchMapping(value = "/{id}/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ownership.isSelfOrAdmin(authentication, #id)")
    public ResponseEntity<UserProfileResponse> uploadResume(
            @PathVariable Long id, @RequestPart("file") MultipartFile resume) {
        return ResponseEntity.ok(userService.uploadResume(id, resume));
    }

    // Step 1: user submits their email → backend sends reset email
    @PostMapping("/password/reset-request")
    public ResponseEntity<Void> requestPasswordReset(@RequestParam String email) {
        userService.requestPasswordReset(email);
        return ResponseEntity.ok().build();
    }

    // Step 2 user clicks on the email link -> backend validates token and
    // redirects to frontend reset-password page with token
    // frontend shows the enter new password form
    @GetMapping("/password/reset-confirm")
    public ResponseEntity<Void> confirmResetToken(@RequestParam String token) {
        URI redirectUri = URI.create(frontendUrl + "/reset-password?token=" + token);
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }

    // Step 3: frontend submits new password + token → backend changes password
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(
            @RequestParam String token, @Valid @RequestBody ChangePasswordRequest request) {
        userService.resetPassword(token, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }
}
