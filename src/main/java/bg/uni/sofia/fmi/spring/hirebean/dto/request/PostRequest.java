package bg.uni.sofia.fmi.spring.hirebean.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Company ID is required")
    private Long companyId;

    private Long authorId;
    private String imageUrl;
}
