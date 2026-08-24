package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobOfferResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Company;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobOffer;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobStatus;
import bg.uni.sofia.fmi.spring.hirebean.repository.CompanyRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobOfferRepository;
import bg.uni.sofia.fmi.spring.hirebean.service.AuditLogService;
import bg.uni.sofia.fmi.spring.hirebean.service.JobOfferService;
import bg.uni.sofia.fmi.spring.hirebean.service.StorageService;
import jakarta.persistence.criteria.Join;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class JobOfferServiceImpl implements JobOfferService {

    private final JobOfferRepository jobOfferRepository;
    private final CompanyRepository companyRepository;
    private final StorageService storageService;
    private final AuditLogService auditLogService;

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
                .companyId(jobOffer.getCompany().getId())
                .companyName(jobOffer.getCompany().getName())
                .companyLogoUrl(
                        storageService.getPublicUrl(jobOffer.getCompany().getLogoUrl()))
                .tags(jobOffer.getTags())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobOfferResponse> getAllOffers(
            String search,
            String location,
            BigDecimal minSalary,
            BigDecimal maxSalary,
            Long companyId,
            Set<String> tags,
            Pageable pageable) {
        return jobOfferRepository
                .findAll(buildSpecification(search, location, minSalary, maxSalary, companyId, tags), pageable)
                .map(this::mapToResponse);
    }

    @Override
    public JobOfferResponse getOfferById(Long id) {
        JobOffer jobOffer = jobOfferRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job offer not found with id: " + id));
        return mapToResponse(jobOffer);
    }

    @Override
    @Transactional
    public JobOfferResponse createOffer(JobOfferRequest request) {
        Company company = companyRepository
                .findById(request.getCompanyId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Company not found with id: " + request.getCompanyId()));

        JobOffer jobOffer = JobOffer.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .minSalary(request.getMinSalary())
                .maxSalary(request.getMaxSalary())
                .jobType(request.getJobType())
                .status(request.getStatus() != null ? request.getStatus() : JobStatus.ACTIVE)
                .tags(request.getTags() != null ? request.getTags() : new HashSet<>())
                .company(company)
                .build();

        JobOffer saved = jobOfferRepository.save(jobOffer);
        auditLogService.record("CREATE", "JobOffer", saved.getId(), "Created job offer", "INFO");
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public JobOfferResponse updateOffer(Long id, JobOfferRequest request) {
        JobOffer jobOffer = jobOfferRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job offer not found with id: " + id));

        Company company = companyRepository
                .findById(request.getCompanyId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Company not found with id: " + request.getCompanyId()));

        jobOffer.setTitle(request.getTitle());
        jobOffer.setDescription(request.getDescription());
        jobOffer.setLocation(request.getLocation());
        jobOffer.setMinSalary(request.getMinSalary());
        jobOffer.setMaxSalary(request.getMaxSalary());
        jobOffer.setJobType(request.getJobType());
        jobOffer.setStatus(request.getStatus() != null ? request.getStatus() : JobStatus.ACTIVE);
        jobOffer.setTags(request.getTags() != null ? request.getTags() : new HashSet<>());
        jobOffer.setCompany(company);

        JobOffer saved = jobOfferRepository.save(jobOffer);
        auditLogService.record("UPDATE", "JobOffer", saved.getId(), "Updated job offer", "INFO");
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteOffer(Long id) {
        JobOffer jobOffer = jobOfferRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job offer not found with id: " + id));
        jobOfferRepository.delete(jobOffer);
        auditLogService.record("DELETE", "JobOffer", id, "Deleted job offer", "WARN");
    }

    private Specification<JobOffer> buildSpecification(
            String search,
            String location,
            BigDecimal minSalary,
            BigDecimal maxSalary,
            Long companyId,
            Set<String> tags) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)));
            }

            if (StringUtils.hasText(location)) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("location")), "%" + location.toLowerCase() + "%"));
            }

            if (minSalary != null) {
                predicate = criteriaBuilder.and(
                        predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("maxSalary"), minSalary));
            }

            if (maxSalary != null) {
                predicate = criteriaBuilder.and(
                        predicate, criteriaBuilder.lessThanOrEqualTo(root.get("minSalary"), maxSalary));
            }

            if (companyId != null) {
                predicate = criteriaBuilder.and(
                        predicate, criteriaBuilder.equal(root.get("company").get("id"), companyId));
            }

            if (tags != null && !tags.isEmpty()) {
                query.distinct(true);
                Join<JobOffer, String> tagsJoin = root.join("tags");
                Set<String> normalizedTags = tags.stream()
                        .filter(StringUtils::hasText)
                        .map(String::toLowerCase)
                        .collect(java.util.stream.Collectors.toSet());
                if (!normalizedTags.isEmpty()) {
                    predicate =
                            criteriaBuilder.and(criteriaBuilder.lower(tagsJoin).in(normalizedTags), predicate);
                }
            }

            return predicate;
        };
    }
}
