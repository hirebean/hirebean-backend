package bg.uni.sofia.fmi.spring.hirebean.repository;

import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    List<User> findAllByCompanyId(Long companyId);
}
