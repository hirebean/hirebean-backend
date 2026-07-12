package bg.uni.sofia.fmi.spring.hirebean.service;

import bg.uni.sofia.fmi.spring.hirebean.dto.response.LogResponse;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    void record(String action, String entity, Long entityId, String details, String severity);

    void record(String action, String entity, Long entityId, Long actorId, String details, String severity);

    Page<LogResponse> searchLogs(
            Long actorId,
            String action,
            String entity,
            String severity,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    LogResponse getLogById(Long id);

    void deleteLog(Long id);
}
