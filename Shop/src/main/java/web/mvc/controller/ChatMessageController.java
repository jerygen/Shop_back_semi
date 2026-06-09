package web.mvc.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import web.mvc.dto.request.ChatMessageReq;
import web.mvc.dto.response.ChatMessageRes;
import web.mvc.service.ChatService;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/rooms/{chatRoomNo}")
    public void sendMessage(
            Principal principal,
            @DestinationVariable Long chatRoomNo,
            @Payload ChatMessageReq chatMessageReq
    ) {
        ChatMessageRes message = chatService.sendMessage(principal.getName(), chatRoomNo, chatMessageReq);
        messagingTemplate.convertAndSend("/sub/chat/rooms/" + chatRoomNo, message);
    }
}
