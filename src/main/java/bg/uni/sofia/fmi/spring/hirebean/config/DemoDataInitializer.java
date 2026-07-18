package bg.uni.sofia.fmi.spring.hirebean.config;

import bg.uni.sofia.fmi.spring.hirebean.model.entity.Bookmark;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.CandidateProfile;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Company;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobApplication;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobOffer;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Notification;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Post;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Role;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.ApplicationStatus;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobStatus;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobType;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.RoleType;
import bg.uni.sofia.fmi.spring.hirebean.repository.BookmarkRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.CompanyRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobApplicationRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobOfferRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.NotificationRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.PostRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.RoleRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements CommandLineRunner {

    static final String ADMIN_EMAIL = "admin@hirebean.dev";
    static final String EMPLOYER_EMAIL = "employer@hirebean.dev";
    static final String CANDIDATE_EMAIL = "candidate@hirebean.dev";
    static final String COMPANY_NAME = "BluePeak Technologies";

    private static final String ADMIN_PASSWORD = "Admin123!";
    private static final String EMPLOYER_PASSWORD = "Employer123!";
    private static final String CANDIDATE_PASSWORD = "Candidate123!";

    private static final String APPLICATION_JOB_TITLE = "Senior Java Engineer";
    private static final String CANDIDATE_NOTIFICATION = "Your application for Senior Java Engineer is under review.";
    private static final String EMPLOYER_NOTIFICATION = "New application received for Senior Java Engineer.";

    private static final List<JobSeed> JOBS = List.of(
            new JobSeed(
                    APPLICATION_JOB_TITLE,
                    "Build reliable hiring workflows and scalable APIs with Java, Spring Boot, PostgreSQL, and AWS.",
                    "Sofia / Hybrid",
                    JobType.FULL_TIME,
                    "6000",
                    "9000",
                    JobStatus.ACTIVE,
                    Set.of("Java", "Spring Boot", "PostgreSQL", "AWS")),
            new JobSeed(
                    "React TypeScript Engineer",
                    "Create accessible, responsive product experiences for candidates and hiring teams.",
                    "Remote (Bulgaria)",
                    JobType.PART_TIME,
                    "3000",
                    "4800",
                    JobStatus.ACTIVE,
                    Set.of("React", "TypeScript", "Material UI", "Vite")),
            new JobSeed(
                    "Cloud and DevOps Engineer",
                    "Improve delivery pipelines, observability, and cloud infrastructure for a growing SaaS platform.",
                    "Sofia",
                    JobType.CONTRACT,
                    "5500",
                    "8500",
                    JobStatus.ACTIVE,
                    Set.of("AWS", "Docker", "Kubernetes", "Terraform")),
            new JobSeed(
                    "QA Automation Engineer",
                    "Own end-to-end quality automation across web, API, and continuous delivery workflows.",
                    "Plovdiv / Hybrid",
                    JobType.FREELANCE,
                    "3500",
                    "5500",
                    JobStatus.ACTIVE,
                    Set.of("Playwright", "Java", "REST Assured", "CI/CD")),
            new JobSeed(
                    "Software Engineering Intern",
                    "Join a mentored team and contribute to production features across the HireBean platform.",
                    "Sofia",
                    JobType.INTERN,
                    "1500",
                    "2200",
                    JobStatus.ACTIVE,
                    Set.of("Java", "React", "Git", "SQL")),
            new JobSeed(
                    "Product Designer",
                    "Shape a clear and inclusive hiring experience from discovery through polished product delivery.",
                    "Varna / Remote",
                    JobType.FULL_TIME,
                    "4000",
                    "6500",
                    JobStatus.DRAFT,
                    Set.of("Figma", "Design Systems", "UX Research")));

    private static final List<PostSeed> POSTS = List.of(
            new PostSeed(
                    "How BluePeak builds supportive engineering teams",
                    "A practical look at mentorship, focused delivery, and the habits that help our engineers grow."),
            new PostSeed(
                    "Inside our hybrid work culture",
                    "We combine intentional office time with flexible remote work and clear, documented collaboration."));

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobOfferRepository jobOfferRepository;
    private final PostRepository postRepository;
    private final BookmarkRepository bookmarkRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Role adminRole = requireRole(RoleType.ADMIN);
        Role employerRole = requireRole(RoleType.EMPLOYER);
        Role candidateRole = requireRole(RoleType.CANDIDATE);

        Company company = ensureCompany();
        ensureUser(ADMIN_EMAIL, ADMIN_PASSWORD, "Demo", "Admin", adminRole, null);
        User employer = ensureUser(EMPLOYER_EMAIL, EMPLOYER_PASSWORD, "Elena", "Petrova", employerRole, company);
        User candidate = ensureUser(CANDIDATE_EMAIL, CANDIDATE_PASSWORD, "Nikolay", "Ivanov", candidateRole, null);
        ensureCandidateProfile(candidate);

        List<JobOffer> jobs =
                JOBS.stream().map(seed -> ensureJob(company, seed)).toList();
        POSTS.forEach(seed -> ensurePost(company, employer, seed));

        JobOffer applicationJob = jobs.stream()
                .filter(job -> APPLICATION_JOB_TITLE.equals(job.getTitle()))
                .findFirst()
                .orElseThrow();
        ensureBookmark(candidate, applicationJob);
        ensureApplication(candidate, applicationJob);
        ensureNotification(candidate, CANDIDATE_NOTIFICATION, "APPLICATION_STATUS");
        ensureNotification(employer, EMPLOYER_NOTIFICATION, "NEW_APPLICATION");

        log.info("HireBean demo data is ready");
    }

    private Role requireRole(RoleType roleType) {
        return roleRepository
                .findByName(roleType)
                .orElseThrow(() -> new IllegalStateException("Required role is missing: " + roleType));
    }

    private Company ensureCompany() {
        return companyRepository
                .findByName(COMPANY_NAME)
                .orElseGet(() -> companyRepository.save(Company.builder()
                        .name(COMPANY_NAME)
                        .description(
                                "BluePeak Technologies builds thoughtful software products that make hiring simpler and more human.")
                        .websiteUrl("https://bluepeak.example")
                        .location("Sofia, Bulgaria")
                        .build()));
    }

    private User ensureUser(
            String email, String rawPassword, String firstName, String lastName, Role role, Company company) {
        User user = userRepository.findByEmail(email).orElseGet(() -> User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .firstName(firstName)
                .lastName(lastName)
                .roles(new HashSet<>())
                .company(company)
                .build());

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        if (user.getFirstName() == null || user.getFirstName().isBlank()) {
            user.setFirstName(firstName);
        }
        if (user.getLastName() == null || user.getLastName().isBlank()) {
            user.setLastName(lastName);
        }
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
        user.getRoles().add(role);
        if (company != null) {
            user.setCompany(company);
        }
        return userRepository.save(user);
    }

    private void ensureCandidateProfile(User candidate) {
        CandidateProfile profile = candidate.getCandidateProfile();
        if (profile == null) {
            profile = CandidateProfile.builder()
                    .user(candidate)
                    .bio("Java and React developer focused on reliable products and collaborative teams.")
                    .linkedinUrl("https://www.linkedin.com/in/hirebean-demo-candidate")
                    .githubUrl("https://github.com/hirebean-demo-candidate")
                    .jobTitle("Full-stack Developer")
                    .build();
            candidate.setCandidateProfile(profile);
            userRepository.save(candidate);
        }
    }

    private JobOffer ensureJob(Company company, JobSeed seed) {
        return jobOfferRepository
                .findByCompanyIdAndTitle(company.getId(), seed.title())
                .orElseGet(() -> jobOfferRepository.save(JobOffer.builder()
                        .title(seed.title())
                        .description(seed.description())
                        .location(seed.location())
                        .jobType(seed.jobType())
                        .minSalary(new BigDecimal(seed.minSalary()))
                        .maxSalary(new BigDecimal(seed.maxSalary()))
                        .status(seed.status())
                        .company(company)
                        .tags(seed.tags())
                        .build()));
    }

    private void ensurePost(Company company, User employer, PostSeed seed) {
        if (postRepository
                .findByCompanyIdAndTitle(company.getId(), seed.title())
                .isEmpty()) {
            postRepository.save(Post.builder()
                    .title(seed.title())
                    .content(seed.content())
                    .company(company)
                    .author(employer)
                    .build());
        }
    }

    private void ensureBookmark(User candidate, JobOffer jobOffer) {
        if (!bookmarkRepository.existsByUserIdAndJobOfferId(candidate.getId(), jobOffer.getId())) {
            bookmarkRepository.save(
                    Bookmark.builder().user(candidate).jobOffer(jobOffer).build());
        }
    }

    private void ensureApplication(User candidate, JobOffer jobOffer) {
        if (!jobApplicationRepository.existsByCandidateIdAndJobOfferId(candidate.getId(), jobOffer.getId())) {
            jobApplicationRepository.save(JobApplication.builder()
                    .candidate(candidate)
                    .jobOffer(jobOffer)
                    .coverLetter(
                            "I am excited to contribute my Spring Boot and React experience to BluePeak's product team.")
                    .status(ApplicationStatus.REVIEWED)
                    .cvKey(null)
                    .build());
        }
    }

    private void ensureNotification(User recipient, String message, String type) {
        if (!notificationRepository.existsByRecipientIdAndTypeAndMessage(recipient.getId(), type, message)) {
            notificationRepository.save(Notification.builder()
                    .recipient(recipient)
                    .message(message)
                    .type(type)
                    .build());
        }
    }

    private record JobSeed(
            String title,
            String description,
            String location,
            JobType jobType,
            String minSalary,
            String maxSalary,
            JobStatus status,
            Set<String> tags) {}

    private record PostSeed(String title, String content) {}
}
