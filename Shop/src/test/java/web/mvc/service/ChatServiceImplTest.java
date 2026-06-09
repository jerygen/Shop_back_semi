package web.mvc.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import web.mvc.domain.ChatMessage;
import web.mvc.domain.ChatRoom;
import web.mvc.domain.User;
import web.mvc.dto.request.ChatMessageReq;
import web.mvc.dto.response.ChatMessageRes;
import web.mvc.dto.response.ChatRoomRes;
import web.mvc.exception.ChatException;
import web.mvc.exception.ErrorCode;
import web.mvc.repository.ChatMessageRepository;
import web.mvc.repository.ChatRoomRepository;
import web.mvc.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    void createRoomCreatesChatRoomForLoggedInUser() {
        User user = user("customer01", "Customer", "ROLE_USER");
        when(userRepository.findByUserId("customer01")).thenReturn(Optional.of(user));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom chatRoom = invocation.getArgument(0);
            chatRoom.setChatRoomNo(1L);
            return chatRoom;
        });

        ChatRoomRes result = chatService.createRoom("customer01");

        assertThat(result.getChatRoomNo()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo("customer01");
        assertThat(result.getUserName()).isEqualTo("Customer");
        assertThat(result.getCreatedAt()).isNotNull();
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    void findAllRoomsReturnsRoomsWithUserInfo() {
        User customer = user("customer01", "Customer", "ROLE_USER");
        ChatRoom chatRoom = chatRoom(1L, customer);
        when(chatRoomRepository.findAllWithUser()).thenReturn(List.of(chatRoom));

        List<ChatRoomRes> result = chatService.findAllRooms();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChatRoomNo()).isEqualTo(1L);
        assertThat(result.get(0).getUserId()).isEqualTo("customer01");
    }

    @Test
    void findMessagesAllowsRoomOwner() {
        User customer = user("customer01", "Customer", "ROLE_USER");
        ChatRoom chatRoom = chatRoom(1L, customer);
        ChatMessage message = chatMessage(10L, chatRoom, customer, "hello");

        when(chatRoomRepository.findByIdWithUser(1L)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findByUserId("customer01")).thenReturn(Optional.of(customer));
        when(chatMessageRepository.findAllByChatRoomNoWithSender(1L)).thenReturn(List.of(message));

        List<ChatMessageRes> result = chatService.findMessages("customer01", 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("hello");
        assertThat(result.get(0).getSenderId()).isEqualTo("customer01");
    }

    @Test
    void findMessagesAllowsAdminForAnyRoom() {
        User owner = user("customer01", "Customer", "ROLE_USER");
        User admin = user("admin01", "Admin", "ROLE_ADMIN");
        ChatRoom chatRoom = chatRoom(1L, owner);
        ChatMessage message = chatMessage(10L, chatRoom, owner, "question");

        when(chatRoomRepository.findByIdWithUser(1L)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findByUserId("admin01")).thenReturn(Optional.of(admin));
        when(chatMessageRepository.findAllByChatRoomNoWithSender(1L)).thenReturn(List.of(message));

        List<ChatMessageRes> result = chatService.findMessages("admin01", 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("question");
    }

    @Test
    void findMessagesRejectsNonParticipantUser() {
        User owner = user("customer01", "Customer", "ROLE_USER");
        User other = user("other01", "Other", "ROLE_USER");
        ChatRoom chatRoom = chatRoom(1L, owner);

        when(chatRoomRepository.findByIdWithUser(1L)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findByUserId("other01")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> chatService.findMessages("other01", 1L))
                .isInstanceOf(ChatException.class)
                .satisfies(exception ->
                        assertThat(((ChatException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.NOT_CHAT_ROOM_PARTICIPANT));
    }

    @Test
    void sendMessageSavesMessageAndReturnsResponse() {
        User customer = user("customer01", "Customer", "ROLE_USER");
        ChatRoom chatRoom = chatRoom(1L, customer);
        ChatMessageReq request = ChatMessageReq.builder()
                .content("배송 문의입니다.")
                .build();

        when(userRepository.findByUserId("customer01")).thenReturn(Optional.of(customer));
        when(chatRoomRepository.findByIdWithUser(1L)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage chatMessage = invocation.getArgument(0);
            chatMessage.setChatMessageNo(100L);
            return chatMessage;
        });

        ChatMessageRes result = chatService.sendMessage("customer01", 1L, request);

        assertThat(result.getChatMessageNo()).isEqualTo(100L);
        assertThat(result.getChatRoomNo()).isEqualTo(1L);
        assertThat(result.getSenderId()).isEqualTo("customer01");
        assertThat(result.getContent()).isEqualTo("배송 문의입니다.");
        assertThat(result.getSentAt()).isNotNull();
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    void sendMessageRejectsBlankContent() {
        ChatMessageReq request = ChatMessageReq.builder()
                .content("   ")
                .build();

        assertThatThrownBy(() -> chatService.sendMessage("customer01", 1L, request))
                .isInstanceOf(ChatException.class)
                .satisfies(exception ->
                        assertThat(((ChatException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void sendMessageRejectsMissingChatRoom() {
        User customer = user("customer01", "Customer", "ROLE_USER");
        ChatMessageReq request = ChatMessageReq.builder()
                .content("hello")
                .build();

        when(userRepository.findByUserId("customer01")).thenReturn(Optional.of(customer));
        when(chatRoomRepository.findByIdWithUser(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessage("customer01", 99L, request))
                .isInstanceOf(ChatException.class)
                .satisfies(exception ->
                        assertThat(((ChatException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.CHAT_NOT_FOUND));
    }

    private User user(String userId, String userName, String role) {
        User user = User.builder()
                .userId(userId)
                .userName(userName)
                .role(role)
                .build();
        user.setUserNo("ROLE_ADMIN".equals(role) ? 99L : 1L);
        return user;
    }

    private ChatRoom chatRoom(Long chatRoomNo, User user) {
        ChatRoom chatRoom = ChatRoom.builder()
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
        chatRoom.setChatRoomNo(chatRoomNo);
        return chatRoom;
    }

    private ChatMessage chatMessage(Long chatMessageNo, ChatRoom chatRoom, User sender, String content) {
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(content)
                .sentAt(LocalDateTime.now())
                .build();
        chatMessage.setChatMessageNo(chatMessageNo);
        return chatMessage;
    }
}
