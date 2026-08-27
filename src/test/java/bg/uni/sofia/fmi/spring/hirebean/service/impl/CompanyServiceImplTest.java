package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.CompanyRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.CompanyResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.BusinessException;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.exception.company.CompanyAlreadyExistsException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Company;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Role;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.LogSeverity;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.RoleType;
import bg.uni.sofia.fmi.spring.hirebean.repository.CompanyRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import bg.uni.sofia.fmi.spring.hirebean.service.AuditLogService;
import bg.uni.sofia.fmi.spring.hirebean.service.StorageService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final String COMPANY_NAME = "foo";
    private static final String OTHER_NAME = "bar";
    private static final String EMAIL = "test@test.com";
    private static final String LOGO_KEY = "company-logos/abc.png";
    private static final String LOGO_URL = "https://storage.test/public/abc.png";

    @Mock
    private StorageService storageService;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    private final Company company = createCompany();

    private CompanyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CompanyServiceImpl(storageService, companyRepository, userRepository, auditLogService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllCompanies_noSearch_returnsAllCompanies() {
        when(companyRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(company)));

        var result = service.getAllCompanies(null, Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo(COMPANY_NAME);
        verify(companyRepository, never())
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(any(), any(), any());
    }

    @Test
    void getAllCompanies_blankSearch_isTreatedAsNoSearch() {
        when(companyRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(company)));

        service.getAllCompanies("   ", Pageable.unpaged());

        verify(companyRepository).findAll(any(Pageable.class));
    }

    @Test
    void getAllCompanies_withSearch_delegatesToSearchQuery() {
        when(companyRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        eq("foo"), eq("foo"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(company)));

        var result = service.getAllCompanies("foo", Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(companyRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getCompanyById_existingCompany_returnsMappedCompanyWithPublicLogoUrl() {
        company.setLogoUrl(LOGO_KEY);
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(storageService.getPublicUrl(LOGO_KEY)).thenReturn(LOGO_URL);

        CompanyResponse response = service.getCompanyById(COMPANY_ID);

        assertThat(response.getId()).isEqualTo(COMPANY_ID);
        assertThat(response.getName()).isEqualTo(COMPANY_NAME);
        assertThat(response.getLogoUrl()).isEqualTo(LOGO_URL);
    }

    @Test
    void getCompanyById_companyDoesNotExist_throwsResourceNotFound() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCompanyById(COMPANY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(COMPANY_ID));
    }

    @Test
    void createCompany_nameAlreadyTaken_throwsCompanyAlreadyExists() {
        when(companyRepository.findByName(COMPANY_NAME)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> service.createCompany(createRequest(COMPANY_NAME)))
                .isInstanceOf(CompanyAlreadyExistsException.class);

        verify(companyRepository, never()).save(any());
    }

    @Test
    void createCompany_employerWithoutCompany_assignsCompanyToThatEmployer() {
        User employer = createUser(RoleType.EMPLOYER);
        stubAuthenticatedUser(employer);
        stubSuccessfulSave();

        service.createCompany(createRequest(COMPANY_NAME));

        assertThat(employer.getCompany()).isSameAs(company);
        verify(userRepository).save(employer);
        verify(auditLogService).record("CREATE", "Company", COMPANY_ID, "Created company", LogSeverity.INFO);
    }

    @Test
    void createCompany_employerAlreadyHasCompany_doesNotReassign() {
        User employer = createUser(RoleType.EMPLOYER);
        Company existing = createCompany();
        employer.setCompany(existing);
        stubAuthenticatedUser(employer);
        stubSuccessfulSave();

        service.createCompany(createRequest(COMPANY_NAME));

        assertThat(employer.getCompany()).isSameAs(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createCompany_candidateCreatesCompany_doesNotAssignIt() {
        User candidate = createUser(RoleType.CANDIDATE);
        stubAuthenticatedUser(candidate);
        stubSuccessfulSave();

        service.createCompany(createRequest(COMPANY_NAME));

        assertThat(candidate.getCompany()).isNull();
        verify(userRepository, never()).save(any());
    }

    @Test
    void createCompany_noAuthentication_savesCompanyWithoutAssigningIt() {
        stubSuccessfulSave();

        CompanyResponse response = service.createCompany(createRequest(COMPANY_NAME));

        assertThat(response.getName()).isEqualTo(COMPANY_NAME);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateCompany_companyDoesNotExist_throwsResourceNotFound() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCompany(COMPANY_ID, createRequest(COMPANY_NAME)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(companyRepository, never()).save(any());
    }

    @Test
    void updateCompany_nameTakenByAnotherCompany_throwsConflict() {
        Company other = createCompany();
        other.setId(99L);
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyRepository.findByName(OTHER_NAME)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.updateCompany(COMPANY_ID, createRequest(OTHER_NAME)))
                .isInstanceOf(BusinessException.class)
                .satisfies(thrown ->
                        assertThat(((BusinessException) thrown).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void updateCompany_keepingItsOwnName_updatesSuccessfully() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyRepository.findByName(COMPANY_NAME)).thenReturn(Optional.of(company));
        when(companyRepository.save(company)).thenReturn(company);

        CompanyRequest request = createRequest(COMPANY_NAME);
        request.setLocation("test-location");

        service.updateCompany(COMPANY_ID, request);

        assertThat(company.getLocation()).isEqualTo("test-location");
        verify(auditLogService).record("UPDATE", "Company", COMPANY_ID, "Updated company", LogSeverity.INFO);
    }

    @Test
    void updateCompany_blankLogoUrl_keepsExistingLogo() {
        company.setLogoUrl(LOGO_KEY);
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyRepository.findByName(COMPANY_NAME)).thenReturn(Optional.empty());
        when(companyRepository.save(company)).thenReturn(company);

        CompanyRequest request = createRequest(COMPANY_NAME);
        request.setLogoUrl("  ");

        service.updateCompany(COMPANY_ID, request);

        assertThat(company.getLogoUrl()).isEqualTo(LOGO_KEY);
    }

    @Test
    void updateCompany_newLogoUrl_replacesExistingLogo() {
        company.setLogoUrl(LOGO_KEY);
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyRepository.findByName(COMPANY_NAME)).thenReturn(Optional.empty());
        when(companyRepository.save(company)).thenReturn(company);

        CompanyRequest request = createRequest(COMPANY_NAME);
        request.setLogoUrl("company-logos/new.png");

        service.updateCompany(COMPANY_ID, request);

        assertThat(company.getLogoUrl()).isEqualTo("company-logos/new.png");
    }

    @Test
    void deleteCompany_existingCompany_deletesAndAuditsAtWarnLevel() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        service.deleteCompany(COMPANY_ID);

        verify(companyRepository).delete(company);
        verify(auditLogService).record("DELETE", "Company", COMPANY_ID, "Deleted company", LogSeverity.WARN);
    }

    @Test
    void deleteCompany_companyDoesNotExist_throwsResourceNotFound() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCompany(COMPANY_ID)).isInstanceOf(ResourceNotFoundException.class);

        verify(companyRepository, never()).delete(any());
    }

    private Company createCompany() {
        Company created = Company.builder()
                .name(COMPANY_NAME)
                .description("test-description")
                .websiteUrl("https://foo.test")
                .location("test-city")
                .build();
        created.setId(COMPANY_ID);
        return created;
    }

    private User createUser(RoleType roleType) {
        User created = User.builder()
                .email(EMAIL)
                .password("password-hash")
                .firstName("foo")
                .lastName("bar")
                .roles(new HashSet<>(Set.of(new Role(1L, roleType))))
                .build();
        created.setId(USER_ID);
        return created;
    }

    private CompanyRequest createRequest(String name) {
        CompanyRequest request = new CompanyRequest();
        request.setName(name);
        request.setDescription("test-description");
        request.setWebsiteUrl("https://foo.test");
        request.setLocation("test-city");
        return request;
    }

    private void stubAuthenticatedUser(User authenticatedUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(authenticatedUser.getEmail(), null, List.of()));
        SecurityContextHolder.setContext(context);
        when(userRepository.findByEmail(authenticatedUser.getEmail())).thenReturn(Optional.of(authenticatedUser));
    }

    private void stubSuccessfulSave() {
        when(companyRepository.findByName(COMPANY_NAME)).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenReturn(company);
    }
}
