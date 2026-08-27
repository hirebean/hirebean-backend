package bg.uni.sofia.fmi.spring.hirebean.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bg.uni.sofia.fmi.spring.hirebean.dto.response.NotificationResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.security.JwtAuthenticationFilter;
import bg.uni.sofia.fmi.spring.hirebean.security.OwnershipAuthorizationService;
import bg.uni.sofia.fmi.spring.hirebean.service.NotificationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = NotificationController.class,
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class NotificationControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long NOTIFICATION_ID = 2L;
    private static final String MESSAGE = "foo";
    private static final String TYPE = "TEST_TYPE";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean(name = "ownership")
    private OwnershipAuthorizationService ownership;

    @Test
    @WithMockUser
    void getNotificationsForUser_ownershipAllows_returnsNotifications() throws Exception {
        when(ownership.isSelfOrAdmin(any(), eq(USER_ID))).thenReturn(true);
        when(notificationService.getNotificationsByUser(USER_ID)).thenReturn(List.of(notificationResponse(false)));

        mockMvc.perform(get("/api/notifications/user/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(NOTIFICATION_ID))
                .andExpect(jsonPath("$[0].message").value(MESSAGE))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    @WithMockUser
    void getNotificationsForUser_ownershipDenies_returnsForbidden() throws Exception {
        when(ownership.isSelfOrAdmin(any(), eq(USER_ID))).thenReturn(false);

        mockMvc.perform(get("/api/notifications/user/{userId}", USER_ID)).andExpect(status().isForbidden());

        verify(notificationService, never()).getNotificationsByUser(any());
    }

    @Test
    @WithMockUser
    void getNotificationsForUser_nonNumericUserId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/notifications/user/{userId}", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void countUnread_ownershipAllows_returnsCount() throws Exception {
        when(ownership.isSelfOrAdmin(any(), eq(USER_ID))).thenReturn(true);
        when(notificationService.countUnreadNotifications(USER_ID)).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/user/{userId}/unread-count", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @WithMockUser
    void countUnread_ownershipDenies_returnsForbidden() throws Exception {
        when(ownership.isSelfOrAdmin(any(), eq(USER_ID))).thenReturn(false);

        mockMvc.perform(get("/api/notifications/user/{userId}/unread-count", USER_ID))
                .andExpect(status().isForbidden());

        verify(notificationService, never()).countUnreadNotifications(any());
    }

    @Test
    @WithMockUser
    void markAllAsReadWithPatch_ownershipAllows_returnsNoContent() throws Exception {
        when(ownership.isSelfOrAdmin(any(), eq(USER_ID))).thenReturn(true);

        mockMvc.perform(patch("/api/notifications/user/{userId}/mark-all-read", USER_ID))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(USER_ID);
    }

    @Test
    @WithMockUser
    void markAllAsReadWithPatch_ownershipDenies_returnsForbidden() throws Exception {
        when(ownership.isSelfOrAdmin(any(), eq(USER_ID))).thenReturn(false);

        mockMvc.perform(patch("/api/notifications/user/{userId}/mark-all-read", USER_ID))
                .andExpect(status().isForbidden());

        verify(notificationService, never()).markAllAsRead(any());
    }

    @Test
    @WithMockUser
    void markAsRead_ownershipAllows_returnsUpdatedNotification() throws Exception {
        when(ownership.canManageNotification(any(), eq(NOTIFICATION_ID))).thenReturn(true);
        when(notificationService.markAsRead(NOTIFICATION_ID)).thenReturn(notificationResponse(true));

        mockMvc.perform(patch("/api/notifications/{notificationId}/mark-read", NOTIFICATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    @WithMockUser
    void markAsRead_ownershipDenies_returnsForbidden() throws Exception {
        when(ownership.canManageNotification(any(), eq(NOTIFICATION_ID))).thenReturn(false);

        mockMvc.perform(patch("/api/notifications/{notificationId}/mark-read", NOTIFICATION_ID))
                .andExpect(status().isForbidden());

        verify(notificationService, never()).markAsRead(any());
    }

    @Test
    @WithMockUser
    void markAsRead_notificationDoesNotExist_returnsNotFound() throws Exception {
        when(ownership.canManageNotification(any(), eq(NOTIFICATION_ID))).thenReturn(true);
        when(notificationService.markAsRead(NOTIFICATION_ID))
                .thenThrow(new ResourceNotFoundException("Notification not found"));

        mockMvc.perform(patch("/api/notifications/{notificationId}/mark-read", NOTIFICATION_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private NotificationResponse notificationResponse(boolean read) {
        return NotificationResponse.builder()
                .id(NOTIFICATION_ID)
                .recipientId(USER_ID)
                .message(MESSAGE)
                .type(TYPE)
                .read(read)
                .build();
    }
}
