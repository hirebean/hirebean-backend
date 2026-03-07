package bg.uni.sofia.fmi.spring.hirebean.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyResponse {

    private Long id;
    private String name;
    private String description;
    private String websiteUrl;
    private String logoUrl;
    private String location;
    private LocalDateTime createdAt;

}
