package bg.uni.sofia.fmi.spring.hirebean.repository;

import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobOffer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {

    Page<JobOffer> findAllByCompanyId(Long companyId, Pageable pageable);

    Page<JobOffer> findByTitleContainingIgnoreCase(String title, Pageable pageable);

}
