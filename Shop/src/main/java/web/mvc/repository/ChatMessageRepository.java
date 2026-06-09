package web.mvc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import web.mvc.domain.ChatMessage;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("""
            select cm
            from ChatMessage cm
            join fetch cm.sender
            where cm.chatRoom.chatRoomNo = :chatRoomNo
            order by cm.sentAt asc
            """)
    List<ChatMessage> findAllByChatRoomNoWithSender(Long chatRoomNo);
}
