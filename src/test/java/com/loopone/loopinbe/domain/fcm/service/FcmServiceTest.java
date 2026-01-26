package com.loopone.loopinbe.domain.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.loopone.loopinbe.domain.fcm.dto.res.FcmMessageResponse;
import com.loopone.loopinbe.domain.fcm.serviceImpl.FcmServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @Test
    @DisplayName("성공: FirebaseMessaging에 올바른 Message가 전달된다")
    void sendMessage_success() throws Exception {
        // given
        FcmServiceImpl fcmService = new FcmServiceImpl();
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        when(firebaseMessaging.send(any(Message.class))).thenReturn("ok");

        FcmMessageResponse req = FcmMessageResponse.builder()
                .eventId("evt-1")
                .targetToken("token-123")
                .title("title-1")
                .body("body-1")
                .build();

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            mocked.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);

            // when
            fcmService.sendMessageTo(req);

            // then
            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(firebaseMessaging).send(captor.capture());
            Message msg = captor.getValue();
            assertThat(readToken(msg)).isEqualTo("token-123");
            assertThat(readData(msg)).isEqualTo(Map.of("title", "title-1", "body", "body-1"));
        }
    }

    @Test
    @DisplayName("예외: FirebaseMessagingException 발생 시 전파하지 않는다")
    void sendMessage_firebaseExceptionIsSwallowed() throws Exception {
        // given
        FcmServiceImpl fcmService = new FcmServiceImpl();
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(mock(FirebaseMessagingException.class));

        FcmMessageResponse req = FcmMessageResponse.builder()
                .eventId("evt-2")
                .targetToken("token-err")
                .title("title-err")
                .body("body-err")
                .build();

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            mocked.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);

            // when / then
            assertThatCode(() -> fcmService.sendMessageTo(req)).doesNotThrowAnyException();
            verify(firebaseMessaging).send(any(Message.class));
        }
    }

    private String readToken(Message message) {
        try {
            var field = Message.class.getDeclaredField("token");
            field.setAccessible(true);
            Object value = field.get(message);
            return value != null ? value.toString() : null;
        } catch (ReflectiveOperationException e) {
            fail("Failed to access token field via reflection", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> readData(Message message) {
        try {
            var field = Message.class.getDeclaredField("data");
            field.setAccessible(true);
            Object value = field.get(message);
            return (Map<String, String>) value;
        } catch (ReflectiveOperationException e) {
            fail("Failed to access data field via reflection", e);
            return Map.of();
        }
    }
}
