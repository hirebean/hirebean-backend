package bg.uni.sofia.fmi.spring.hirebean.controller;

import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobOfferResponse;
import bg.uni.sofia.fmi.spring.hirebean.service.BookmarkService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping("/user/{userId}")
    @PreAuthorize("@ownership.isSelfOrAdmin(authentication, #userId)")
    public ResponseEntity<List<JobOfferResponse>> getBookmarks(@PathVariable Long userId) {
        return ResponseEntity.ok(bookmarkService.getBookmarsByUserId(userId));
    }

    @PostMapping("/user/{userId}/job/{jobOfferId}")
    @PreAuthorize("@ownership.isSelfOrAdmin(authentication, #userId)")
    public ResponseEntity<Void> addBookmark(@PathVariable Long userId, @PathVariable Long jobOfferId) {
        bookmarkService.addBookmark(userId, jobOfferId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/user/{userId}/job/{jobOfferId}")
    @PreAuthorize("@ownership.isSelfOrAdmin(authentication, #userId)")
    public ResponseEntity<Void> removeBookmark(@PathVariable Long userId, @PathVariable Long jobOfferId) {
        bookmarkService.removeBookmark(userId, jobOfferId);
        return ResponseEntity.noContent().build();
    }
}
