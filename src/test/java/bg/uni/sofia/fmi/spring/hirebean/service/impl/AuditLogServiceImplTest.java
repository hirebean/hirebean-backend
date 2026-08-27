package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bg.uni.sofia.fmi.spring.hirebean.dto.response.LogResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Log;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.LogSeverity;
import bg.uni.sofia.fmi.spring.hirebean.repository.LogRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    private static final Long LOG_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final Long ENTITY_ID = 3L;
    private static final String EMAIL = "test@test.com";
    private static final String ACTION = "CREATE";
    private static final String ENTITY = "TestEntity";
    private static final String DETAILS = "foo";

    @Mock
    private LogRepository logRepository;

    @Mock
    private UserRepository userRepository;

    private final User user = createUser();
    private final Log log = createLog();

    private AuditLogServiceImpl auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(logRepository, userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void record_explicitActorId_storesThatActorSnapshot() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        auditLogService.record(ACTION, ENTITY, ENTITY_ID, USER_ID, DETAILS, LogSeverity.INFO);

        Log saved = captureSavedLog();
        assertThat(saved.getActorId()).isEqualTo(USER_ID);
        assertThat(saved.getActorEmail()).isEqualTo(EMAIL);
        assertThat(saved.getAction()).isEqualTo(ACTION);
        assertThat(saved.getEntity()).isEqualTo(ENTITY);
        assertThat(saved.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(saved.getDetails()).isEqualTo(DETAILS);
        assertThat(saved.getSeverity()).isEqualTo(LogSeverity.INFO);
    }

    @Test
    void record_explicitActorIdThatDoesNotExist_storesNoActor() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        auditLogService.record(ACTION, ENTITY, ENTITY_ID, USER_ID, DETAILS, LogSeverity.INFO);

        Log saved = captureSavedLog();
        assertThat(saved.getActorId()).isNull();
        assertThat(saved.getActorEmail()).isNull();
    }

    @Test
    void record_noActorIdButAuthenticatedUser_resolvesActorFromSecurityContext() {
        stubAuthenticatedUser();

        auditLogService.record(ACTION, ENTITY, ENTITY_ID, DETAILS, LogSeverity.INFO);

        Log saved = captureSavedLog();
        assertThat(saved.getActorId()).isEqualTo(USER_ID);
        assertThat(saved.getActorEmail()).isEqualTo(EMAIL);
    }

    @Test
    void record_noActorIdAndNoAuthentication_storesNoActor() {
        auditLogService.record(ACTION, ENTITY, ENTITY_ID, DETAILS, LogSeverity.INFO);

        Log saved = captureSavedLog();
        assertThat(saved.getActorId()).isNull();
        assertThat(saved.getActorEmail()).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void record_anonymousAuthentication_storesNoActor() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        SecurityContextHolder.setContext(context);

        auditLogService.record(ACTION, ENTITY, ENTITY_ID, DETAILS, LogSeverity.INFO);

        Log saved = captureSavedLog();
        assertThat(saved.getActorId()).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void record_authenticatedUserNotInDatabase_storesNoActor() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(EMAIL, null, List.of()));
        SecurityContextHolder.setContext(context);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        auditLogService.record(ACTION, ENTITY, ENTITY_ID, DETAILS, LogSeverity.INFO);

        Log saved = captureSavedLog();
        assertThat(saved.getActorId()).isNull();
    }

    @Test
    void record_nullSeverity_defaultsToInfo() {
        auditLogService.record(ACTION, ENTITY, ENTITY_ID, DETAILS, null);

        assertThat(captureSavedLog().getSeverity()).isEqualTo(LogSeverity.INFO);
    }

    @Test
    void record_warnSeverity_keepsThatSeverity() {
        auditLogService.record(ACTION, ENTITY, ENTITY_ID, DETAILS, LogSeverity.WARN);

        assertThat(captureSavedLog().getSeverity()).isEqualTo(LogSeverity.WARN);
    }

    @Test
    void searchLogs_matchingLogs_returnsMappedPage() {
        when(logRepository.findAll(ArgumentMatchers.<Specification<Log>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        Page<LogResponse> result = auditLogService.searchLogs(null, null, null, null, null, null, Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
        LogResponse response = result.getContent().get(0);
        assertThat(response.getId()).isEqualTo(LOG_ID);
        assertThat(response.getAction()).isEqualTo(ACTION);
        assertThat(response.getEntity()).isEqualTo(ENTITY);
        assertThat(response.getActorId()).isEqualTo(USER_ID);
        assertThat(response.getActorEmail()).isEqualTo(EMAIL);
        assertThat(response.getSeverity()).isEqualTo(LogSeverity.WARN);
    }

    @Test
    void searchLogs_allFiltersProvided_stillDelegatesToRepository() {
        when(logRepository.findAll(ArgumentMatchers.<Specification<Log>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<LogResponse> result = auditLogService.searchLogs(
                USER_ID,
                ACTION,
                ENTITY,
                LogSeverity.WARN,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
        verify(logRepository).findAll(ArgumentMatchers.<Specification<Log>>any(), any(Pageable.class));
    }

    @Test
    void getLogById_existingLog_returnsMappedLog() {
        when(logRepository.findById(LOG_ID)).thenReturn(Optional.of(log));

        LogResponse response = auditLogService.getLogById(LOG_ID);

        assertThat(response.getId()).isEqualTo(LOG_ID);
        assertThat(response.getDetails()).isEqualTo(DETAILS);
    }

    @Test
    void getLogById_logDoesNotExist_throwsResourceNotFound() {
        when(logRepository.findById(LOG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditLogService.getLogById(LOG_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(LOG_ID));
    }

    @Test
    void deleteLog_existingLog_deletesIt() {
        when(logRepository.findById(LOG_ID)).thenReturn(Optional.of(log));

        auditLogService.deleteLog(LOG_ID);

        verify(logRepository).delete(log);
    }

    @Test
    void deleteLog_logDoesNotExist_throwsResourceNotFound() {
        when(logRepository.findById(LOG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditLogService.deleteLog(LOG_ID)).isInstanceOf(ResourceNotFoundException.class);

        verify(logRepository, never()).delete(any(Log.class));
    }

    private Log captureSavedLog() {
        ArgumentCaptor<Log> captor = ArgumentCaptor.forClass(Log.class);
        verify(logRepository).save(captor.capture());
        return captor.getValue();
    }

    private User createUser() {
        User created = User.builder()
                .email(EMAIL)
                .password("password-hash")
                .firstName("foo")
                .lastName("bar")
                .roles(new HashSet<>())
                .build();
        created.setId(USER_ID);
        return created;
    }

    private Log createLog() {
        return Log.builder()
                .id(LOG_ID)
                .action(ACTION)
                .entity(ENTITY)
                .entityId(ENTITY_ID)
                .actorId(USER_ID)
                .actorEmail(EMAIL)
                .details(DETAILS)
                .severity(LogSeverity.WARN)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void stubAuthenticatedUser() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(EMAIL, null, List.of()));
        SecurityContextHolder.setContext(context);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }
}
