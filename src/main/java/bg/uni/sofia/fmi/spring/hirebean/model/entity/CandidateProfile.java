package bg.uni.sofia.fmi.spring.hirebean.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "candidate_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE candidate_profiles SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class CandidateProfile extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // S3 key - accessed key via presigned URL (private URL with expiration)
    private String resumeUrl; // link to file

    // S3 key - accessed via CDN ( public URL)
    private String profilePictureUrl; // link to file

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String linkedinUrl;
    private String githubUrl;
    private String jobTitle;
}
