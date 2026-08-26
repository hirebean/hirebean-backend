package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.InterviewInvitationRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.ReviewApplicationRequest;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Company;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobApplication;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobOffer;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.ApplicationStatus;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.LogSeverity;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobApplicationRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobOfferRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import bg.uni.sofia.fmi.spring.hirebean.service.AuditLogService;
import bg.uni.sofia.fmi.spring.hirebean.service.EmailService;
import bg.uni.sofia.fmi.spring.hirebean.service.NotificationService;
import bg.uni.sofia.fmi.spring.hirebean.service.StorageService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceImplTest {

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

        User candidate = User.builder().email("candidate@test.local").build();
        candidate.setId(20L);
        JobOffer offer = JobOffer.builder()
                .title("Backend Developer")
                .company(Company.builder().name("HireBean").build())
                .build();
        offer.setId(30L);
        application = JobApplication.builder()
                .candidate(candidate)
                .jobOffer(offer)
                .status(ApplicationStatus.PENDING)
                .build();
        application.setId(40L);

        when(storageService.getPresignedUrl(null)).thenReturn(null);
        when(jobApplicationRepository.findById(40L)).thenReturn(Optional.of(application));
        when(jobApplicationRepository.save(application)).thenReturn(application);
    }

    @Test
    void reviewStoresFeedbackNotifiesCandidateAndAuditsChange() {
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
}
