package web.mvc.dto;

import org.junit.jupiter.api.Test;
import web.mvc.domain.ChatMessage;
import web.mvc.domain.ChatRoom;
import web.mvc.domain.User;
import web.mvc.dto.response.ChatMessageRes;
import web.mvc.dto.response.ChatRoomRes;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ChatResponseDtoTest {

    @Test
    void chatRoomResMapsOnlySafeRoomAndUserFields() {
        User user = User.builder()
                .userId("customer01")
                .password("secret")
                .userName("Customer")
                .role("ROLE_USER")
                .build();
        user.setUserNo(1L);

        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 9, 10, 0);
        ChatRoom chatRoom = ChatRoom.builder()
                .user(user)
                .createdAt(createdAt)
                .build();
        chatRoom.setChatRoomNo(10L);

        ChatRoomRes result = new ChatRoomRes(chatRoom);

        assertThat(result.getChatRoomNo()).isEqualTo(10L);
        assertThat(result.getUserNo()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo("customer01");
        assertThat(result.getUserName()).isEqualTo("Customer");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void chatMessageResMapsMessageSenderAndRoomFields() {
        User sender = User.builder()
                .userId("admin01")
                .password("secret")
                .userName("Admin")
                .role("ROLE_ADMIN")
                .build();
        sender.setUserNo(99L);

        ChatRoom chatRoom = ChatRoom.builder()
                .user(sender)
                .createdAt(LocalDateTime.of(2026, 6, 9, 10, 0))
                .build();
        chatRoom.setChatRoomNo(10L);

        LocalDateTime sentAt = LocalDateTime.of(2026, 6, 9, 10, 5);
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content("답변입니다.")
                .sentAt(sentAt)
                .build();
        chatMessage.setChatMessageNo(20L);

        ChatMessageRes result = new ChatMessageRes(chatMessage);

        assertThat(result.getChatMessageNo()).isEqualTo(20L);
        assertThat(result.getChatRoomNo()).isEqualTo(10L);
        assertThat(result.getSenderNo()).isEqualTo(99L);
        assertThat(result.getSenderId()).isEqualTo("admin01");
        assertThat(result.getSenderName()).isEqualTo("Admin");
        assertThat(result.getSenderRole()).isEqualTo("ROLE_ADMIN");
        assertThat(result.getContent()).isEqualTo("답변입니다.");
        assertThat(result.getSentAt()).isEqualTo(sentAt);
    }
}
