package bg.uni.sofia.fmi.spring.hirebean.service;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferFilterRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobOfferResponse;
import bg.uni.sofia.fmi.spring.hirebean.security.JobOfferVisibilityScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobOfferService {

    Page<JobOfferResponse> getAllOffers(
            JobOfferFilterRequest filterRequest, JobOfferVisibilityScope scope, Pageable pageable);

    JobOfferResponse getOfferById(Long id, JobOfferVisibilityScope scope);

    JobOfferResponse createOffer(JobOfferRequest request);

    JobOfferResponse updateOffer(Long id, JobOfferRequest request);

    void deleteOffer(Long id);
}
