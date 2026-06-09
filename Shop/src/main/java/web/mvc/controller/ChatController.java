package web.mvc.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import web.mvc.dto.response.ApiResponse;
import web.mvc.dto.response.ChatMessageRes;
import web.mvc.dto.response.ChatRoomRes;
import web.mvc.security.CustomUserDetails;
import web.mvc.service.ChatService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/api/chat/rooms")
    public ResponseEntity<ApiResponse<ChatRoomRes>> createRoom(@AuthenticationPrincipal CustomUserDetails userDetails) {
        ChatRoomRes chatRoom = chatService.createRoom(userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(chatRoom));
    }

    @GetMapping("/api/admin/chat/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomRes>>> findAllRooms() {
        List<ChatRoomRes> chatRooms = chatService.findAllRooms();
        return ResponseEntity.ok(ApiResponse.success(chatRooms));
    }

    @GetMapping("/api/chat/rooms/{chatRoomNo}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageRes>>> findMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long chatRoomNo
    ) {
        List<ChatMessageRes> messages = chatService.findMessages(userDetails.getUser().getUserId(), chatRoomNo);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
}
