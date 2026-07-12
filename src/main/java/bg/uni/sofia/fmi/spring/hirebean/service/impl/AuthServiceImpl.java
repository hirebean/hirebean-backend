package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.LoginRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.RegisterRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.AuthResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.BusinessException;
import bg.uni.sofia.fmi.spring.hirebean.exception.auth.UnauthorizedException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Company;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.RevokedToken;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Role;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.RoleType;
import bg.uni.sofia.fmi.spring.hirebean.repository.CompanyRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.RevokedTokenRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.RoleRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import bg.uni.sofia.fmi.spring.hirebean.security.JwtUtil;
import bg.uni.sofia.fmi.spring.hirebean.security.UserDetailsServiceImpl;
import bg.uni.sofia.fmi.spring.hirebean.service.AuditLogService;
import bg.uni.sofia.fmi.spring.hirebean.service.AuthService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
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
    private final CompanyRepository companyRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuditLogService auditLogService;

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

        RoleType requestedRole = request.getRole() == null ? RoleType.CANDIDATE : request.getRole();
        if (requestedRole == RoleType.ADMIN) {
            throw new BusinessException("Admin users cannot self-register", HttpStatus.BAD_REQUEST);
        }

        Role role = roleRepository
                .findByName(requestedRole)
                .orElseThrow(() -> new BusinessException(
                        "Role " + requestedRole + " not found", HttpStatus.INTERNAL_SERVER_ERROR));

        Company company = null;
        if (request.getCompanyId() != null) {
            company = companyRepository
                    .findById(request.getCompanyId())
                    .orElseThrow(() -> new BusinessException(
                            "Company not found with id: " + request.getCompanyId(), HttpStatus.NOT_FOUND));
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(Set.of(role))
                .company(company)
                .build();

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        auditLogService.record("REGISTER", "User", user.getId(), "User registered as " + requestedRole, "INFO");

        return AuthResponse.builder()
                .userId(user.getId())
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(requestedRole.name())
                .companyId(company != null ? company.getId() : null)
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
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

        auditLogService.record("LOGIN", "User", user.getId(), user.getId(), "User logged in", "INFO");

        return AuthResponse.builder()
                .userId(user.getId())
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(primaryRole)
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .build();
    }

    @Override
    @Transactional
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing bearer token");
        }

        String token = authorizationHeader.substring(7);
        if (revokedTokenRepository.existsByToken(token)) {
            return;
        }

        Date expiration = jwtUtil.extractExpiration(token);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
        revokedTokenRepository.save(
                RevokedToken.builder().token(token).expiresAt(expiresAt).build());
        revokedTokenRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());
        auditLogService.record("LOGOUT", "User", null, "User logged out", "INFO");
    }
}
