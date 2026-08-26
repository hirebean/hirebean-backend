package bg.uni.sofia.fmi.spring.hirebean.dto.response;

import bg.uni.sofia.fmi.spring.hirebean.model.enums.LogSeverity;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogResponse {

    private Long id;
    private String action;
    private String entity;
    private Long entityId;
    private Long actorId;
    private String actorEmail;
    private String details;
    private LogSeverity severity;
    private LocalDateTime timestamp;
}
