package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.LoginRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.RegisterRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.AuthResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.BusinessException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Role;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.RoleType;
import bg.uni.sofia.fmi.spring.hirebean.repository.RoleRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import bg.uni.sofia.fmi.spring.hirebean.security.JwtUtil;
import bg.uni.sofia.fmi.spring.hirebean.security.UserDetailsServiceImpl;
import bg.uni.sofia.fmi.spring.hirebean.service.AuthService;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    // Priority: ADMIN > EMPLOYER > CANDIDATE
    // Used to return the highest-privilege role in the auth response
    // so the frontend knows what UI to render.
    private static final List<RoleType> ROLE_PRIORITY = List.of(RoleType.ADMIN, RoleType.EMPLOYER, RoleType.CANDIDATE);

    private String resolvePrimaryRole(Set<Role> roles) {
        return roles.stream()
                .map(role -> role.getName())
                .min(Comparator.comparingInt(ROLE_PRIORITY::indexOf))
                .orElse(RoleType.CANDIDATE) // Default to CANDIDATE if no roles found, though this should not happen
                .name();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(
                    "User with email " + request.getEmail() + " already exists", HttpStatus.CONFLICT);
        }

        // Default роля при регистрация е CANDIDATE
        Role candidateRole = roleRepository
                .findByName(RoleType.CANDIDATE)
                .orElseThrow(() -> new BusinessException("Role CANDIDATE not found", HttpStatus.INTERNAL_SERVER_ERROR));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(Set.of(candidateRole))
                .build();

        userRepository.save(user);
        // Генерираме JWT токен веднага след регистрацията
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(RoleType.CANDIDATE.name())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            // AuthenticationManager проверява email + password срещу базата
            // Вътрешно извиква UserDetailsServiceImpl.loadUserByUsername()
            // и сравнява BCrypt хешовете
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        String primaryRole = resolvePrimaryRole(user.getRoles());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(primaryRole)
                .build();
    }
}
