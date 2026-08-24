package bg.uni.sofia.fmi.spring.hirebean.dto.response;

import bg.uni.sofia.fmi.spring.hirebean.model.enums.ApplicationStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobApplicationResponse {

    private Long id;
    private Long candidateId;
    private String candidateEmail;
    private Long jobOfferId;
    private String jobTitle;
    private String coverLetter;
    private String cvUrl; // time-limited Supabase Storage URL
    private ApplicationStatus status;

    private LocalDateTime createdAt;
}
