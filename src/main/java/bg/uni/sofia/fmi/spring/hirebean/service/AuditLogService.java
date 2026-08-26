package bg.uni.sofia.fmi.spring.hirebean.service;

import bg.uni.sofia.fmi.spring.hirebean.dto.response.LogResponse;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.LogSeverity;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    void record(String action, String entity, Long entityId, String details, LogSeverity severity);

    void record(String action, String entity, Long entityId, Long actorId, String details, LogSeverity severity);

    Page<LogResponse> searchLogs(
            Long actorId,
            String action,
            String entity,
            LogSeverity severity,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    LogResponse getLogById(Long id);

    void deleteLog(Long id);
}
