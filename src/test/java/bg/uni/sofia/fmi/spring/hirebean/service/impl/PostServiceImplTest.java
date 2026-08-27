package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import bg.uni.sofia.fmi.spring.hirebean.service.StorageService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    private static final Long POST_ID = 1L;
    private static final Long COMPANY_ID = 2L;
    private static final Long AUTHOR_ID = 3L;
    private static final String TITLE = "foo";
    private static final String CONTENT = "bar";
    private static final String COMPANY_NAME = "baz";
    private static final String AUTHOR_EMAIL = "test@test.com";
    private static final String IMAGE_KEY = "post-images/abc.png";
    private static final String IMAGE_URL = "https://storage.test/public/abc.png";

    @Mock
    private PostRepository postRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private AuditLogService auditLogService;

    private final Company company = createCompany();
    private final User author = createAuthor();
    private final Post post = createPost(author);

    private PostServiceImpl postService;

    @BeforeEach
    void setUp() {
        postService =
                new PostServiceImpl(postRepository, companyRepository, userRepository, storageService, auditLogService);
    }

    @Test
    void getAllPosts_noCompanyFilter_returnsAllPosts() {
        when(postRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(post)));
        when(storageService.getPublicUrl(IMAGE_KEY)).thenReturn(IMAGE_URL);

        Page<PostResponse> result = postService.getAllPosts(null, Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
        PostResponse response = result.getContent().get(0);
        assertThat(response.getId()).isEqualTo(POST_ID);
        assertThat(response.getTitle()).isEqualTo(TITLE);
        assertThat(response.getCompanyName()).isEqualTo(COMPANY_NAME);
        assertThat(response.getAuthorEmail()).isEqualTo(AUTHOR_EMAIL);
        assertThat(response.getImageUrl()).isEqualTo(IMAGE_URL);
        verify(postRepository, never()).findAllByCompanyId(any(), any());
    }

    @Test
    void getAllPosts_withCompanyFilter_delegatesToCompanyQuery() {
        when(postRepository.findAllByCompanyId(COMPANY_ID, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(post)));

        Page<PostResponse> result = postService.getAllPosts(COMPANY_ID, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(postRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getPostById_existingPost_returnsMappedPost() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        PostResponse response = postService.getPostById(POST_ID);

        assertThat(response.getId()).isEqualTo(POST_ID);
        assertThat(response.getContent()).isEqualTo(CONTENT);
        assertThat(response.getAuthorId()).isEqualTo(AUTHOR_ID);
    }

    @Test
    void getPostById_postWithoutAuthor_returnsNullAuthorFields() {
        Post authorless = createPost(null);
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(authorless));

        PostResponse response = postService.getPostById(POST_ID);

        assertThat(response.getAuthorId()).isNull();
        assertThat(response.getAuthorEmail()).isNull();
    }

    @Test
    void getPostById_postDoesNotExist_throwsResourceNotFound() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(POST_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(POST_ID));
    }

    @Test
    void createPost_validRequest_savesPostAndAudits() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        PostResponse response = postService.createPost(createRequest(AUTHOR_ID));

        assertThat(response.getTitle()).isEqualTo(TITLE);
        verify(auditLogService).record("CREATE", "Post", POST_ID, AUTHOR_ID, "Created company post", LogSeverity.INFO);
    }

    @Test
    void createPost_noAuthorId_savesPostWithoutAuthor() {
        Post authorless = createPost(null);
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(postRepository.save(any(Post.class))).thenReturn(authorless);

        PostResponse response = postService.createPost(createRequest(null));

        assertThat(response.getAuthorId()).isNull();
        verifyNoInteractions(userRepository);
        verify(auditLogService).record("CREATE", "Post", POST_ID, null, "Created company post", LogSeverity.INFO);
    }

    @Test
    void createPost_companyDoesNotExist_throwsResourceNotFound() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(createRequest(AUTHOR_ID)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(COMPANY_ID));

        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_authorDoesNotExist_throwsResourceNotFound() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(createRequest(AUTHOR_ID)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(AUTHOR_ID));

        verify(postRepository, never()).save(any());
    }

    @Test
    void updatePost_validRequest_updatesFieldsAndAudits() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));
        when(postRepository.save(post)).thenReturn(post);

        PostRequest request = createRequest(AUTHOR_ID);
        request.setTitle("updated-title");
        request.setContent("updated-content");

        postService.updatePost(POST_ID, request);

        assertThat(post.getTitle()).isEqualTo("updated-title");
        assertThat(post.getContent()).isEqualTo("updated-content");
        assertThat(post.getCompany()).isSameAs(company);
        verify(auditLogService).record("UPDATE", "Post", POST_ID, AUTHOR_ID, "Updated company post", LogSeverity.INFO);
    }

    @Test
    void updatePost_noAuthorId_clearsExistingAuthor() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(postRepository.save(post)).thenReturn(post);

        postService.updatePost(POST_ID, createRequest(null));

        assertThat(post.getAuthor()).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void updatePost_postDoesNotExist_throwsResourceNotFound() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePost(POST_ID, createRequest(AUTHOR_ID)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(postRepository, never()).save(any());
    }

    @Test
    void updatePost_companyDoesNotExist_throwsResourceNotFound() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePost(POST_ID, createRequest(AUTHOR_ID)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(postRepository, never()).save(any());
    }

    @Test
    void deletePost_existingPost_deletesAndAuditsAtWarnLevel() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        postService.deletePost(POST_ID);

        verify(postRepository).delete(post);
        verify(auditLogService).record("DELETE", "Post", POST_ID, "Deleted company post", LogSeverity.WARN);
    }

    @Test
    void deletePost_postDoesNotExist_throwsResourceNotFound() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(POST_ID)).isInstanceOf(ResourceNotFoundException.class);

        verify(postRepository, never()).delete(any());
    }

    private Company createCompany() {
        Company created = Company.builder()
                .name(COMPANY_NAME)
                .description("test-description")
                .build();
        created.setId(COMPANY_ID);
        return created;
    }

    private User createAuthor() {
        User created = User.builder()
                .email(AUTHOR_EMAIL)
                .password("password-hash")
                .firstName("foo")
                .lastName("bar")
                .roles(new HashSet<>())
                .build();
        created.setId(AUTHOR_ID);
        return created;
    }

    private Post createPost(User postAuthor) {
        Post created = Post.builder()
                .title(TITLE)
                .content(CONTENT)
                .imageUrl(IMAGE_KEY)
                .company(company)
                .author(postAuthor)
                .build();
        created.setId(POST_ID);
        return created;
    }

    private PostRequest createRequest(Long authorId) {
        PostRequest request = new PostRequest();
        request.setTitle(TITLE);
        request.setContent(CONTENT);
        request.setCompanyId(COMPANY_ID);
        request.setAuthorId(authorId);
        request.setImageUrl(IMAGE_KEY);
        return request;
    }
}
