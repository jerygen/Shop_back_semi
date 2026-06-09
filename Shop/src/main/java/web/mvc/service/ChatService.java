package web.mvc.service;

import web.mvc.dto.request.ChatMessageReq;
import web.mvc.dto.response.ChatMessageRes;
import web.mvc.dto.response.ChatRoomRes;

import java.util.List;

public interface ChatService {
    ChatRoomRes createRoom(String userId);

    List<ChatRoomRes> findAllRooms();

    List<ChatMessageRes> findMessages(String userId, Long chatRoomNo);

    ChatMessageRes sendMessage(String userId, Long chatRoomNo, ChatMessageReq chatMessageReq);
}
