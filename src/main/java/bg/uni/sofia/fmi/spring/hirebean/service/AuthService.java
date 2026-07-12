package bg.uni.sofia.fmi.spring.hirebean.service;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.LoginRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.RegisterRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String authorizationHeader);
}
