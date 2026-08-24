package bg.uni.sofia.fmi.spring.hirebean.dto.request;

import bg.uni.sofia.fmi.spring.hirebean.model.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewApplicationRequest {

    @NotNull(message = "Application status is required")
    private ApplicationStatus status;

    @Size(max = 4000, message = "Feedback must not exceed 4000 characters")
    private String feedbackMessage;
}
