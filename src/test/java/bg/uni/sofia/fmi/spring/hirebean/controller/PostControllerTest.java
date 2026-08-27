package bg.uni.sofia.fmi.spring.hirebean.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.PostRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.PostResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.security.JwtAuthenticationFilter;
import bg.uni.sofia.fmi.spring.hirebean.security.OwnershipAuthorizationService;
import bg.uni.sofia.fmi.spring.hirebean.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = PostController.class,
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class PostControllerTest {

    private static final Long POST_ID = 1L;
    private static final Long COMPANY_ID = 2L;
    private static final Long AUTHOR_ID = 3L;
    private static final String TITLE = "foo";
    private static final String CONTENT = "bar";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    @MockBean(name = "ownership")
    private OwnershipAuthorizationService ownership;

    @Test
    @WithAnonymousUser
    void getAllPosts_anonymousUser_returnsPagedPosts() throws Exception {
        when(postService.getAllPosts(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(postResponse())));

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(TITLE))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithAnonymousUser
    void getAllPosts_withCompanyId_passesFilterToService() throws Exception {
        when(postService.getAllPosts(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/posts").param("companyId", String.valueOf(COMPANY_ID)))
                .andExpect(status().isOk());

        verify(postService).getAllPosts(eq(COMPANY_ID), any(Pageable.class));
    }

    @Test
    @WithAnonymousUser
    void getAllPosts_nonNumericCompanyId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/posts").param("companyId", "abc")).andExpect(status().isBadRequest());

        verify(postService, never()).getAllPosts(any(), any());
    }

    @Test
    @WithAnonymousUser
    void getPostById_existingPost_returnsPost() throws Exception {
        when(postService.getPostById(POST_ID)).thenReturn(postResponse());

        mockMvc.perform(get("/api/posts/{id}", POST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(POST_ID))
                .andExpect(jsonPath("$.content").value(CONTENT));
    }

    @Test
    @WithAnonymousUser
    void getPostById_postDoesNotExist_returnsNotFound() throws Exception {
        when(postService.getPostById(POST_ID)).thenThrow(new ResourceNotFoundException("Post not found"));

        mockMvc.perform(get("/api/posts/{id}", POST_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void createPost_ownershipAllows_returnsCreated() throws Exception {
        when(ownership.canCreatePost(any(), any(PostRequest.class))).thenReturn(true);
        when(postService.createPost(any(PostRequest.class))).thenReturn(postResponse());

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(TITLE));
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void createPost_ownershipDenies_returnsForbidden() throws Exception {
        when(ownership.canCreatePost(any(), any(PostRequest.class))).thenReturn(false);

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest())))
                .andExpect(status().isForbidden());

        verify(postService, never()).createPost(any());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void createPost_blankContent_returnsBadRequest() throws Exception {
        when(ownership.canCreatePost(any(), any(PostRequest.class))).thenReturn(true);

        PostRequest request = postRequest();
        request.setContent("  ");

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(postService, never()).createPost(any());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void createPost_missingCompanyId_returnsBadRequest() throws Exception {
        when(ownership.canCreatePost(any(), any(PostRequest.class))).thenReturn(true);

        PostRequest request = postRequest();
        request.setCompanyId(null);

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(postService, never()).createPost(any());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void updatePost_ownershipAllows_returnsUpdatedPost() throws Exception {
        when(ownership.canUpdatePost(any(), eq(POST_ID), any(PostRequest.class)))
                .thenReturn(true);
        when(postService.updatePost(eq(POST_ID), any(PostRequest.class))).thenReturn(postResponse());

        mockMvc.perform(put("/api/posts/{id}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(TITLE));
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void updatePost_ownershipDenies_returnsForbidden() throws Exception {
        when(ownership.canUpdatePost(any(), eq(POST_ID), any(PostRequest.class)))
                .thenReturn(false);

        mockMvc.perform(put("/api/posts/{id}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest())))
                .andExpect(status().isForbidden());

        verify(postService, never()).updatePost(any(), any());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void deletePost_ownershipAllows_returnsNoContent() throws Exception {
        when(ownership.canManagePost(any(), eq(POST_ID))).thenReturn(true);

        mockMvc.perform(delete("/api/posts/{id}", POST_ID)).andExpect(status().isNoContent());

        verify(postService).deletePost(POST_ID);
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void deletePost_ownershipDenies_returnsForbidden() throws Exception {
        when(ownership.canManagePost(any(), eq(POST_ID))).thenReturn(false);

        mockMvc.perform(delete("/api/posts/{id}", POST_ID)).andExpect(status().isForbidden());

        verify(postService, never()).deletePost(any());
    }

    private PostResponse postResponse() {
        return PostResponse.builder()
                .id(POST_ID)
                .title(TITLE)
                .content(CONTENT)
                .companyId(COMPANY_ID)
                .companyName("baz")
                .authorId(AUTHOR_ID)
                .authorEmail("test@test.com")
                .build();
    }

    private PostRequest postRequest() {
        PostRequest request = new PostRequest();
        request.setTitle(TITLE);
        request.setContent(CONTENT);
        request.setCompanyId(COMPANY_ID);
        request.setAuthorId(AUTHOR_ID);
        return request;
    }
}
