package web.mvc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import web.mvc.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("select u from User u where u.userId=?1")
    User duplicateCheck(Long userId);

    Boolean existsByUserId(String userId);

    User findByUserId(String userId);

}
