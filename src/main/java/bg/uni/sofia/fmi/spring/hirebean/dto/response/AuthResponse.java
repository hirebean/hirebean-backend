package bg.uni.sofia.fmi.spring.hirebean.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private Long userId;
    private String token;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Long companyId;
}
