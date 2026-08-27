package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bg.uni.sofia.fmi.spring.hirebean.dto.response.NotificationResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Notification;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.User;
import bg.uni.sofia.fmi.spring.hirebean.repository.NotificationRepository;
import bg.uni.sofia.fmi.spring.hirebean.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long NOTIFICATION_ID = 2L;
    private static final String EMAIL = "test@test.com";
    private static final String MESSAGE = "foo";
    private static final String TYPE = "TEST_TYPE";

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    private final User user = createUser();
    private final Notification notification = createNotification(false);

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository, userRepository);
    }

    @Test
    void getNotificationsByUser_existingNotifications_returnsMappedList() {
        when(notificationRepository.findAllByRecipientId(USER_ID)).thenReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.getNotificationsByUser(USER_ID);

        assertThat(result).hasSize(1);
        NotificationResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(NOTIFICATION_ID);
        assertThat(response.getRecipientId()).isEqualTo(USER_ID);
        assertThat(response.getMessage()).isEqualTo(MESSAGE);
        assertThat(response.getType()).isEqualTo(TYPE);
        assertThat(response.isRead()).isFalse();
    }

    @Test
    void getNotificationsByUser_noNotifications_returnsEmptyList() {
        when(notificationRepository.findAllByRecipientId(USER_ID)).thenReturn(List.of());

        assertThat(notificationService.getNotificationsByUser(USER_ID)).isEmpty();
    }

    @Test
    void createNotification_existingRecipient_savesUnreadNotification() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(NOTIFICATION_ID);
            return saved;
        });

        NotificationResponse response = notificationService.createNotification(USER_ID, MESSAGE, TYPE);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipient()).isSameAs(user);
        assertThat(captor.getValue().getMessage()).isEqualTo(MESSAGE);
        assertThat(captor.getValue().isRead()).isFalse();
        assertThat(response.getMessage()).isEqualTo(MESSAGE);
        assertThat(response.getType()).isEqualTo(TYPE);
    }

    @Test
    void createNotification_recipientDoesNotExist_throwsResourceNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.createNotification(USER_ID, MESSAGE, TYPE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(USER_ID));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_existingNotification_marksItReadAndSaves() {
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead(NOTIFICATION_ID);

        assertThat(notification.isRead()).isTrue();
        assertThat(response.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_notificationDoesNotExist_throwsResourceNotFound() {
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NOTIFICATION_ID));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllAsRead_unreadNotifications_marksAllOfThemReadAndSavesThem() {
        Notification second = createNotification(false);
        second.setId(3L);
        when(notificationRepository.findAllByRecipientIdAndIsRead(USER_ID, false))
                .thenReturn(List.of(notification, second));

        notificationService.markAllAsRead(USER_ID);

        assertThat(notification.isRead()).isTrue();
        assertThat(second.isRead()).isTrue();
        verify(notificationRepository).saveAll(List.of(notification, second));
    }

    @Test
    void markAllAsRead_noUnreadNotifications_savesEmptyList() {
        when(notificationRepository.findAllByRecipientIdAndIsRead(USER_ID, false))
                .thenReturn(List.of());

        notificationService.markAllAsRead(USER_ID);

        verify(notificationRepository).saveAll(List.of());
    }

    @Test
    void countUnreadNotifications_existingUser_returnsRepositoryCount() {
        when(notificationRepository.countByRecipientIdAndIsRead(USER_ID, false)).thenReturn(5L);

        assertThat(notificationService.countUnreadNotifications(USER_ID)).isEqualTo(5L);
    }

    private User createUser() {
        User created = User.builder()
                .email(EMAIL)
                .password("password-hash")
                .firstName("foo")
                .lastName("bar")
                .roles(new HashSet<>())
                .build();
        created.setId(USER_ID);
        return created;
    }

    private Notification createNotification(boolean read) {
        Notification created = Notification.builder()
                .recipient(user)
                .message(MESSAGE)
                .type(TYPE)
                .isRead(read)
                .build();
        created.setId(NOTIFICATION_ID);
        return created;
    }
}
