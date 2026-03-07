package bg.uni.sofia.fmi.spring.hirebean.service;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.CompanyRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.CompanyResponse;
import java.util.List;

public interface CompanyService {

    List<CompanyResponse> getAllCompanies();

    CompanyResponse getCompanyById(Long id);

    CompanyResponse createCompany(CompanyRequest request);

}
