package bg.uni.sofia.fmi.spring.hirebean.controller;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobOfferResponse;
import bg.uni.sofia.fmi.spring.hirebean.service.JobOfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobOfferController {

    private final JobOfferService jobOfferService;

    @GetMapping
    public ResponseEntity<Page<JobOfferResponse>> getAllJobs(Pageable pageable) {
        return ResponseEntity.ok(jobOfferService.getAllOffers(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobOfferResponse> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobOfferService.getOfferById(id));
    }

    @PostMapping
    public ResponseEntity<JobOfferResponse> createJob(@Valid @RequestBody JobOfferRequest request) {
        return ResponseEntity.ok(jobOfferService.createOffer(request));
    }

}
