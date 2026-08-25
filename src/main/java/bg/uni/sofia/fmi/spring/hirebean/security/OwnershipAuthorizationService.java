package bg.uni.sofia.fmi.spring.hirebean.security;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobOfferRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.request.PostRequest;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobApplication;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobOffer;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Notification;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Post;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.RoleType;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobApplicationRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobOfferRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.NotificationRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.PostRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("ownership")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnershipAuthorizationService {

    private final UserRepository userRepository;
    private final JobOfferRepository jobOfferRepository;
    private final PostRepository postRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final NotificationRepository notificationRepository;

    public boolean isSelfOrAdmin(Authentication authentication, Long userId) {
        if (isAdmin(authentication)) {
            return true;
        }

        return currentUser(authentication)
                .map(user -> user.getId().equals(userId))
                .orElse(false);
    }

    public boolean canCreateCompany(Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }

        return currentUser(authentication)
                .filter(user -> hasRole(user, RoleType.EMPLOYER))
                .map(user -> user.getCompany() == null)
                .orElse(false);
    }

    public boolean canManageCompany(Authentication authentication, Long companyId) {
        if (isAdmin(authentication)) {
            return true;
        }

        return currentEmployerOwnsCompany(authentication, companyId);
    }

    public boolean canCreateJobOffer(Authentication authentication, JobOfferRequest request) {
        if (request == null || request.getCompanyId() == null) {
            return false;
        }

        return isAdmin(authentication) || currentEmployerOwnsCompany(authentication, request.getCompanyId());
    }

    public boolean canUpdateJobOffer(Authentication authentication, Long jobOfferId, JobOfferRequest request) {
        if (isAdmin(authentication)) {
            return true;
        }

        return canManageJobOffer(authentication, jobOfferId) && canCreateJobOffer(authentication, request);
    }

    public boolean canManageJobOffer(Authentication authentication, Long jobOfferId) {
        if (isAdmin(authentication)) {
            return true;
        }

        return jobOfferRepository
                .findById(jobOfferId)
                .map(JobOffer::getCompany)
                .map(company -> currentEmployerOwnsCompany(authentication, company.getId()))
                .orElse(false);
    }

    public boolean canCreatePost(Authentication authentication, PostRequest request) {
        if (request == null || request.getCompanyId() == null) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        return currentUser(authentication)
                .filter(user -> hasRole(user, RoleType.EMPLOYER))
                .filter(user -> user.getCompany() != null)
                .filter(user -> user.getCompany().getId().equals(request.getCompanyId()))
                .map(user -> request.getAuthorId() == null || user.getId().equals(request.getAuthorId()))
                .orElse(false);
    }

    public boolean canUpdatePost(Authentication authentication, Long postId, PostRequest request) {
        if (isAdmin(authentication)) {
            return true;
        }

        return canManagePost(authentication, postId) && canCreatePost(authentication, request);
    }

    public boolean canManagePost(Authentication authentication, Long postId) {
        if (isAdmin(authentication)) {
            return true;
        }

        return postRepository
                .findById(postId)
                .map(Post::getCompany)
                .map(company -> currentEmployerOwnsCompany(authentication, company.getId()))
                .orElse(false);
    }

    public boolean canApplyAsCandidate(Authentication authentication, Long candidateId) {
        if (isAdmin(authentication)) {
            return true;
        }

        return currentUser(authentication)
                .filter(user -> hasRole(user, RoleType.CANDIDATE))
                .map(user -> user.getId().equals(candidateId))
                .orElse(false);
    }

    public boolean canViewCandidateApplications(Authentication authentication, Long candidateId) {
        return isSelfOrAdmin(authentication, candidateId);
    }

    public boolean canViewJobApplications(Authentication authentication, Long jobOfferId) {
        return canManageJobOffer(authentication, jobOfferId);
    }

    public boolean canManageApplication(Authentication authentication, Long applicationId) {
        if (isAdmin(authentication)) {
            return true;
        }

        return jobApplicationRepository
                .findById(applicationId)
                .map(JobApplication::getJobOffer)
                .map(JobOffer::getCompany)
                .map(company -> currentEmployerOwnsCompany(authentication, company.getId()))
                .orElse(false);
    }

    public boolean canManageNotification(Authentication authentication, Long notificationId) {
        if (isAdmin(authentication)) {
            return true;
        }

        return notificationRepository
                .findById(notificationId)
                .map(Notification::getRecipient)
                .map(recipient -> currentUser(authentication)
                        .map(user -> user.getId().equals(recipient.getId()))
                        .orElse(false))
                .orElse(false);
    }

    public JobOfferVisibilityScope getJobOfferVisibilityScope(Authentication authentication) {
        if (isAdmin(authentication)) {
            return JobOfferVisibilityScope.fullVisibility();
        }

        return currentUser(authentication)
                .filter(user -> hasRole(user, RoleType.EMPLOYER))
                .map(User::getCompany)
                .map(company -> JobOfferVisibilityScope.managedCompanyVisibility(company.getId()))
                .orElseGet(JobOfferVisibilityScope::publicVisibility);
    }

    private boolean isAdmin(Authentication authentication) {
        return hasAuthority(authentication, "ROLE_ADMIN");
    }

    private boolean currentEmployerOwnsCompany(Authentication authentication, Long companyId) {
        if (companyId == null) {
            return false;
        }

        return currentUser(authentication)
                .filter(user -> hasRole(user, RoleType.EMPLOYER))
                .filter(user -> user.getCompany() != null)
                .map(user -> user.getCompany().getId().equals(companyId))
                .orElse(false);
    }

    private Optional<User> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        String email = authentication.getName();
        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            return Optional.empty();
        }

        return userRepository.findByEmail(email);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }

    private boolean hasRole(User user, RoleType roleType) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleType);
    }
}
