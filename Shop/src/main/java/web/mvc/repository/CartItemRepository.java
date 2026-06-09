package web.mvc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import web.mvc.domain.Cart;
import web.mvc.domain.CartItem;
import web.mvc.domain.Product;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem,Long> {
    @Query("""
            select ci
            from CartItem ci
            where ci.cart = :cart
            and ci.product =:product
            """)
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
}
