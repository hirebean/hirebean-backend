package bg.uni.sofia.fmi.spring.hirebean.controller;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferFilterRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobOfferResponse;
import bg.uni.sofia.fmi.spring.hirebean.security.OwnershipAuthorizationService;
import bg.uni.sofia.fmi.spring.hirebean.service.JobOfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobOfferController {

    private final OwnershipAuthorizationService ownershipAuthorizationService;
    private final JobOfferService jobOfferService;

    @GetMapping
    public ResponseEntity<Page<JobOfferResponse>> getJobOffers(
            JobOfferFilterRequest filterRequest, Pageable pageable, Authentication authentication) {
        return ResponseEntity.ok(jobOfferService.getAllOffers(
                filterRequest, ownershipAuthorizationService.getJobOfferVisibilityScope(authentication), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobOfferResponse> getJobById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(jobOfferService.getOfferById(
                id, ownershipAuthorizationService.getJobOfferVisibilityScope(authentication)));
    }

    @PostMapping
    @PreAuthorize("@ownership.canCreateJobOffer(authentication, #request)")
    public ResponseEntity<JobOfferResponse> createJob(@Valid @RequestBody JobOfferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobOfferService.createOffer(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ownership.canUpdateJobOffer(authentication, #id, #request)")
    public ResponseEntity<JobOfferResponse> updateJob(
            @PathVariable Long id, @Valid @RequestBody JobOfferRequest request) {
        return ResponseEntity.ok(jobOfferService.updateOffer(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownership.canManageJobOffer(authentication, #id)")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobOfferService.deleteOffer(id);
        return ResponseEntity.noContent().build();
    }
}
