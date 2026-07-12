package bg.uni.sofia.fmi.spring.hirebean.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostResponse {

    private Long id;
    private String title;
    private String content;
    private String imageUrl;
    private Long companyId;
    private String companyName;
    private Long authorId;
    private String authorEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
