package web.mvc.dto.response;

import lombok.*;
import web.mvc.domain.Cart;
import web.mvc.domain.CartItem;

import java.util.List;

@Getter
@AllArgsConstructor
public class CartRes {
    private Long cartNo;
    private String userName;
    private Integer totalQuantity;
    private Integer totalAmount;
    private List<CartItemRes> cartItems;

    public CartRes(Cart cart) {
        this.cartNo = cart.getCartNo();
        this.userName = cart.getUser().getUserName();
        this.cartItems = cart.getCartItems().stream().map(CartItemRes::new).toList();
        this.totalQuantity = cartItems.stream().mapToInt(CartItemRes::getQuantity).sum();
        this.totalAmount = cartItems.stream().mapToInt(CartItemRes::getAmount).sum();
    }

}
