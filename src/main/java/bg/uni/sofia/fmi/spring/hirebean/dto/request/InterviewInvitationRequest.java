package bg.uni.sofia.fmi.spring.hirebean.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class InterviewInvitationRequest {

    @NotNull(message = "Interview date and time are required")
    @Future(message = "Interview date and time must be in the future")
    private LocalDateTime interviewAt;

    @Size(max = 2000, message = "Interview message must not exceed 2000 characters")
    private String message;
}
