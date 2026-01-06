package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.CompanyRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.CompanyResponse;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Company;
import bg.uni.sofia.fmi.spring.hirebean.repository.CompanyRepository;
import bg.uni.sofia.fmi.spring.hirebean.service.CompanyService;
import bg.uni.sofia.fmi.spring.hirebean.service.S3Service;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
  private final S3Service s3Service;

  private final CompanyRepository companyRepository;

  private CompanyResponse mapToResponse(Company company) {
    return CompanyResponse.builder()
        .id(company.getId())
        .name(company.getName())
        .description(company.getDescription())
        .websiteUrl(company.getWebsiteUrl())
        .logoUrl(s3Service.getPublicUrl(company.getLogoUrl()))
        .location(company.getLocation())
        .createdAt(company.getCreatedAt())
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CompanyResponse> getAllCompanies() {
    return companyRepository.findAll().stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Override
  public CompanyResponse getCompanyById(Long id) {
    Company company =
        companyRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));
    return mapToResponse(company);
  }

  @Override
  @Transactional
  public CompanyResponse createCompany(CompanyRequest request) {
    Company company =
        Company.builder()
            .name(request.getName())
            .description(request.getDescription())
            .websiteUrl(request.getWebsiteUrl())
            .logoUrl(request.getLogoUrl())
            .location(request.getLocation())
            .build();
    return mapToResponse(companyRepository.save(company));
  }
}
