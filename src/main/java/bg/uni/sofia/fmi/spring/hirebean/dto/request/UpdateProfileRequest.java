package bg.uni.sofia.fmi.spring.hirebean.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;

    // CandidateProfile fields
    private String bio;
    private String linkedInUrl;
    private String githubUrl;
    private String jobTitle;
    private String profilePicture;
}
