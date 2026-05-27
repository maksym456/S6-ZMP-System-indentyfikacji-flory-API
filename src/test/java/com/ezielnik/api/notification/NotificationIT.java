package com.ezielnik.api.notification;

import com.ezielnik.api.IntegrationTestBase;
import com.ezielnik.api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationIT extends IntegrationTestBase {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    private String aliceToken;
    private User alice;

    @BeforeEach
    void setUpAlice() {
        alice = registerAndVerify("alice", "alice@example.com", "Password1!");
        aliceToken = loginAndGetToken("alice", "Password1!");
    }

    private void createNotificationForAlice(String title, String message) {
        notificationService.createNotification(alice, title, message);
    }

    @Test
    void getNotifications_emptyForNewUser() {
        ResponseEntity<List<NotificationResponse>> resp = restTemplate.exchange(
                "/notifications", HttpMethod.GET, withAuth(aliceToken),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEmpty();
    }

    @Test
    void getNotifications_returnsAllNotifications() {
        createNotificationForAlice("Title 1", "Message 1");
        createNotificationForAlice("Title 2", "Message 2");

        ResponseEntity<List<NotificationResponse>> resp = restTemplate.exchange(
                "/notifications", HttpMethod.GET, withAuth(aliceToken),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(2);
    }

    @Test
    void getUnreadNotifications_returnsOnlyUnread() {
        createNotificationForAlice("Unread 1", "msg");
        createNotificationForAlice("Unread 2", "msg");

        // Mark one as read
        List<Notification> all = notificationRepository.findByUserIdOrderByCreatedAtDesc(alice.getId());
        Notification first = all.getFirst();
        first.setRead(true);
        notificationRepository.save(first);

        ResponseEntity<List<NotificationResponse>> resp = restTemplate.exchange(
                "/notifications/unread", HttpMethod.GET, withAuth(aliceToken),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var unreadBody = resp.getBody();
        assertThat(unreadBody).hasSize(1);
        assertThat(unreadBody.getFirst().isRead()).isFalse();
    }

    @Test
    void markAsRead_changesStatus() {
        createNotificationForAlice("Test Notification", "msg");
        UUID notificationId = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(alice.getId())
                .getFirst().getId();

        ResponseEntity<String> resp = restTemplate.exchange(
                "/notifications/" + notificationId + "/read",
                HttpMethod.PATCH, withAuth(aliceToken), String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        Notification notification = notificationRepository.findById(notificationId).orElseThrow();
        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markAsRead_alreadyRead_returnsAlreadyRead() {
        createNotificationForAlice("Already Read", "msg");
        UUID notificationId = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(alice.getId())
                .getFirst().getId();

        restTemplate.exchange(
                "/notifications/" + notificationId + "/read",
                HttpMethod.PATCH, withAuth(aliceToken), String.class
        );

        ResponseEntity<String> secondResp = restTemplate.exchange(
                "/notifications/" + notificationId + "/read",
                HttpMethod.PATCH, withAuth(aliceToken), String.class
        );
        assertThat(secondResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondResp.getBody()).contains("already read");
    }

    @Test
    void markAsRead_otherUsersNotification_returns403() {
        registerAndVerify("bob", "bob@example.com", "Password1!");
        String bobToken = loginAndGetToken("bob", "Password1!");

        createNotificationForAlice("Alice's notification", "msg");
        UUID notificationId = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(alice.getId())
                .getFirst().getId();

        ResponseEntity<String> resp = restTemplate.exchange(
                "/notifications/" + notificationId + "/read",
                HttpMethod.PATCH, withAuth(bobToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void markAllAsRead_marksAll() {
        createNotificationForAlice("N1", "msg");
        createNotificationForAlice("N2", "msg");
        createNotificationForAlice("N3", "msg");

        ResponseEntity<String> resp = restTemplate.exchange(
                "/notifications/read-all", HttpMethod.PATCH, withAuth(aliceToken), String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Notification> all = notificationRepository.findByUserIdOrderByCreatedAtDesc(alice.getId());
        assertThat(all).allMatch(Notification::isRead);
    }

    @Test
    void markAllAsRead_noUnread_returnsNoUnreadMessage() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/notifications/read-all", HttpMethod.PATCH, withAuth(aliceToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("No unread");
    }

    @Test
    void getNotifications_requiresAuthentication() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/notifications", HttpMethod.GET,
                noAuth(), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void notifications_doNotLeakBetweenUsers() {
        registerAndVerify("bob", "bob@example.com", "Password1!");
        String bobToken = loginAndGetToken("bob", "Password1!");

        createNotificationForAlice("Alice's private notification", "msg");

        ResponseEntity<List<NotificationResponse>> bobResp = restTemplate.exchange(
                "/notifications", HttpMethod.GET, withAuth(bobToken),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(bobResp.getBody()).isEmpty();
    }
}
