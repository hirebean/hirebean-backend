package bg.uni.sofia.fmi.spring.hirebean.service;

import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobOfferResponse;
import java.util.List;

public interface BookmarkService {

    List<JobOfferResponse> getBookmarsByUserId(Long userId);

    void addBookmark(Long userId, Long jobOfferId);

    void removeBookmark(Long userId, Long jobOfferId);
}
