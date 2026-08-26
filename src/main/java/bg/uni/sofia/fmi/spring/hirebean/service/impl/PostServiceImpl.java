package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.PostRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.PostResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Company;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Post;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.LogSeverity;
import bg.uni.sofia.fmi.spring.hirebean.repository.CompanyRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.PostRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import bg.uni.sofia.fmi.spring.hirebean.service.AuditLogService;
import bg.uni.sofia.fmi.spring.hirebean.service.PostService;
import bg.uni.sofia.fmi.spring.hirebean.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final AuditLogService auditLogService;

    private PostResponse mapToResponse(Post post) {
        User author = post.getAuthor();
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .imageUrl(storageService.getPublicUrl(post.getImageUrl()))
                .companyId(post.getCompany().getId())
                .companyName(post.getCompany().getName())
                .authorId(author != null ? author.getId() : null)
                .authorEmail(author != null ? author.getEmail() : null)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private Company getCompany(Long companyId) {
        return companyRepository
                .findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + companyId));
    }

    private User getAuthor(Long authorId) {
        if (authorId == null) {
            return null;
        }

        return userRepository
                .findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + authorId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPosts(Long companyId, Pageable pageable) {
        Page<Post> posts = companyId == null
                ? postRepository.findAll(pageable)
                : postRepository.findAllByCompanyId(companyId, pageable);
        return posts.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        Post post = postRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        return mapToResponse(post);
    }

    @Override
    @Transactional
    public PostResponse createPost(PostRequest request) {
        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .company(getCompany(request.getCompanyId()))
                .author(getAuthor(request.getAuthorId()))
                .build();

        Post saved = postRepository.save(post);
        auditLogService.record(
                "CREATE", "Post", saved.getId(), request.getAuthorId(), "Created company post", LogSeverity.INFO);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public PostResponse updatePost(Long id, PostRequest request) {
        Post post = postRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setImageUrl(request.getImageUrl());
        post.setCompany(getCompany(request.getCompanyId()));
        post.setAuthor(getAuthor(request.getAuthorId()));

        Post saved = postRepository.save(post);
        auditLogService.record(
                "UPDATE", "Post", saved.getId(), request.getAuthorId(), "Updated company post", LogSeverity.INFO);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        Post post = postRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        postRepository.delete(post);
        auditLogService.record("DELETE", "Post", id, "Deleted company post", LogSeverity.WARN);
    }
}
