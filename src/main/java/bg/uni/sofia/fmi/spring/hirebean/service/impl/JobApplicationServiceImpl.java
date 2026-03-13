package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobApplicationRequest;
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
import bg.uni.sofia.fmi.spring.hirebean.service.JobApplicationService;
import bg.uni.sofia.fmi.spring.hirebean.service.S3Service;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;

    private final UserRepository userRepository;

    private final JobOfferRepository jobOfferRepository;

    private final S3Service s3Service;

    private JobApplicationResponse mapToResponse(JobApplication application) {
        return JobApplicationResponse.builder()
                .id(application.getId())
                .candidateId(application.getCandidate().getId())
                .candidateEmail(application.getCandidate().getEmail())
                .jobOfferId(application.getJobOffer().getId())
                .jobTitle(application.getJobOffer().getTitle())
                .coverLetter(application.getCoverLetter())
                .cvUrl(s3Service.getPresignedUrl(application.getCvKey()))
                .status(application.getStatus())
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

        // Upload CV to S3 under "cvs/" folder
        String cvKey = null;
        if (cvFile != null && !cvFile.isEmpty()) {
            cvKey = s3Service.uploadFile(cvFile, "cvs");
        }

        JobApplication application = JobApplication.builder()
                .candidate(candidate)
                .jobOffer(jobOffer)
                .coverLetter(request.getCoverLetter())
                .cvKey(cvKey)
                .status(ApplicationStatus.PENDING)
                .build();

        return mapToResponse(jobApplicationRepository.save(application));
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

        return mapToResponse(jobApplication);
    }
}
