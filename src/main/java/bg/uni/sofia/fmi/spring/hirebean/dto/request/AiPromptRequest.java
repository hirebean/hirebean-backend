package bg.uni.sofia.fmi.spring.hirebean.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiPromptRequest {

    @NotBlank(message = "Prompt is required")
    @Size(max = 4000, message = "Prompt must be at most 4000 characters")
    private String prompt;

    private String purpose;
}
