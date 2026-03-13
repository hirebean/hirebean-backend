package bg.uni.sofia.fmi.spring.hirebean.service;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.ChangePasswordRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.UpdateProfileRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.UserProfileResponse;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.UserResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id);

    UserResponse getUserByEmail(String email);

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    UserProfileResponse uploadProfilePicture(Long userId, MultipartFile picture);

    // 1. sends reset email with token, token is valid for 30 minutes
    void requestPasswordReset(String email);

    // 2. validates token and changes password
    void resetPassword(String token, ChangePasswordRequest request);

    void deleteUserById(Long id);
}
