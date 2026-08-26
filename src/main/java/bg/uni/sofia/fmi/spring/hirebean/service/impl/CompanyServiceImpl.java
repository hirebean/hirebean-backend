package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.CompanyRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.CompanyResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.BusinessException;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.exception.company.CompanyAlreadyExistsException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Company;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.LogSeverity;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.RoleType;
import bg.uni.sofia.fmi.spring.hirebean.repository.CompanyRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import bg.uni.sofia.fmi.spring.hirebean.service.AuditLogService;
import bg.uni.sofia.fmi.spring.hirebean.service.CompanyService;
import bg.uni.sofia.fmi.spring.hirebean.service.StorageService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final StorageService storageService;

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    private CompanyResponse mapToResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .description(company.getDescription())
                .websiteUrl(company.getWebsiteUrl())
                .logoUrl(storageService.getPublicUrl(company.getLogoUrl()))
                .location(company.getLocation())
                .createdAt(company.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public CompanyResponse getCompanyById(Long id) {
        Company company = companyRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
        return mapToResponse(company);
    }

    @Override
    @Transactional
    public CompanyResponse createCompany(CompanyRequest request) {
        if (companyRepository.findByName(request.getName()).isPresent()) {
            throw new CompanyAlreadyExistsException("Company with name '" + request.getName() + "' already exists.");
        }
        Company company = Company.builder()
                .name(request.getName())
                .description(request.getDescription())
                .websiteUrl(request.getWebsiteUrl())
                .logoUrl(request.getLogoUrl())
                .location(request.getLocation())
                .build();
        Company saved = companyRepository.save(company);
        assignCompanyToCurrentEmployer(saved);
        auditLogService.record("CREATE", "Company", saved.getId(), "Created company", LogSeverity.INFO);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CompanyResponse updateCompany(Long id, CompanyRequest request) {
        Company company = companyRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));

        companyRepository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException(
                        "Company with name '" + request.getName() + "' already exists.", HttpStatus.CONFLICT);
            }
        });

        company.setName(request.getName());
        company.setDescription(request.getDescription());
        company.setWebsiteUrl(request.getWebsiteUrl());
        if (request.getLogoUrl() != null && !request.getLogoUrl().isBlank()) {
            company.setLogoUrl(request.getLogoUrl());
        }
        company.setLocation(request.getLocation());

        Company saved = companyRepository.save(company);
        auditLogService.record("UPDATE", "Company", saved.getId(), "Updated company", LogSeverity.INFO);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCompany(Long id) {
        Company company = companyRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
        companyRepository.delete(company);
        auditLogService.record("DELETE", "Company", id, "Deleted company", LogSeverity.WARN);
    }

    private void assignCompanyToCurrentEmployer(Company company) {
        currentUser().ifPresent(user -> {
            if (user.getCompany() == null && hasRole(user, RoleType.EMPLOYER)) {
                user.setCompany(company);
                userRepository.save(user);
            }
        });
    }

    private boolean hasRole(User user, RoleType roleType) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleType);
    }

    private Optional<User> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        String email = authentication.getName();
        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            return Optional.empty();
        }

        return userRepository.findByEmail(email);
    }
}
