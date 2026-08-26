package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferFilterRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobOfferResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Company;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobOffer;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobStatus;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.LogSeverity;
import bg.uni.sofia.fmi.spring.hirebean.repository.CompanyRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobOfferRepository;
import bg.uni.sofia.fmi.spring.hirebean.security.JobOfferVisibilityScope;
import bg.uni.sofia.fmi.spring.hirebean.service.AuditLogService;
import bg.uni.sofia.fmi.spring.hirebean.service.JobOfferService;
import bg.uni.sofia.fmi.spring.hirebean.service.StorageService;
import jakarta.persistence.criteria.Join;
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
            JobOfferFilterRequest filterRequest, JobOfferVisibilityScope scope, Pageable pageable) {
        return jobOfferRepository
                .findAll(buildSpecification(filterRequest, scope), pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public JobOfferResponse getOfferById(Long id, JobOfferVisibilityScope scope) {
        JobOffer jobOffer = jobOfferRepository
                .findById(id)
                .filter(offer -> canViewJobOffer(offer, scope))
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
        auditLogService.record("CREATE", "JobOffer", saved.getId(), "Created job offer", LogSeverity.INFO);
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
        auditLogService.record("UPDATE", "JobOffer", saved.getId(), "Updated job offer", LogSeverity.INFO);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteOffer(Long id) {
        JobOffer jobOffer = jobOfferRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job offer not found with id: " + id));
        jobOfferRepository.delete(jobOffer);
        auditLogService.record("DELETE", "JobOffer", id, "Deleted job offer", LogSeverity.WARN);
    }

    private Specification<JobOffer> buildSpecification(JobOfferFilterRequest filter, JobOfferVisibilityScope scope) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();

            if (StringUtils.hasText(filter.getSearch())) {
                String pattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)));
            }

            if (StringUtils.hasText(filter.getLocation())) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("location")),
                                "%" + filter.getLocation().toLowerCase() + "%"));
            }

            if (filter.getMinSalary() != null) {
                predicate = criteriaBuilder.and(
                        predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("maxSalary"), filter.getMinSalary()));
            }

            if (filter.getMaxSalary() != null) {
                predicate = criteriaBuilder.and(
                        predicate, criteriaBuilder.lessThanOrEqualTo(root.get("minSalary"), filter.getMaxSalary()));
            }

            if (filter.getCompanyId() != null) {
                predicate = criteriaBuilder.and(
                        predicate, criteriaBuilder.equal(root.get("company").get("id"), filter.getCompanyId()));
            }

            if (filter.getTags() != null && !filter.getTags().isEmpty()) {
                query.distinct(true);
                Join<JobOffer, String> tagsJoin = root.join("tags");
                Set<String> normalizedTags = filter.getTags().stream()
                        .filter(StringUtils::hasText)
                        .map(String::toLowerCase)
                        .collect(java.util.stream.Collectors.toSet());
                if (!normalizedTags.isEmpty()) {
                    predicate =
                            criteriaBuilder.and(criteriaBuilder.lower(tagsJoin).in(normalizedTags), predicate);
                }
            }

            if (filter.getJobStatus() != null) {
                predicate = criteriaBuilder.and(
                        predicate, criteriaBuilder.equal(root.get("status"), filter.getJobStatus()));
            }

            if (!scope.allStatuses()) {
                var activeOnly = criteriaBuilder.equal(root.get("status"), JobStatus.ACTIVE);
                predicate = scope.managedCompanyId() == null
                        ? criteriaBuilder.and(predicate, activeOnly)
                        : criteriaBuilder.and(
                                predicate,
                                criteriaBuilder.or(
                                        activeOnly,
                                        criteriaBuilder.equal(
                                                root.get("company").get("id"), scope.managedCompanyId())));
            }

            return predicate;
        };
    }

    private boolean canViewJobOffer(JobOffer offer, JobOfferVisibilityScope scope) {
        return scope.allStatuses()
                || offer.getStatus() == JobStatus.ACTIVE
                || (scope.managedCompanyId() != null
                        && scope.managedCompanyId().equals(offer.getCompany().getId()));
    }
}
