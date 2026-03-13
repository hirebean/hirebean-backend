package bg.uni.sofia.fmi.spring.hirebean.controller;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.ChangePasswordRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.UpdateProfileRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.UserProfileResponse;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.UserResponse;
import bg.uni.sofia.fmi.spring.hirebean.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getProfile(id));
    }

    @PatchMapping("/{id}/profile")
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            @PathVariable Long id, @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(id, request));
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
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }
}
