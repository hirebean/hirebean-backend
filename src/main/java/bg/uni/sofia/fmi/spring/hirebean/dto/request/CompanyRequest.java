package bg.uni.sofia.fmi.spring.hirebean.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyRequest {

    @NotBlank
    private String name;
    private String description;
    private String websiteUrl;
    private String logoUrl;
    private String location;

}
