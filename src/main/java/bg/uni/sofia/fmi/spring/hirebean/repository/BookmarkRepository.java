package bg.uni.sofia.fmi.spring.hirebean.repository;

import bg.uni.sofia.fmi.spring.hirebean.model.entity.Bookmark;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findAllByUserId(Long userId);

    Optional<Bookmark> findByUserIdAndJobOfferId(Long userId, Long jobOfferId);

    boolean existsByUserIdAndJobOfferId(Long userId, Long jobOfferId);
}
