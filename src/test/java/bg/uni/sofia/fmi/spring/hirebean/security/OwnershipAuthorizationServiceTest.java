package bg.uni.sofia.fmi.spring.hirebean.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferRequest;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Company;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobApplication;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobOffer;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Role;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.RoleType;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobApplicationRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobOfferRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.NotificationRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.PostRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class OwnershipAuthorizationServiceTest {

    private static final String EMPLOYER_EMAIL = "employer@hirebean.test";
    private static final String CANDIDATE_EMAIL = "candidate@hirebean.test";

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobOfferRepository jobOfferRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private NotificationRepository notificationRepository;

    private OwnershipAuthorizationService ownershipAuthorizationService;
    private Company employerCompany;
    private Company otherCompany;
    private User employer;
    private User candidate;

    @BeforeEach
    void setUp() {
        ownershipAuthorizationService = new OwnershipAuthorizationService(
                userRepository, jobOfferRepository, postRepository, jobApplicationRepository, notificationRepository);

        employerCompany = company(1L);
        otherCompany = company(2L);
        employer = user(10L, EMPLOYER_EMAIL, RoleType.EMPLOYER, employerCompany);
        candidate = user(20L, CANDIDATE_EMAIL, RoleType.CANDIDATE, null);
    }

    @Test
    void employerCanCreateJobOfferOnlyForOwnCompany() {
        when(userRepository.findByEmail(EMPLOYER_EMAIL)).thenReturn(Optional.of(employer));

        JobOfferRequest ownCompanyRequest = jobOfferRequest(employerCompany.getId());
        JobOfferRequest otherCompanyRequest = jobOfferRequest(otherCompany.getId());

        Authentication authentication = authentication(EMPLOYER_EMAIL, "ROLE_EMPLOYER");

        assertTrue(ownershipAuthorizationService.canCreateJobOffer(authentication, ownCompanyRequest));
        assertFalse(ownershipAuthorizationService.canCreateJobOffer(authentication, otherCompanyRequest));
    }

    @Test
    void candidateCanApplyOnlyAsSelf() {
        when(userRepository.findByEmail(CANDIDATE_EMAIL)).thenReturn(Optional.of(candidate));

        Authentication authentication = authentication(CANDIDATE_EMAIL, "ROLE_CANDIDATE");

        assertTrue(ownershipAuthorizationService.canApplyAsCandidate(authentication, candidate.getId()));
        assertFalse(ownershipAuthorizationService.canApplyAsCandidate(authentication, 999L));
    }

    @Test
    void employerCanManageApplicationsOnlyForOwnCompanyJobs() {
        when(userRepository.findByEmail(EMPLOYER_EMAIL)).thenReturn(Optional.of(employer));
        when(jobApplicationRepository.findById(100L))
                .thenReturn(Optional.of(jobApplication(100L, jobOffer(50L, employerCompany))));
        when(jobApplicationRepository.findById(200L))
                .thenReturn(Optional.of(jobApplication(200L, jobOffer(60L, otherCompany))));

        Authentication authentication = authentication(EMPLOYER_EMAIL, "ROLE_EMPLOYER");

        assertTrue(ownershipAuthorizationService.canManageApplication(authentication, 100L));
        assertFalse(ownershipAuthorizationService.canManageApplication(authentication, 200L));
    }

    private Authentication authentication(String email, String authority) {
        return new UsernamePasswordAuthenticationToken(email, null, Set.of(new SimpleGrantedAuthority(authority)));
    }

    private JobOfferRequest jobOfferRequest(Long companyId) {
        JobOfferRequest request = new JobOfferRequest();
        request.setCompanyId(companyId);
        return request;
    }

    private Company company(Long id) {
        Company company = Company.builder().name("Company " + id).build();
        company.setId(id);
        return company;
    }

    private User user(Long id, String email, RoleType roleType, Company company) {
        User user = User.builder()
                .email(email)
                .password("secret")
                .roles(Set.of(new Role(null, roleType)))
                .company(company)
                .build();
        user.setId(id);
        return user;
    }

    private JobOffer jobOffer(Long id, Company company) {
        JobOffer jobOffer = JobOffer.builder()
                .title("Backend Developer")
                .description("Spring")
                .company(company)
                .build();
        jobOffer.setId(id);
        return jobOffer;
    }

    private JobApplication jobApplication(Long id, JobOffer jobOffer) {
        JobApplication application =
                JobApplication.builder().candidate(candidate).jobOffer(jobOffer).build();
        application.setId(id);
        return application;
    }
}
