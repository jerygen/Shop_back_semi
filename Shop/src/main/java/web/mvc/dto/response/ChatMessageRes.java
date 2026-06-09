package web.mvc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import web.mvc.domain.ChatMessage;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatMessageRes {
    private Long chatMessageNo;
    private Long chatRoomNo;
    private Long senderNo;
    private String senderId;
    private String senderName;
    private String senderRole;
    private String content;
    private LocalDateTime sentAt;

    public ChatMessageRes(ChatMessage chatMessage) {
        this.chatMessageNo = chatMessage.getChatMessageNo();
        this.chatRoomNo = chatMessage.getChatRoom().getChatRoomNo();
        this.senderNo = chatMessage.getSender().getUserNo();
        this.senderId = chatMessage.getSender().getUserId();
        this.senderName = chatMessage.getSender().getUserName();
        this.senderRole = chatMessage.getSender().getRole();
        this.content = chatMessage.getContent();
        this.sentAt = chatMessage.getSentAt();
    }
}
