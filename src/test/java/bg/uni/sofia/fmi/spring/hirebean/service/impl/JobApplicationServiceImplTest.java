package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.InterviewInvitationRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobApplicationRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.ReviewApplicationRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobApplicationResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.application.JobApplicationAlreadyExistsException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Company;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobApplication;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobOffer;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.ApplicationStatus;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobStatus;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.LogSeverity;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobApplicationRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobOfferRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import bg.uni.sofia.fmi.spring.hirebean.service.AuditLogService;
import bg.uni.sofia.fmi.spring.hirebean.service.EmailService;
import bg.uni.sofia.fmi.spring.hirebean.service.NotificationService;
import bg.uni.sofia.fmi.spring.hirebean.service.StorageService;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceImplTest {

    private static final String APPLICATION_CONSTRAINT = "uk_job_applications_candidate_job_offer";

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobOfferRepository jobOfferRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    private JobApplicationServiceImpl service;
    private JobApplication application;
    private User candidate;
    private JobOffer jobOffer;

    @BeforeEach
    void setUp() {
        service = new JobApplicationServiceImpl(
                jobApplicationRepository,
                userRepository,
                jobOfferRepository,
                storageService,
                notificationService,
                emailService,
                auditLogService);

        candidate = User.builder().email("candidate@test.local").build();
        candidate.setId(20L);
        Company company = Company.builder().name("HireBean").build();
        company.setId(10L);
        jobOffer = JobOffer.builder()
                .title("Backend Developer")
                .company(company)
                .status(JobStatus.ACTIVE)
                .build();
        jobOffer.setId(30L);
        application = JobApplication.builder()
                .candidate(candidate)
                .jobOffer(jobOffer)
                .status(ApplicationStatus.PENDING)
                .build();
        application.setId(40L);
    }

    @Test
    void reviewStoresFeedbackNotifiesCandidateAndAuditsChange() {
        stubStoredApplication();
        ReviewApplicationRequest request = new ReviewApplicationRequest();
        request.setStatus(ApplicationStatus.REVIEWED);
        request.setFeedbackMessage("Please send your availability for a technical interview.");

        var response = service.review(40L, request);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.REVIEWED);
        assertThat(response.getFeedbackMessage()).contains("technical interview");
        verify(notificationService)
                .createNotification(
                        20L,
                        "Your application for Backend Developer is now REVIEWED: "
                                + "Please send your availability for a technical interview.",
                        "APPLICATION_FEEDBACK");
        verify(auditLogService)
                .record(
                        "REVIEW",
                        "JobApplication",
                        40L,
                        "Updated application to REVIEWED with candidate feedback",
                        LogSeverity.INFO);
    }

    @Test
    void schedulingInterviewCanBeModifiedAndNotifiesCandidate() {
        stubStoredApplication();
        LocalDateTime interviewAt =
                LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
        InterviewInvitationRequest request = new InterviewInvitationRequest();
        request.setInterviewAt(interviewAt);
        request.setMessage("The interview will take place over Google Meet.");

        var response = service.scheduleInterview(40L, request);

        assertThat(response.getInterviewAt()).isEqualTo(interviewAt);
        assertThat(response.getInterviewMessage()).isEqualTo(request.getMessage());
        verify(notificationService)
                .createNotification(
                        20L,
                        "You are invited to an interview for Backend Developer on "
                                + interviewAt.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                                + ". The interview will take place over Google Meet.",
                        "INTERVIEW_INVITATION");
        verify(auditLogService)
                .record(
                        "INTERVIEW_SCHEDULED",
                        "JobApplication",
                        40L,
                        "Interview scheduled for "
                                + interviewAt.format(
                                        java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")),
                        LogSeverity.INFO);
    }

    @Test
    void duplicateApplicationReturnsConflictWithoutUploadingCv() {
        JobApplicationRequest request = applicationRequest();
        MockMultipartFile cv = cvFile();
        when(jobOfferRepository.findById(30L)).thenReturn(Optional.of(jobOffer));
        when(jobApplicationRepository.existsByCandidateIdAndJobOfferId(20L, 30L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.apply(20L, request, cv))
                .isInstanceOf(JobApplicationAlreadyExistsException.class)
                .hasMessage("You have already applied for this job.")
                .extracting(error -> ((JobApplicationAlreadyExistsException) error).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(jobApplicationRepository, never()).saveAndFlush(any());
        verifyNoInteractions(storageService);
    }

    @Test
    void hasAppliedReturnsRepositoryState() {
        when(jobApplicationRepository.existsByCandidateIdAndJobOfferId(20L, 30L))
                .thenReturn(true, false);

        assertThat(service.hasApplied(20L, 30L)).isTrue();
        assertThat(service.hasApplied(20L, 30L)).isFalse();
    }

    @Test
    void newApplicationIsVisibleForTheJobAfterPersistence() {
        AtomicReference<JobApplication> persisted = new AtomicReference<>();
        MockMultipartFile cv = cvFile();
        when(jobOfferRepository.findById(30L)).thenReturn(Optional.of(jobOffer));
        when(userRepository.findById(20L)).thenReturn(Optional.of(candidate));
        when(jobApplicationRepository.saveAndFlush(any(JobApplication.class))).thenAnswer(invocation -> {
            JobApplication saved = invocation.getArgument(0);
            saved.setId(41L);
            persisted.set(saved);
            return saved;
        });
        when(storageService.uploadFile(cv, "cvs")).thenReturn("cvs/candidate.pdf");
        when(storageService.getPresignedUrl("cvs/candidate.pdf")).thenReturn("https://storage.test/candidate.pdf");
        when(jobApplicationRepository.findAllByJobOfferId(30L)).thenAnswer(invocation -> List.of(persisted.get()));

        var created = service.apply(20L, applicationRequest(), cv);
        var visibleApplications = service.getApplicationsForJobOffer(30L);

        assertThat(created.getId()).isEqualTo(41L);
        assertThat(created.getCvUrl()).isEqualTo("https://storage.test/candidate.pdf");
        assertThat(visibleApplications)
                .extracting(JobApplicationResponse::getId)
                .containsExactly(41L);
        InOrder persistenceBeforeStorage = inOrder(jobApplicationRepository, storageService);
        persistenceBeforeStorage.verify(jobApplicationRepository).saveAndFlush(any(JobApplication.class));
        persistenceBeforeStorage.verify(storageService).uploadFile(cv, "cvs");
    }

    @Test
    void raceOnApplicationUniqueConstraintReturnsConflictWithoutUploadingCv() {
        DataIntegrityViolationException integrityError = constraintViolation(APPLICATION_CONSTRAINT);
        stubApplicationPrerequisites();
        when(jobApplicationRepository.saveAndFlush(any(JobApplication.class))).thenThrow(integrityError);

        assertThatThrownBy(() -> service.apply(20L, applicationRequest(), cvFile()))
                .isInstanceOf(JobApplicationAlreadyExistsException.class)
                .hasMessage("You have already applied for this job.");

        verifyNoInteractions(storageService);
    }

    @Test
    void unrelatedIntegrityViolationIsNotMaskedAsDuplicateApplication() {
        DataIntegrityViolationException integrityError = constraintViolation("uk_other_constraint");
        stubApplicationPrerequisites();
        when(jobApplicationRepository.saveAndFlush(any(JobApplication.class))).thenThrow(integrityError);

        assertThatThrownBy(() -> service.apply(20L, applicationRequest(), cvFile()))
                .isSameAs(integrityError);
    }

    private void stubStoredApplication() {
        when(storageService.getPresignedUrl(null)).thenReturn(null);
        when(jobApplicationRepository.findById(40L)).thenReturn(Optional.of(application));
        when(jobApplicationRepository.save(application)).thenReturn(application);
    }

    private void stubApplicationPrerequisites() {
        when(jobOfferRepository.findById(30L)).thenReturn(Optional.of(jobOffer));
        when(userRepository.findById(20L)).thenReturn(Optional.of(candidate));
    }

    private JobApplicationRequest applicationRequest() {
        JobApplicationRequest request = new JobApplicationRequest();
        request.setJobOfferId(30L);
        request.setCoverLetter("I would love to join the team.");
        return request;
    }

    private MockMultipartFile cvFile() {
        return new MockMultipartFile("cv", "candidate.pdf", "application/pdf", "CV".getBytes());
    }

    private DataIntegrityViolationException constraintViolation(String constraintName) {
        ConstraintViolationException cause =
                new ConstraintViolationException("Constraint violation", new SQLException(), "insert", constraintName);
        return new DataIntegrityViolationException("Could not persist job application", cause);
    }
}
