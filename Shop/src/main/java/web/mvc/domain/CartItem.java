package web.mvc.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartItemNo;

    @ManyToOne
    @JoinColumn(name="cart_no")
    private Cart cart;

    @ManyToOne
    @JoinColumn(name="product_no")
    private Product product;

    private int quantity;
}
