package bg.uni.sofia.fmi.spring.hirebean.repository;

import bg.uni.sofia.fmi.spring.hirebean.model.entity.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteAllByUserId(Long userId);
}
