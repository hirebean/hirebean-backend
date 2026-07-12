package bg.uni.sofia.fmi.spring.hirebean.service;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobOfferResponse;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobOfferService {

    Page<JobOfferResponse> getAllOffers(
            String search,
            String location,
            BigDecimal minSalary,
            BigDecimal maxSalary,
            Long companyId,
            Set<String> tags,
            Pageable pageable);

    JobOfferResponse getOfferById(Long id);

    JobOfferResponse createOffer(JobOfferRequest request);

    JobOfferResponse updateOffer(Long id, JobOfferRequest request);

    void deleteOffer(Long id);
}
