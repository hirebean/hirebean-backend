package bg.uni.sofia.fmi.spring.hirebean.controller;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobApplicationRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobApplicationResponse;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.ApplicationStatus;
import bg.uni.sofia.fmi.spring.hirebean.service.JobApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class JobApplicationController {
    private final JobApplicationService jobApplicationService;

    // multipart/form-data
    @PostMapping(value = "/apply/{candidateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ownership.canApplyAsCandidate(authentication, #candidateId)")
    public ResponseEntity<JobApplicationResponse> apply(
            @PathVariable Long candidateId,
            @Valid @RequestPart("data") JobApplicationRequest request,
            @RequestPart(value = "cv", required = true) MultipartFile cvFile) {

        return ResponseEntity.ok(jobApplicationService.apply(candidateId, request, cvFile));
    }

    @GetMapping("/candidate/{candidateId}")
    @PreAuthorize("@ownership.canViewCandidateApplications(authentication, #candidateId)")
    public ResponseEntity<List<JobApplicationResponse>> getApplicationsByCandidateId(@PathVariable Long candidateId) {
        return ResponseEntity.ok(jobApplicationService.getApplicationsForCandidate(candidateId));
    }

    @GetMapping("/job/{jobOfferId}")
    @PreAuthorize("@ownership.canViewJobApplications(authentication, #jobOfferId)")
    public ResponseEntity<List<JobApplicationResponse>> getByJobOffer(@PathVariable Long jobOfferId) {
        return ResponseEntity.ok(jobApplicationService.getApplicationsForJobOffer(jobOfferId));
    }

    @PatchMapping("/{applicationId}/status")
    @PreAuthorize("@ownership.canManageApplication(authentication, #applicationId)")
    public ResponseEntity<JobApplicationResponse> updateStatus(
            @PathVariable Long applicationId, @RequestParam ApplicationStatus status) {
        return ResponseEntity.ok(jobApplicationService.updateStatus(applicationId, status));
    }
}
