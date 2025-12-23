package bg.uni.sofia.fmi.spring.hirebean.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SoftDeleteRepository<T, ID> extends JpaRepository<T, ID> {

    @Override
    @Query("SELECT e FROM #{#entityName} e WHERE e.deletedAt IS NULL")
    List<T> findAll();

    @Override
    @Query("SELECT e FROM #{#entityName} e WHERE e.id = ?1 AND e.deletedAt IS NULL")
    Optional<T> findById(ID id);

    @Query("update #{#entityName} e set e.deletedAt = CURRENT_TIMESTAMP where e.id = ?1")
    @Transactional
    @Modifying
    void softDelete(ID id);

    @Override
    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.deletedAt IS NULL")
    long count();

    //hard delete if needed
    void deleteById(ID id);
}
