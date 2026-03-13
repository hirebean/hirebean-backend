package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobOfferResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Bookmark;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.JobOffer;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.repository.BookmarkRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.JobOfferRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import bg.uni.sofia.fmi.spring.hirebean.service.BookmarkService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;

    private JobOfferResponse mapJobOfferToResponse(JobOffer jobOffer) {
        return JobOfferResponse.builder()
                .id(jobOffer.getId())
                .title(jobOffer.getTitle())
                .description(jobOffer.getDescription())
                .companyId(jobOffer.getCompany().getId())
                .companyName(jobOffer.getCompany().getName())
                .companyLogoUrl(jobOffer.getCompany().getLogoUrl())
                .location(jobOffer.getLocation())
                .jobType(jobOffer.getJobType())
                .minSalary(jobOffer.getMinSalary())
                .maxSalary(jobOffer.getMaxSalary())
                .status(jobOffer.getStatus())
                .createdAt(jobOffer.getCreatedAt())
                .tags(jobOffer.getTags())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobOfferResponse> getBookmarsByUserId(Long userId) {
        return bookmarkRepository.findAllByUserId(userId).stream()
                .map(bookmark -> mapJobOfferToResponse(bookmark.getJobOffer()))
                .toList();
    }

    @Override
    @Transactional
    public void addBookmark(Long userId, Long jobOfferId) {
        if (bookmarkRepository.existsByUserIdAndJobOfferId(userId, jobOfferId)) {
            return;
        }
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        JobOffer jobOffer = jobOfferRepository
                .findById(jobOfferId)
                .orElseThrow(() -> new ResourceNotFoundException("Job offer not found with id: " + jobOfferId));

        Bookmark bookmark = Bookmark.builder().user(user).jobOffer(jobOffer).build();

        bookmarkRepository.save(bookmark);
    }

    @Override
    @Transactional
    public void removeBookmark(Long userId, Long jobOfferId) {

        Bookmark bookmark = bookmarkRepository
                .findByUserIdAndJobOfferId(userId, jobOfferId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bookmark not found for user id: " + userId + " and job offer id: " + jobOfferId));

        bookmarkRepository.delete(bookmark);
    }
}
