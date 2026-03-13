package bg.uni.sofia.fmi.spring.hirebean.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobApplicationRequest {

    @NotNull(message = "Job offer ID is required")
    private Long jobOfferId;

    private String coverLetter;
    private String cvUrl;
}
