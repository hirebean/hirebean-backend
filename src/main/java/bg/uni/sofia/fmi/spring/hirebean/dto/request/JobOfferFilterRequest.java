package bg.uni.sofia.fmi.spring.hirebean.dto.request;

import bg.uni.sofia.fmi.spring.hirebean.model.enums.JobStatus;
import java.math.BigDecimal;
import java.util.Set;
import lombok.Data;

@Data
public class JobOfferFilterRequest {

    private String search;
    private String location;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private Long companyId;
    private Set<String> tags;
    private JobStatus jobStatus;
}
