package web.mvc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import web.mvc.domain.ChatRoom;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatRoomRes {
    private Long chatRoomNo;
    private Long userNo;
    private String userId;
    private String userName;
    private LocalDateTime createdAt;

    public ChatRoomRes(ChatRoom chatRoom) {
        this.chatRoomNo = chatRoom.getChatRoomNo();
        this.userNo = chatRoom.getUser().getUserNo();
        this.userId = chatRoom.getUser().getUserId();
        this.userName = chatRoom.getUser().getUserName();
        this.createdAt = chatRoom.getCreatedAt();
    }
}
