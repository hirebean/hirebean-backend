package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.response.LogResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Log;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.LogSeverity;
import bg.uni.sofia.fmi.spring.hirebean.repository.LogRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import bg.uni.sofia.fmi.spring.hirebean.service.AuditLogService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final LogRepository logRepository;
    private final UserRepository userRepository;

    private LogResponse mapToResponse(Log log) {
        User actor = log.getActor();
        return LogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .entity(log.getEntity())
                .entityId(log.getEntityId())
                .actorId(actor != null ? actor.getId() : null)
                .actorEmail(actor != null ? actor.getEmail() : null)
                .details(log.getDetails())
                .severity(log.getSeverity())
                .timestamp(log.getTimestamp())
                .build();
    }

    private User resolveActor(Long actorId) {
        if (actorId != null) {
            return userRepository.findById(actorId).orElse(null);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();
        if (!StringUtils.hasText(email) || "anonymousUser".equals(email)) {
            return null;
        }

        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entity, Long entityId, String details, LogSeverity severity) {
        record(action, entity, entityId, null, details, severity);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String action, String entity, Long entityId, Long actorId, String details, LogSeverity severity) {
        Log log = Log.builder()
                .action(action)
                .entity(entity)
                .entityId(entityId)
                .actor(resolveActor(actorId))
                .details(details)
                .severity(severity != null ? severity : LogSeverity.INFO)
                .build();
        logRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LogResponse> searchLogs(
            Long actorId,
            String action,
            String entity,
            LogSeverity severity,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {

        Specification<Log> specification = (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();

            if (actorId != null) {
                predicate = criteriaBuilder.and(
                        predicate, criteriaBuilder.equal(root.get("actor").get("id"), actorId));
            }
            if (StringUtils.hasText(action)) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("action")), "%" + action.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(entity)) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("entity")), "%" + entity.toLowerCase() + "%"));
            }
            if (severity != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("severity"), severity));
            }
            if (from != null) {
                predicate = criteriaBuilder.and(
                        predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), from));
            }
            if (to != null) {
                predicate =
                        criteriaBuilder.and(predicate, criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), to));
            }

            return predicate;
        };

        return logRepository.findAll(specification, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public LogResponse getLogById(Long id) {
        Log log = logRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Log not found with id: " + id));
        return mapToResponse(log);
    }

    @Override
    @Transactional
    public void deleteLog(Long id) {
        Log log = logRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Log not found with id: " + id));
        logRepository.delete(log);
    }
}
