package web.mvc.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import web.mvc.domain.ChatRoom;
import web.mvc.domain.User;
import web.mvc.exception.ChatException;
import web.mvc.exception.ErrorCode;
import web.mvc.jwt.JWTUtil;
import web.mvc.repository.ChatRoomRepository;
import web.mvc.security.CustomUserDetails;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JWTUtil jwtUtil;
    private final ChatRoomRepository chatRoomRepository;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/pub");
        registry.enableSimpleBroker("/sub");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    accessor.setUser(createAuthentication(accessor.getFirstNativeHeader("Authorization")));
                }

                if (StompCommand.SEND.equals(accessor.getCommand())
                        || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    validateChatRoomAccess(accessor);
                }

                return message;
            }
        });
    }

    private Authentication createAuthentication(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing websocket token");
        }

        String token = authorization.substring(7);
        if (jwtUtil.isExpired(token)) {
            throw new AccessDeniedException("Expired websocket token");
        }

        User user = new User();
        user.setUserNo(jwtUtil.getUserNo(token));
        user.setUserId(jwtUtil.getUserId(token));
        user.setUserName(jwtUtil.getUserName(token));
        user.setRole(jwtUtil.getRole(token));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private void validateChatRoomAccess(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication authentication)) {
            throw new AccessDeniedException("Unauthenticated websocket request");
        }

        Long chatRoomNo = extractChatRoomNo(accessor.getDestination());
        if (chatRoomNo == null) {
            return;
        }

        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (admin) {
            return;
        }

        ChatRoom chatRoom = chatRoomRepository.findByIdWithUser(chatRoomNo)
                .orElseThrow(() -> new ChatException(ErrorCode.CHAT_NOT_FOUND));

        if (!chatRoom.getUser().getUserId().equals(authentication.getName())) {
            throw new AccessDeniedException("Not chat room participant");
        }
    }

    private Long extractChatRoomNo(String destination) {
        if (destination == null) {
            return null;
        }

        String pubPrefix = "/pub/chat/rooms/";
        String subPrefix = "/sub/chat/rooms/";
        String value = null;

        if (destination.startsWith(pubPrefix)) {
            value = destination.substring(pubPrefix.length());
        }

        if (destination.startsWith(subPrefix)) {
            value = destination.substring(subPrefix.length());
        }

        if (value == null || value.isBlank()) {
            return null;
        }

        return Long.parseLong(value);
    }
}
