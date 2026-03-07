package bg.uni.sofia.fmi.spring.hirebean.dto.response;

import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobStatus;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobOfferResponse {

    private Long id;
    private String title;
    private String description;
    private String location;
    private JobType jobType;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private JobStatus status;
    private LocalDateTime createdAt;

    private Long companyId;
    private String companyName;
    private String companyLogoUrl;

    private Set<String> tags;

}
