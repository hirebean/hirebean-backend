package bg.uni.sofia.fmi.spring.hirebean.service;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.PostRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {

    Page<PostResponse> getAllPosts(Long companyId, Pageable pageable);

    PostResponse getPostById(Long id);

    PostResponse createPost(PostRequest request);

    PostResponse updatePost(Long id, PostRequest request);

    void deletePost(Long id);
}
