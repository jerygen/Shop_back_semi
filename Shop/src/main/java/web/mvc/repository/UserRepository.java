package web.mvc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import web.mvc.domain.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUserId(String userId);

    Optional<User> findByUserId(String userId);

    @Query("""
            select u, count(o)
            from User u
            left join Orders o on o.user = u
            group by u
            order by u.userNo asc
            """)
    List<Object[]> findAllWithOrderCount();
}
