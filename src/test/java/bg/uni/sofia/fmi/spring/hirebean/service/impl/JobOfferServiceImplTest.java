package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferFilterRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobOfferResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Company;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobOffer;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobStatus;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobType;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.LogSeverity;
import bg.uni.sofia.fmi.spring.hirebean.repository.CompanyRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobOfferRepository;
import bg.uni.sofia.fmi.spring.hirebean.security.JobOfferVisibilityScope;
import bg.uni.sofia.fmi.spring.hirebean.service.AuditLogService;
import bg.uni.sofia.fmi.spring.hirebean.service.StorageService;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class JobOfferServiceImplTest {

    private static final Long JOB_ID = 1L;
    private static final Long COMPANY_ID = 2L;
    private static final Long OTHER_COMPANY_ID = 3L;
    private static final String TITLE = "foo";
    private static final String COMPANY_NAME = "bar";
    private static final String LOGO_KEY = "company-logos/abc.png";
    private static final String LOGO_URL = "https://storage.test/public/abc.png";

    @Mock
    private JobOfferRepository jobOfferRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private AuditLogService auditLogService;

    private final Company company = createCompany();
    private final JobOffer jobOffer = createJobOffer(JobStatus.ACTIVE);

    private JobOfferServiceImpl jobOfferService;

    @BeforeEach
    void setUp() {
        jobOfferService =
                new JobOfferServiceImpl(jobOfferRepository, companyRepository, storageService, auditLogService);
    }

    @Test
    void getAllOffers_matchingOffers_returnsMappedPage() {
        when(jobOfferRepository.findAll(ArgumentMatchers.<Specification<JobOffer>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(jobOffer)));
        when(storageService.getPublicUrl(LOGO_KEY)).thenReturn(LOGO_URL);

        Page<JobOfferResponse> result = jobOfferService.getAllOffers(
                new JobOfferFilterRequest(), JobOfferVisibilityScope.publicVisibility(), Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
        JobOfferResponse response = result.getContent().get(0);
        assertThat(response.getId()).isEqualTo(JOB_ID);
        assertThat(response.getTitle()).isEqualTo(TITLE);
        assertThat(response.getCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(response.getCompanyName()).isEqualTo(COMPANY_NAME);
        assertThat(response.getCompanyLogoUrl()).isEqualTo(LOGO_URL);
    }

    @Test
    void getOfferById_activeOfferAndPublicScope_returnsOffer() {
        when(jobOfferRepository.findById(JOB_ID)).thenReturn(Optional.of(jobOffer));

        JobOfferResponse response = jobOfferService.getOfferById(JOB_ID, JobOfferVisibilityScope.publicVisibility());

        assertThat(response.getId()).isEqualTo(JOB_ID);
        assertThat(response.getStatus()).isEqualTo(JobStatus.ACTIVE);
    }

    @Test
    void getOfferById_draftOfferAndPublicScope_throwsResourceNotFound() {
        JobOffer draft = createJobOffer(JobStatus.DRAFT);
        when(jobOfferRepository.findById(JOB_ID)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> jobOfferService.getOfferById(JOB_ID, JobOfferVisibilityScope.publicVisibility()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOfferById_draftOfferAndFullVisibility_returnsOffer() {
        JobOffer draft = createJobOffer(JobStatus.DRAFT);
        when(jobOfferRepository.findById(JOB_ID)).thenReturn(Optional.of(draft));

        JobOfferResponse response = jobOfferService.getOfferById(JOB_ID, JobOfferVisibilityScope.fullVisibility());

        assertThat(response.getStatus()).isEqualTo(JobStatus.DRAFT);
    }

    @Test
    void getOfferById_draftOfferOwnedByManagedCompany_returnsOffer() {
        JobOffer draft = createJobOffer(JobStatus.DRAFT);
        when(jobOfferRepository.findById(JOB_ID)).thenReturn(Optional.of(draft));

        JobOfferResponse response =
                jobOfferService.getOfferById(JOB_ID, JobOfferVisibilityScope.managedCompanyVisibility(COMPANY_ID));

        assertThat(response.getStatus()).isEqualTo(JobStatus.DRAFT);
    }

    @Test
    void getOfferById_draftOfferOwnedByAnotherCompany_throwsResourceNotFound() {
        JobOffer draft = createJobOffer(JobStatus.DRAFT);
        when(jobOfferRepository.findById(JOB_ID)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> jobOfferService.getOfferById(
                        JOB_ID, JobOfferVisibilityScope.managedCompanyVisibility(OTHER_COMPANY_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOfferById_offerDoesNotExist_throwsResourceNotFound() {
        when(jobOfferRepository.findById(JOB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobOfferService.getOfferById(JOB_ID, JobOfferVisibilityScope.fullVisibility()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(JOB_ID));
    }

    @Test
    void createOffer_companyDoesNotExist_throwsResourceNotFound() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobOfferService.createOffer(createRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(COMPANY_ID));

        verify(jobOfferRepository, never()).save(any());
    }

    @Test
    void createOffer_validRequest_savesOfferAndAudits() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(jobOffer);

        JobOfferResponse response = jobOfferService.createOffer(createRequest());

        assertThat(response.getTitle()).isEqualTo(TITLE);
        verify(auditLogService).record("CREATE", "JobOffer", JOB_ID, "Created job offer", LogSeverity.INFO);
    }

    @Test
    void createOffer_statusOmitted_defaultsToActive() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(jobOfferRepository.save(any(JobOffer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobOfferRequest request = createRequest();
        request.setStatus(null);

        JobOfferResponse response = jobOfferService.createOffer(request);

        assertThat(response.getStatus()).isEqualTo(JobStatus.ACTIVE);
    }

    @Test
    void createOffer_tagsOmitted_defaultsToEmptySet() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(jobOfferRepository.save(any(JobOffer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobOfferRequest request = createRequest();
        request.setTags(null);

        JobOfferResponse response = jobOfferService.createOffer(request);

        assertThat(response.getTags()).isEmpty();
    }

    @Test
    void updateOffer_offerDoesNotExist_throwsResourceNotFound() {
        when(jobOfferRepository.findById(JOB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobOfferService.updateOffer(JOB_ID, createRequest()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobOfferRepository, never()).save(any());
    }

    @Test
    void updateOffer_companyDoesNotExist_throwsResourceNotFound() {
        when(jobOfferRepository.findById(JOB_ID)).thenReturn(Optional.of(jobOffer));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobOfferService.updateOffer(JOB_ID, createRequest()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobOfferRepository, never()).save(any());
    }

    @Test
    void updateOffer_validRequest_updatesFieldsAndAudits() {
        when(jobOfferRepository.findById(JOB_ID)).thenReturn(Optional.of(jobOffer));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(jobOfferRepository.save(jobOffer)).thenReturn(jobOffer);

        JobOfferRequest request = createRequest();
        request.setTitle("baz");
        request.setLocation("test-city");
        request.setStatus(JobStatus.ARCHIVED);

        jobOfferService.updateOffer(JOB_ID, request);

        assertThat(jobOffer.getTitle()).isEqualTo("baz");
        assertThat(jobOffer.getLocation()).isEqualTo("test-city");
        assertThat(jobOffer.getStatus()).isEqualTo(JobStatus.ARCHIVED);
        verify(auditLogService).record("UPDATE", "JobOffer", JOB_ID, "Updated job offer", LogSeverity.INFO);
    }

    @Test
    void updateOffer_statusOmitted_defaultsToActive() {
        JobOffer archived = createJobOffer(JobStatus.ARCHIVED);
        when(jobOfferRepository.findById(JOB_ID)).thenReturn(Optional.of(archived));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(jobOfferRepository.save(archived)).thenReturn(archived);

        JobOfferRequest request = createRequest();
        request.setStatus(null);

        jobOfferService.updateOffer(JOB_ID, request);

        assertThat(archived.getStatus()).isEqualTo(JobStatus.ACTIVE);
    }

    @Test
    void deleteOffer_existingOffer_deletesAndAuditsAtWarnLevel() {
        when(jobOfferRepository.findById(JOB_ID)).thenReturn(Optional.of(jobOffer));

        jobOfferService.deleteOffer(JOB_ID);

        verify(jobOfferRepository).delete(jobOffer);
        verify(auditLogService).record("DELETE", "JobOffer", JOB_ID, "Deleted job offer", LogSeverity.WARN);
    }

    @Test
    void deleteOffer_offerDoesNotExist_throwsResourceNotFound() {
        when(jobOfferRepository.findById(JOB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobOfferService.deleteOffer(JOB_ID)).isInstanceOf(ResourceNotFoundException.class);

        verify(jobOfferRepository, never()).delete(any(JobOffer.class));
    }

    private Company createCompany() {
        Company created = Company.builder()
                .name(COMPANY_NAME)
                .description("test-description")
                .logoUrl(LOGO_KEY)
                .build();
        created.setId(COMPANY_ID);
        return created;
    }

    private JobOffer createJobOffer(JobStatus status) {
        JobOffer created = JobOffer.builder()
                .title(TITLE)
                .description("test-description")
                .location("test-city")
                .jobType(JobType.FULL_TIME)
                .minSalary(BigDecimal.valueOf(1000))
                .maxSalary(BigDecimal.valueOf(2000))
                .status(status)
                .company(company)
                .tags(new HashSet<>(Set.of("java")))
                .build();
        created.setId(JOB_ID);
        return created;
    }

    private JobOfferRequest createRequest() {
        JobOfferRequest request = new JobOfferRequest();
        request.setTitle(TITLE);
        request.setDescription("test-description");
        request.setLocation("test-city");
        request.setJobType(JobType.FULL_TIME);
        request.setMinSalary(BigDecimal.valueOf(1000));
        request.setMaxSalary(BigDecimal.valueOf(2000));
        request.setStatus(JobStatus.ACTIVE);
        request.setCompanyId(COMPANY_ID);
        request.setTags(new HashSet<>(Set.of("java")));
        return request;
    }
}
