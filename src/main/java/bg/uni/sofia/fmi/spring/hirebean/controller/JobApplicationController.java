package bg.uni.sofia.fmi.spring.hirebean.controller;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobApplicationRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobApplicationResponse;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.ApplicationStatus;
import bg.uni.sofia.fmi.spring.hirebean.service.JobApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class JobApplicationController {
    private final JobApplicationService jobApplicationService;

    @PostMapping("/apply/{candidateId}")
    public ResponseEntity<JobApplicationResponse> apply(
            @PathVariable Long candidateId, @Valid @RequestBody JobApplicationRequest request) {
        return ResponseEntity.ok(jobApplicationService.apply(candidateId, request));
    }

    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<JobApplicationResponse>> getApplicationsByCandidateId(@PathVariable Long candidateId) {
        return ResponseEntity.ok(jobApplicationService.getApplicationsForCandidate(candidateId));
    }

    @GetMapping("/job/{jobOfferId}")
    public ResponseEntity<List<JobApplicationResponse>> getByJobOffer(@PathVariable Long jobOfferId) {
        return ResponseEntity.ok(jobApplicationService.getApplicationsForJobOffer(jobOfferId));
    }

    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<JobApplicationResponse> updateStatus(
            @PathVariable Long applicationId, @RequestParam ApplicationStatus status) {
        return ResponseEntity.ok(jobApplicationService.updateStatus(applicationId, status));
    }
}
