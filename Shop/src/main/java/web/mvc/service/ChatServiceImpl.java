package web.mvc.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import web.mvc.domain.ChatMessage;
import web.mvc.domain.ChatRoom;
import web.mvc.domain.User;
import web.mvc.dto.request.ChatMessageReq;
import web.mvc.dto.response.ChatMessageRes;
import web.mvc.dto.response.ChatRoomRes;
import web.mvc.exception.ChatException;
import web.mvc.exception.ErrorCode;
import web.mvc.exception.UserException;
import web.mvc.repository.ChatMessageRepository;
import web.mvc.repository.ChatRoomRepository;
import web.mvc.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ChatRoomRes createRoom(String userId) {
        User user = getUser(userId);
        ChatRoom chatRoom = ChatRoom.builder()
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        return new ChatRoomRes(chatRoomRepository.save(chatRoom));
    }

    @Override
    @Transactional
    public List<ChatRoomRes> findAllRooms() {
        return chatRoomRepository.findAllWithUser().stream()
                .map(ChatRoomRes::new)
                .toList();
    }

    @Override
    @Transactional
    public List<ChatMessageRes> findMessages(String userId, Long chatRoomNo) {
        ChatRoom chatRoom = getChatRoom(chatRoomNo);
        validateParticipant(userId, chatRoom);

        return chatMessageRepository.findAllByChatRoomNoWithSender(chatRoomNo).stream()
                .map(ChatMessageRes::new)
                .toList();
    }

    @Override
    @Transactional
    public ChatMessageRes sendMessage(String userId, Long chatRoomNo, ChatMessageReq chatMessageReq) {
        if (chatMessageReq.getContent() == null || chatMessageReq.getContent().isBlank()) {
            throw new ChatException(ErrorCode.INVALID_INPUT);
        }

        User sender = getUser(userId);
        ChatRoom chatRoom = getChatRoom(chatRoomNo);
        validateParticipant(userId, chatRoom);

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(chatMessageReq.getContent())
                .sentAt(LocalDateTime.now())
                .build();

        return new ChatMessageRes(chatMessageRepository.save(chatMessage));
    }

    private User getUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
    }

    private ChatRoom getChatRoom(Long chatRoomNo) {
        return chatRoomRepository.findByIdWithUser(chatRoomNo)
                .orElseThrow(() -> new ChatException(ErrorCode.CHAT_NOT_FOUND));
    }

    private void validateParticipant(String userId, ChatRoom chatRoom) {
        User user = getUser(userId);
        if ("ROLE_ADMIN".equals(user.getRole())) {
            return;
        }

        if (!chatRoom.getUser().getUserId().equals(userId)) {
            throw new ChatException(ErrorCode.NOT_CHAT_ROOM_PARTICIPANT);
        }
    }
}
