package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.InterviewInvitationRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobApplicationRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.ReviewApplicationRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobApplicationResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.exception.job.JobOfferClosedException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobApplication;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobOffer;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.ApplicationStatus;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobStatus;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobApplicationRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobOfferRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import bg.uni.sofia.fmi.spring.hirebean.service.AuditLogService;
import bg.uni.sofia.fmi.spring.hirebean.service.EmailService;
import bg.uni.sofia.fmi.spring.hirebean.service.JobApplicationService;
import bg.uni.sofia.fmi.spring.hirebean.service.NotificationService;
import bg.uni.sofia.fmi.spring.hirebean.service.StorageService;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    private static final DateTimeFormatter INTERVIEW_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final JobApplicationRepository jobApplicationRepository;

    private final UserRepository userRepository;

    private final JobOfferRepository jobOfferRepository;

    private final StorageService storageService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    private JobApplicationResponse mapToResponse(JobApplication application) {
        return JobApplicationResponse.builder()
                .id(application.getId())
                .candidateId(application.getCandidate().getId())
                .candidateEmail(application.getCandidate().getEmail())
                .jobOfferId(application.getJobOffer().getId())
                .jobTitle(application.getJobOffer().getTitle())
                .coverLetter(application.getCoverLetter())
                .cvUrl(storageService.getPresignedUrl(application.getCvKey()))
                .status(application.getStatus())
                .feedbackMessage(application.getFeedbackMessage())
                .interviewAt(application.getInterviewAt())
                .interviewMessage(application.getInterviewMessage())
                .createdAt(application.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public JobApplicationResponse apply(Long candidateId, JobApplicationRequest request, MultipartFile cvFile) {
        JobOffer jobOffer = jobOfferRepository
                .findById(request.getJobOfferId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Job offer not found with id: " + request.getJobOfferId()));

        if (jobOffer.getStatus() != JobStatus.ACTIVE) {
            throw new JobOfferClosedException(
                    "Cannot apply to job offer with id: " + request.getJobOfferId() + " because it is not active");
        }
        if (jobApplicationRepository.existsByCandidateIdAndJobOfferId(candidateId, request.getJobOfferId())) {
            throw new JobOfferClosedException("Candidate with id: " + candidateId
                    + " has already applied to job offer with id: " + request.getJobOfferId());
        }

        User candidate = userRepository
                .findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + candidateId));

        // Upload CV to private Supabase Storage under the "cvs/" folder
        String cvKey = null;
        if (cvFile != null && !cvFile.isEmpty()) {
            cvKey = storageService.uploadFile(cvFile, "cvs");
        }

        JobApplication application = JobApplication.builder()
                .candidate(candidate)
                .jobOffer(jobOffer)
                .coverLetter(request.getCoverLetter())
                .cvKey(cvKey)
                .status(ApplicationStatus.PENDING)
                .build();

        JobApplication saved = jobApplicationRepository.save(application);

        userRepository
                .findAllByCompanyId(jobOffer.getCompany().getId())
                .forEach(employee -> notificationService.createNotification(
                        employee.getId(),
                        "New application for " + jobOffer.getTitle() + " from " + candidate.getEmail(),
                        "APPLICATION_CREATED"));

        auditLogService.record(
                "APPLY", "JobApplication", saved.getId(), candidateId, "Candidate applied for job", "INFO");
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getApplicationsForCandidate(Long candidateId) {
        return jobApplicationRepository.findAllByCandidateId(candidateId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getApplicationsForJobOffer(Long jobOfferId) {
        return jobApplicationRepository.findAllByJobOfferId(jobOfferId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public JobApplicationResponse updateStatus(Long applicationId, ApplicationStatus status) {
        JobApplication jobApplication = jobApplicationRepository
                .findById(applicationId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Job application not found with id: " + applicationId));
        jobApplication.setStatus(status);
        jobApplicationRepository.save(jobApplication);

        notificationService.createNotification(
                jobApplication.getCandidate().getId(),
                "Your application for " + jobApplication.getJobOffer().getTitle() + " is now " + status,
                "APPLICATION_STATUS_UPDATED");
        emailService.sendApplicationStatusEmail(
                jobApplication.getCandidate().getEmail(),
                jobApplication.getJobOffer().getTitle(),
                status);
        auditLogService.record(
                "STATUS_UPDATE", "JobApplication", applicationId, "Updated application status to " + status, "INFO");

        return mapToResponse(jobApplication);
    }

    @Override
    @Transactional
    public JobApplicationResponse review(Long applicationId, ReviewApplicationRequest request) {
        JobApplication application = getApplication(applicationId);
        application.setStatus(request.getStatus());
        application.setFeedbackMessage(normalize(request.getFeedbackMessage()));
        JobApplication saved = jobApplicationRepository.save(application);

        String feedback = saved.getFeedbackMessage();
        String message = "Your application for " + saved.getJobOffer().getTitle() + " is now " + saved.getStatus();
        if (feedback != null) {
            message += ": " + feedback;
        }
        notificationService.createNotification(saved.getCandidate().getId(), message, "APPLICATION_FEEDBACK");
        emailService.sendApplicationFeedbackEmail(
                saved.getCandidate().getEmail(),
                saved.getJobOffer().getTitle(),
                saved.getStatus(),
                saved.getFeedbackMessage());
        auditLogService.record(
                "REVIEW",
                "JobApplication",
                applicationId,
                "Updated application to " + saved.getStatus() + " with candidate feedback",
                "INFO");
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public JobApplicationResponse scheduleInterview(Long applicationId, InterviewInvitationRequest request) {
        JobApplication application = getApplication(applicationId);
        boolean update = application.getInterviewAt() != null;
        application.setInterviewAt(request.getInterviewAt());
        application.setInterviewMessage(normalize(request.getMessage()));
        JobApplication saved = jobApplicationRepository.save(application);

        String date = saved.getInterviewAt().format(INTERVIEW_DATE_FORMATTER);
        String message = (update ? "Your interview was updated" : "You are invited to an interview") + " for "
                + saved.getJobOffer().getTitle() + " on " + date + ".";
        if (saved.getInterviewMessage() != null) {
            message += " " + saved.getInterviewMessage();
        }
        notificationService.createNotification(
                saved.getCandidate().getId(), message, update ? "INTERVIEW_UPDATED" : "INTERVIEW_INVITATION");
        emailService.sendInterviewInvitationEmail(
                saved.getCandidate().getEmail(),
                saved.getJobOffer().getTitle(),
                saved.getInterviewAt(),
                saved.getInterviewMessage(),
                update);
        auditLogService.record(
                update ? "INTERVIEW_UPDATED" : "INTERVIEW_SCHEDULED",
                "JobApplication",
                applicationId,
                "Interview scheduled for " + date,
                "INFO");
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public JobApplicationResponse cancelInterview(Long applicationId) {
        JobApplication application = getApplication(applicationId);
        if (application.getInterviewAt() == null) {
            return mapToResponse(application);
        }

        application.setInterviewAt(null);
        application.setInterviewMessage(null);
        JobApplication saved = jobApplicationRepository.save(application);
        notificationService.createNotification(
                saved.getCandidate().getId(),
                "The interview for " + saved.getJobOffer().getTitle() + " was cancelled.",
                "INTERVIEW_CANCELLED");
        emailService.sendInterviewCancellationEmail(
                saved.getCandidate().getEmail(), saved.getJobOffer().getTitle());
        auditLogService.record(
                "INTERVIEW_CANCELLED", "JobApplication", applicationId, "Cancelled scheduled interview", "WARNING");
        return mapToResponse(saved);
    }

    private JobApplication getApplication(Long applicationId) {
        return jobApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found: " + applicationId));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
