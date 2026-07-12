package bg.uni.sofia.fmi.spring.hirebean.controller;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobOfferResponse;
import bg.uni.sofia.fmi.spring.hirebean.service.JobOfferService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobOfferController {

    private final JobOfferService jobOfferService;

    @GetMapping
    public ResponseEntity<Page<JobOfferResponse>> getAllJobs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) BigDecimal maxSalary,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Set<String> tags,
            Pageable pageable) {
        return ResponseEntity.ok(
                jobOfferService.getAllOffers(search, location, minSalary, maxSalary, companyId, tags, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobOfferResponse> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobOfferService.getOfferById(id));
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
