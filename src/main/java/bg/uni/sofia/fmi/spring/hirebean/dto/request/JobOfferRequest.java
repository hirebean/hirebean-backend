package bg.uni.sofia.fmi.spring.hirebean.dto.request;

import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobStatus;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Set;
import lombok.Data;

@Data
public class JobOfferRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private String location;

    @NotNull
    private JobType jobType;

    private BigDecimal minSalary;

    private BigDecimal maxSalary;

    private JobStatus status;

    @NotNull(message = "Company ID is required")
    private Long companyId;

    private Set<String> tags;

}
