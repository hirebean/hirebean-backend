package bg.uni.sofia.fmi.spring.hirebean.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bg.uni.sofia.fmi.spring.hirebean.dto.response.LogResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.LogSeverity;
import bg.uni.sofia.fmi.spring.hirebean.security.JwtAuthenticationFilter;
import bg.uni.sofia.fmi.spring.hirebean.security.OwnershipAuthorizationService;
import bg.uni.sofia.fmi.spring.hirebean.service.AuditLogService;
import java.time.LocalDateTime;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = AdminLogController.class,
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class AdminLogControllerTest {

    private static final Long LOG_ID = 1L;
    private static final Long ACTOR_ID = 2L;
    private static final Long ENTITY_ID = 3L;
    private static final String EMAIL = "test@test.com";
    private static final String ACTION = "CREATE";
    private static final String ENTITY = "TestEntity";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean(name = "ownership")
    private OwnershipAuthorizationService ownership;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLogs_admin_returnsPagedLogs() throws Exception {
        when(auditLogService.searchLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(logResponse())));

        mockMvc.perform(get("/api/admin/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(LOG_ID))
                .andExpect(jsonPath("$.content[0].action").value(ACTION))
                .andExpect(jsonPath("$.content[0].severity").value("WARN"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLogs_noFilters_passesNullsToService() throws Exception {
        when(auditLogService.searchLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/logs")).andExpect(status().isOk());

        verify(auditLogService)
                .searchLogs(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLogs_allFiltersProvided_passesThemToService() throws Exception {
        when(auditLogService.searchLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/logs")
                        .param("actorId", String.valueOf(ACTOR_ID))
                        .param("action", ACTION)
                        .param("entity", ENTITY)
                        .param("severity", "WARN")
                        .param("from", "2026-01-01T00:00:00")
                        .param("to", "2026-12-31T23:59:59"))
                .andExpect(status().isOk());

        verify(auditLogService)
                .searchLogs(
                        eq(ACTOR_ID),
                        eq(ACTION),
                        eq(ENTITY),
                        eq(LogSeverity.WARN),
                        eq(LocalDateTime.of(2026, 1, 1, 0, 0, 0)),
                        eq(LocalDateTime.of(2026, 12, 31, 23, 59, 59)),
                        any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLogs_unknownSeverityValue_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/logs").param("severity", "NOT_A_SEVERITY"))
                .andExpect(status().isBadRequest());

        verify(auditLogService, never()).searchLogs(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLogs_lowercaseSeverityValue_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/logs").param("severity", "warn")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLogs_malformedDate_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/logs").param("from", "yesterday")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void getLogs_nonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/logs")).andExpect(status().isForbidden());

        verify(auditLogService, never()).searchLogs(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithAnonymousUser
    void getLogs_anonymousUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/logs")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLogById_existingLog_returnsLog() throws Exception {
        when(auditLogService.getLogById(LOG_ID)).thenReturn(logResponse());

        mockMvc.perform(get("/api/admin/logs/{id}", LOG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(LOG_ID))
                .andExpect(jsonPath("$.actorEmail").value(EMAIL));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLogById_logDoesNotExist_returnsNotFound() throws Exception {
        when(auditLogService.getLogById(LOG_ID)).thenThrow(new ResourceNotFoundException("Log not found"));

        mockMvc.perform(get("/api/admin/logs/{id}", LOG_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLogById_nonNumericId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/logs/{id}", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void getLogById_nonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/logs/{id}", LOG_ID)).andExpect(status().isForbidden());

        verify(auditLogService, never()).getLogById(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteLog_admin_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/logs/{id}", LOG_ID)).andExpect(status().isNoContent());

        verify(auditLogService).deleteLog(LOG_ID);
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void deleteLog_nonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/admin/logs/{id}", LOG_ID)).andExpect(status().isForbidden());

        verify(auditLogService, never()).deleteLog(any());
    }

    private LogResponse logResponse() {
        return LogResponse.builder()
                .id(LOG_ID)
                .action(ACTION)
                .entity(ENTITY)
                .entityId(ENTITY_ID)
                .actorId(ACTOR_ID)
                .actorEmail(EMAIL)
                .details("foo")
                .severity(LogSeverity.WARN)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
