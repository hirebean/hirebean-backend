package bg.uni.sofia.fmi.spring.hirebean.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private Long recipientId;
    private String message;
    private boolean read;
    private String type;
    private LocalDateTime createdAt;
}
