package bg.uni.sofia.fmi.spring.hirebean.repository;

import bg.uni.sofia.fmi.spring.hirebean.model.entity.Company;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByName(String name);

}
