package bg.uni.sofia.fmi.spring.hirebean.config;

import static org.assertj.core.api.Assertions.assertThat;

import bg.uni.sofia.fmi.spring.hirebean.model.entity.Company;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobApplication;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobOffer;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobStatus;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.RoleType;
import bg.uni.sofia.fmi.spring.hirebean.repository.BookmarkRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.CompanyRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobApplicationRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobOfferRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.NotificationRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.PostRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        properties = {
            "app.demo-data.enabled=true",
            "spring.datasource.url=jdbc:h2:mem:hirebean_demo_seed_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        })
@DirtiesContext
@Transactional
class DemoDataInitializerTest {

    private final DemoDataInitializer demoDataInitializer;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobOfferRepository jobOfferRepository;
    private final PostRepository postRepository;
    private final BookmarkRepository bookmarkRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    DemoDataInitializerTest(
            DemoDataInitializer demoDataInitializer,
            UserRepository userRepository,
            CompanyRepository companyRepository,
            JobOfferRepository jobOfferRepository,
            PostRepository postRepository,
            BookmarkRepository bookmarkRepository,
            JobApplicationRepository jobApplicationRepository,
            NotificationRepository notificationRepository,
            PasswordEncoder passwordEncoder) {
        this.demoDataInitializer = demoDataInitializer;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.postRepository = postRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Test
    void seedsCompleteDemoDataIdempotently() {
        SeedCounts beforeSecondRun = currentCounts();

        demoDataInitializer.run();

        assertThat(currentCounts()).isEqualTo(beforeSecondRun);
        assertThat(beforeSecondRun).isEqualTo(new SeedCounts(3, 1, 6, 2, 1, 1, 2));

        Company company =
                companyRepository.findByName(DemoDataInitializer.COMPANY_NAME).orElseThrow();
        User admin = userRepository.findByEmail(DemoDataInitializer.ADMIN_EMAIL).orElseThrow();
        User employer =
                userRepository.findByEmail(DemoDataInitializer.EMPLOYER_EMAIL).orElseThrow();
        User candidate =
                userRepository.findByEmail(DemoDataInitializer.CANDIDATE_EMAIL).orElseThrow();

        assertDemoUser(admin, "Admin123!", RoleType.ADMIN);
        assertDemoUser(employer, "Employer123!", RoleType.EMPLOYER);
        assertDemoUser(candidate, "Candidate123!", RoleType.CANDIDATE);
        assertThat(employer.getCompany().getId()).isEqualTo(company.getId());
        assertThat(candidate.getCandidateProfile()).isNotNull();

        List<JobOffer> jobs = jobOfferRepository
                .findAllByCompanyId(company.getId(), Pageable.unpaged())
                .getContent();
        assertThat(jobs).filteredOn(job -> job.getStatus() == JobStatus.ACTIVE).hasSize(5);
        assertThat(jobs)
                .filteredOn(job -> job.getStatus() == JobStatus.ACTIVE)
                .extracting(JobOffer::getJobType)
                .doesNotHaveDuplicates()
                .hasSize(5);
        assertThat(jobs).filteredOn(job -> job.getStatus() == JobStatus.DRAFT).hasSize(1);

        JobApplication application = jobApplicationRepository.findAll().getFirst();
        assertThat(application.getCvKey()).isNull();
        assertThat(application.getCandidate().getId()).isEqualTo(candidate.getId());
    }

    private void assertDemoUser(User user, String rawPassword, RoleType roleType) {
        assertThat(user.getPassword()).startsWith("$2").isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, user.getPassword())).isTrue();
        assertThat(user.getRoles()).anyMatch(role -> role.getName() == roleType);
    }

    private SeedCounts currentCounts() {
        return new SeedCounts(
                userRepository.count(),
                companyRepository.count(),
                jobOfferRepository.count(),
                postRepository.count(),
                bookmarkRepository.count(),
                jobApplicationRepository.count(),
                notificationRepository.count());
    }

    private record SeedCounts(
            long users, long companies, long jobs, long posts, long bookmarks, long applications, long notifications) {}
}
