package bg.uni.sofia.fmi.spring.hirebean.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.CompanyRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.CompanyResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.security.JwtAuthenticationFilter;
import bg.uni.sofia.fmi.spring.hirebean.security.OwnershipAuthorizationService;
import bg.uni.sofia.fmi.spring.hirebean.service.CompanyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = CompanyController.class,
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class CompanyControllerTest {

    private static final Long COMPANY_ID = 1L;
    private static final String COMPANY_NAME = "foo";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyService companyService;

    @MockBean(name = "ownership")
    private OwnershipAuthorizationService ownership;

    @Test
    @WithAnonymousUser
    void getAllCompanies_anonymousUser_returnsPagedCompanies() throws Exception {
        when(companyService.getAllCompanies(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(companyResponse())));

        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value(COMPANY_NAME))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithAnonymousUser
    void getAllCompanies_withSearch_passesSearchToService() throws Exception {
        when(companyService.getAllCompanies(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/companies").param("search", "foo")).andExpect(status().isOk());

        verify(companyService).getAllCompanies(eq("foo"), any(Pageable.class));
    }

    @Test
    @WithAnonymousUser
    void getCompanyById_existingCompany_returnsCompany() throws Exception {
        when(companyService.getCompanyById(COMPANY_ID)).thenReturn(companyResponse());

        mockMvc.perform(get("/api/companies/{id}", COMPANY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(COMPANY_ID))
                .andExpect(jsonPath("$.name").value(COMPANY_NAME));
    }

    @Test
    @WithAnonymousUser
    void getCompanyById_companyDoesNotExist_returnsNotFound() throws Exception {
        when(companyService.getCompanyById(COMPANY_ID)).thenThrow(new ResourceNotFoundException("Company not found"));

        mockMvc.perform(get("/api/companies/{id}", COMPANY_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithAnonymousUser
    void getCompanyById_nonNumericId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/companies/{id}", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void createCompany_ownershipAllows_returnsCreated() throws Exception {
        when(ownership.canCreateCompany(any())).thenReturn(true);
        when(companyService.createCompany(any(CompanyRequest.class))).thenReturn(companyResponse());

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(companyRequest(COMPANY_NAME))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(COMPANY_NAME));
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void createCompany_ownershipDenies_returnsForbidden() throws Exception {
        when(ownership.canCreateCompany(any())).thenReturn(false);

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(companyRequest(COMPANY_NAME))))
                .andExpect(status().isForbidden());

        verify(companyService, never()).createCompany(any());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void createCompany_blankName_returnsBadRequest() throws Exception {
        when(ownership.canCreateCompany(any())).thenReturn(true);

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(companyRequest("  "))))
                .andExpect(status().isBadRequest());

        verify(companyService, never()).createCompany(any());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void updateCompany_ownershipAllows_returnsUpdatedCompany() throws Exception {
        when(ownership.canManageCompany(any(), eq(COMPANY_ID))).thenReturn(true);
        when(companyService.updateCompany(eq(COMPANY_ID), any(CompanyRequest.class)))
                .thenReturn(companyResponse());

        mockMvc.perform(put("/api/companies/{id}", COMPANY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(companyRequest(COMPANY_NAME))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(COMPANY_NAME));
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void updateCompany_ownershipDenies_returnsForbidden() throws Exception {
        when(ownership.canManageCompany(any(), eq(COMPANY_ID))).thenReturn(false);

        mockMvc.perform(put("/api/companies/{id}", COMPANY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(companyRequest(COMPANY_NAME))))
                .andExpect(status().isForbidden());

        verify(companyService, never()).updateCompany(any(), any());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void deleteCompany_ownershipAllows_returnsNoContent() throws Exception {
        when(ownership.canManageCompany(any(), eq(COMPANY_ID))).thenReturn(true);

        mockMvc.perform(delete("/api/companies/{id}", COMPANY_ID)).andExpect(status().isNoContent());

        verify(companyService).deleteCompany(COMPANY_ID);
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void deleteCompany_ownershipDenies_returnsForbidden() throws Exception {
        when(ownership.canManageCompany(any(), eq(COMPANY_ID))).thenReturn(false);

        mockMvc.perform(delete("/api/companies/{id}", COMPANY_ID)).andExpect(status().isForbidden());

        verify(companyService, never()).deleteCompany(any());
    }

    private CompanyResponse companyResponse() {
        return CompanyResponse.builder()
                .id(COMPANY_ID)
                .name(COMPANY_NAME)
                .description("test-description")
                .websiteUrl("https://foo.test")
                .location("test-city")
                .build();
    }

    private CompanyRequest companyRequest(String name) {
        CompanyRequest request = new CompanyRequest();
        request.setName(name);
        request.setDescription("test-description");
        request.setWebsiteUrl("https://foo.test");
        request.setLocation("test-city");
        return request;
    }
}
