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

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferFilterRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobOfferResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobStatus;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobType;
import bg.uni.sofia.fmi.spring.hirebean.security.JobOfferVisibilityScope;
import bg.uni.sofia.fmi.spring.hirebean.security.JwtAuthenticationFilter;
import bg.uni.sofia.fmi.spring.hirebean.security.OwnershipAuthorizationService;
import bg.uni.sofia.fmi.spring.hirebean.service.JobOfferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
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
        controllers = JobOfferController.class,
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class JobOfferControllerTest {

    private static final Long JOB_ID = 1L;
    private static final Long COMPANY_ID = 2L;
    private static final String TITLE = "foo";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobOfferService jobOfferService;

    @MockBean(name = "ownership")
    private OwnershipAuthorizationService ownership;

    @BeforeEach
    void setUp() {
        when(ownership.getJobOfferVisibilityScope(any())).thenReturn(JobOfferVisibilityScope.publicVisibility());
    }

    @Test
    @WithAnonymousUser
    void getJobOffers_anonymousUser_returnsPagedOffers() throws Exception {
        when(jobOfferService.getAllOffers(any(JobOfferFilterRequest.class), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(jobOfferResponse())));

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(TITLE))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithAnonymousUser
    void getJobOffers_withFilters_bindsThemIntoTheFilterRequest() throws Exception {
        when(jobOfferService.getAllOffers(any(JobOfferFilterRequest.class), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/jobs")
                        .param("search", "foo")
                        .param("location", "test-city")
                        .param("minSalary", "1000")
                        .param("companyId", String.valueOf(COMPANY_ID))
                        .param("jobStatus", "ACTIVE"))
                .andExpect(status().isOk());

        verify(jobOfferService).getAllOffers(any(JobOfferFilterRequest.class), any(), any(Pageable.class));
    }

    @Test
    @WithAnonymousUser
    void getJobOffers_unknownJobStatusValue_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/jobs").param("jobStatus", "NOT_A_STATUS")).andExpect(status().isBadRequest());

        verify(jobOfferService, never()).getAllOffers(any(), any(), any());
    }

    @Test
    @WithAnonymousUser
    void getJobById_visibleOffer_returnsOffer() throws Exception {
        when(jobOfferService.getOfferById(eq(JOB_ID), any())).thenReturn(jobOfferResponse());

        mockMvc.perform(get("/api/jobs/{id}", JOB_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(JOB_ID))
                .andExpect(jsonPath("$.title").value(TITLE));
    }

    @Test
    @WithAnonymousUser
    void getJobById_offerNotVisible_returnsNotFound() throws Exception {
        when(jobOfferService.getOfferById(eq(JOB_ID), any()))
                .thenThrow(new ResourceNotFoundException("Job offer not found with id: " + JOB_ID));

        mockMvc.perform(get("/api/jobs/{id}", JOB_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithAnonymousUser
    void getJobById_nonNumericId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/jobs/{id}", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void createJob_ownershipAllows_returnsCreated() throws Exception {
        when(ownership.canCreateJobOffer(any(), any(JobOfferRequest.class))).thenReturn(true);
        when(jobOfferService.createOffer(any(JobOfferRequest.class))).thenReturn(jobOfferResponse());

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobOfferRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(TITLE));
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void createJob_ownershipDenies_returnsForbidden() throws Exception {
        when(ownership.canCreateJobOffer(any(), any(JobOfferRequest.class))).thenReturn(false);

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobOfferRequest())))
                .andExpect(status().isForbidden());

        verify(jobOfferService, never()).createOffer(any());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void createJob_blankTitle_returnsBadRequest() throws Exception {
        when(ownership.canCreateJobOffer(any(), any(JobOfferRequest.class))).thenReturn(true);

        JobOfferRequest request = jobOfferRequest();
        request.setTitle("  ");

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(jobOfferService, never()).createOffer(any());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void createJob_missingCompanyId_returnsBadRequest() throws Exception {
        when(ownership.canCreateJobOffer(any(), any(JobOfferRequest.class))).thenReturn(true);

        JobOfferRequest request = jobOfferRequest();
        request.setCompanyId(null);

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(jobOfferService, never()).createOffer(any());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void updateJob_ownershipAllows_returnsUpdatedOffer() throws Exception {
        when(ownership.canUpdateJobOffer(any(), eq(JOB_ID), any(JobOfferRequest.class)))
                .thenReturn(true);
        when(jobOfferService.updateOffer(eq(JOB_ID), any(JobOfferRequest.class)))
                .thenReturn(jobOfferResponse());

        mockMvc.perform(put("/api/jobs/{id}", JOB_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobOfferRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(TITLE));
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void updateJob_ownershipDenies_returnsForbidden() throws Exception {
        when(ownership.canUpdateJobOffer(any(), eq(JOB_ID), any(JobOfferRequest.class)))
                .thenReturn(false);

        mockMvc.perform(put("/api/jobs/{id}", JOB_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobOfferRequest())))
                .andExpect(status().isForbidden());

        verify(jobOfferService, never()).updateOffer(any(), any());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void deleteJob_ownershipAllows_returnsNoContent() throws Exception {
        when(ownership.canManageJobOffer(any(), eq(JOB_ID))).thenReturn(true);

        mockMvc.perform(delete("/api/jobs/{id}", JOB_ID)).andExpect(status().isNoContent());

        verify(jobOfferService).deleteOffer(JOB_ID);
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void deleteJob_ownershipDenies_returnsForbidden() throws Exception {
        when(ownership.canManageJobOffer(any(), eq(JOB_ID))).thenReturn(false);

        mockMvc.perform(delete("/api/jobs/{id}", JOB_ID)).andExpect(status().isForbidden());

        verify(jobOfferService, never()).deleteOffer(any());
    }

    private JobOfferResponse jobOfferResponse() {
        return JobOfferResponse.builder()
                .id(JOB_ID)
                .title(TITLE)
                .description("test-description")
                .location("test-city")
                .jobType(JobType.FULL_TIME)
                .minSalary(BigDecimal.valueOf(1000))
                .maxSalary(BigDecimal.valueOf(2000))
                .status(JobStatus.ACTIVE)
                .companyId(COMPANY_ID)
                .companyName("bar")
                .tags(new HashSet<>(Set.of("java")))
                .build();
    }

    private JobOfferRequest jobOfferRequest() {
        JobOfferRequest request = new JobOfferRequest();
        request.setTitle(TITLE);
        request.setDescription("test-description");
        request.setLocation("test-city");
        request.setJobType(JobType.FULL_TIME);
        request.setMinSalary(BigDecimal.valueOf(1000));
        request.setMaxSalary(BigDecimal.valueOf(2000));
        request.setStatus(JobStatus.ACTIVE);
        request.setCompanyId(COMPANY_ID);
        request.setTags(new HashSet<>(Set.of("java")));
        return request;
    }
}
