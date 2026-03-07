package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobOfferResponse;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Company;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobOffer;
import bg.uni.sofia.fmi.spring.hirebean.repository.CompanyRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobOfferRepository;
import bg.uni.sofia.fmi.spring.hirebean.service.JobOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobOfferServiceImpl implements JobOfferService {

    private final JobOfferRepository jobOfferRepository;
    private final CompanyRepository companyRepository;

    private JobOfferResponse mapToResponse(JobOffer jobOffer) {

        return JobOfferResponse.builder()
                .id(jobOffer.getId())
                .title(jobOffer.getTitle())
                .description(jobOffer.getDescription())
                .location(jobOffer.getLocation())
                .minSalary(jobOffer.getMinSalary())
                .maxSalary(jobOffer.getMaxSalary())
                .jobType(jobOffer.getJobType())
                .status(jobOffer.getStatus())
                .createdAt(jobOffer.getCreatedAt())
                .companyId(jobOffer.getCompany()
                        .getId())
                .companyName(jobOffer.getCompany()
                        .getName())
                .companyLogoUrl(jobOffer.getDescription())
                .tags(jobOffer.getTags())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobOfferResponse> getAllOffers(Pageable pageable) {
        return jobOfferRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public JobOfferResponse getOfferById(Long id) {
        JobOffer jobOffer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job offer not found with id: " + id));
        return mapToResponse(jobOffer);
    }

    @Override
    @Transactional
    public JobOfferResponse createOffer(JobOfferRequest request) {
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + request.getCompanyId()));

        JobOffer jobOffer = JobOffer.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .minSalary(request.getMinSalary())
                .maxSalary(request.getMaxSalary())
                .jobType(request.getJobType())
                .status(request.getStatus())
                .tags(request.getTags())
                .company(company)
                .build();

        return mapToResponse(jobOfferRepository.save(jobOffer));
    }

}
