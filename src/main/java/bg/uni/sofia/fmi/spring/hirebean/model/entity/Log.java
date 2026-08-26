package bg.uni.sofia.fmi.spring.hirebean.model.entity;

import bg.uni.sofia.fmi.spring.hirebean.model.enums.LogSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String entity;

    private Long entityId;

    @Column(name = "user_id")
    private Long actorId;

    @Column(name = "user_email")
    private String actorEmail;

    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogSeverity severity;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;
}
