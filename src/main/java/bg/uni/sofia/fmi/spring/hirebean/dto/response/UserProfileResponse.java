package bg.uni.sofia.fmi.spring.hirebean.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;

    // CandidateProfile fields
    private String profilePictureUrl;
    private String bio;
    private String linkedinUrl;
    private String githubUrl;
    private String jobTitle;
    private String resumeUrl; // presigned S3 URL
}
