package web.mvc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import web.mvc.domain.ChatRoom;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("""
            select cr
            from ChatRoom cr
            join fetch cr.user
            order by cr.createdAt desc
            """)
    List<ChatRoom> findAllWithUser();

    @Query("""
            select cr
            from ChatRoom cr
            join fetch cr.user
            where cr.chatRoomNo = :chatRoomNo
            """)
    Optional<ChatRoom> findByIdWithUser(Long chatRoomNo);
}
