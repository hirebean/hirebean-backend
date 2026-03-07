package bg.uni.sofia.fmi.spring.hirebean.service;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobOfferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobOfferService {

    Page<JobOfferResponse> getAllOffers(Pageable pageable);

    JobOfferResponse getOfferById(Long id);

    JobOfferResponse createOffer(JobOfferRequest request);

}
