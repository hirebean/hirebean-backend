package bg.uni.sofia.fmi.spring.hirebean.repository;

import bg.uni.sofia.fmi.spring.hirebean.model.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByRecipientId(Long recipientId);

    List<Notification> findAllByRecipientIdAndIsRead(Long recipientId, boolean isRead);

    boolean countByRecipientIdAndIsRead(Long recipientId, boolean isRead);
}
