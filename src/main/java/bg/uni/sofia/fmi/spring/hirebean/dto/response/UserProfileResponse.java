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
    private String bio;
    private String linkedinUrl;
    private String githubUrl;
    private String jobTitle;
    // CV - private, presigned url (valid for 10 min example)
    private String resumeUrl; // time-limited Supabase Storage URL

    // Profile picture - pulic, served via CDN
    private String profilePictureUrl;
}
