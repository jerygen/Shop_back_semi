package web.mvc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import web.mvc.domain.Cart;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart,Long> {
    @Query("""
                select distinct c 
                from Cart c
                left join fetch c.cartItems ci
                left join fetch ci.product
                where c.user.userId = :userId
            """)
    Optional<Cart> findCartsByUserId(String userId);


}
