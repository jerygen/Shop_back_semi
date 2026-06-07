package web.mvc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import web.mvc.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("select m from User m where m.userId=?1")
    User dupilicateCheck(String id);

    Boolean existsById(String id);

    User findById(String id);
}
