package bg.uni.sofia.fmi.spring.hirebean.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiPromptResponse {

    private String prompt;
    private String response;
    private String provider;
    private LocalDateTime createdAt;
}
