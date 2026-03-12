package bg.uni.sofia.fmi.spring.hirebean.repository;

import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobApplication;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.ApplicationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findAllByCandidateId(Long candidateId);

    List<JobApplication> findAllByJobOfferId(Long jobOfferId);

    boolean existsByCandidateIdAndJobOfferId(Long candidateId, Long jobOfferId);

    List<JobApplication> findAllByJobOfferIdAndStatus(Long jobOfferId, ApplicationStatus status);
}
