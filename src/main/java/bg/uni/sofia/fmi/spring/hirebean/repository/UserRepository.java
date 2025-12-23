package bg.uni.sofia.fmi.spring.hirebean.repository;

import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends SoftDeleteRepository<User, Long> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Boolean existsByEmailAndDeletedAtIsNull(String email);
}