package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.InterviewInvitationRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobApplicationRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.ReviewApplicationRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobApplicationResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.exception.application.JobApplicationAlreadyExistsException;
import bg.uni.sofia.fmi.spring.hirebean.exception.job.JobOfferClosedException;
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
import bg.uni.sofia.fmi.spring.hirebean.service.JobApplicationService;
import bg.uni.sofia.fmi.spring.hirebean.service.NotificationService;
import bg.uni.sofia.fmi.spring.hirebean.service.StorageService;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    private static final DateTimeFormatter INTERVIEW_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final String APPLICATION_UNIQUE_CONSTRAINT = "uk_job_applications_candidate_job_offer";

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
        JobOffer jobOffer = getActiveJobOffer(request.getJobOfferId());
        ensureNotAlreadyApplied(candidateId, jobOffer.getId());
        User candidate = getCandidate(candidateId);
        JobApplication saved = saveNewApplication(candidate, jobOffer, request.getCoverLetter());
        attachCv(saved, cvFile);

        userRepository
                .findAllByCompanyId(jobOffer.getCompany().getId())
                .forEach(employee -> notificationService.createNotification(
                        employee.getId(),
                        "New application for " + jobOffer.getTitle() + " from " + candidate.getEmail(),
                        "APPLICATION_CREATED"));

        auditLogService.record(
                "APPLY", "JobApplication", saved.getId(), candidateId, "Candidate applied for job", LogSeverity.INFO);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasApplied(Long candidateId, Long jobOfferId) {
        return jobApplicationRepository.existsByCandidateIdAndJobOfferId(candidateId, jobOfferId);
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
                "STATUS_UPDATE",
                "JobApplication",
                applicationId,
                "Updated application status to " + status,
                LogSeverity.INFO);

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
                LogSeverity.INFO);
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
                LogSeverity.INFO);
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
                "INTERVIEW_CANCELLED",
                "JobApplication",
                applicationId,
                "Cancelled scheduled interview",
                LogSeverity.WARN);
        return mapToResponse(saved);
    }

    private JobApplication getApplication(Long applicationId) {
        return jobApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found: " + applicationId));
    }

    private JobOffer getActiveJobOffer(Long jobOfferId) {
        JobOffer jobOffer = jobOfferRepository
                .findById(jobOfferId)
                .orElseThrow(() -> new ResourceNotFoundException("Job offer not found with id: " + jobOfferId));
        if (jobOffer.getStatus() != JobStatus.ACTIVE) {
            throw new JobOfferClosedException(
                    "Cannot apply to job offer with id: " + jobOfferId + " because it is not active");
        }
        return jobOffer;
    }

    private void ensureNotAlreadyApplied(Long candidateId, Long jobOfferId) {
        if (jobApplicationRepository.existsByCandidateIdAndJobOfferId(candidateId, jobOfferId)) {
            throw new JobApplicationAlreadyExistsException();
        }
    }

    private User getCandidate(Long candidateId) {
        return userRepository
                .findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + candidateId));
    }

    private JobApplication saveNewApplication(User candidate, JobOffer jobOffer, String coverLetter) {
        JobApplication application = JobApplication.builder()
                .candidate(candidate)
                .jobOffer(jobOffer)
                .coverLetter(coverLetter)
                .status(ApplicationStatus.PENDING)
                .build();
        try {
            return jobApplicationRepository.saveAndFlush(application);
        } catch (DataIntegrityViolationException exception) {
            if (violatesApplicationUniqueConstraint(exception)) {
                throw new JobApplicationAlreadyExistsException();
            }
            throw exception;
        }
    }

    private boolean violatesApplicationUniqueConstraint(Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation
                    && APPLICATION_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintViolation.getConstraintName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void attachCv(JobApplication application, MultipartFile cvFile) {
        if (cvFile != null && !cvFile.isEmpty()) {
            application.setCvKey(storageService.uploadFile(cvFile, "cvs"));
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
