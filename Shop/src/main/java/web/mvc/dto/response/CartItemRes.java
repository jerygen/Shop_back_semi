package web.mvc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import web.mvc.domain.CartItem;

@Getter
@AllArgsConstructor
public class CartItemRes {
    private Long cartItemNo;
    private Long productNo;
    private String productName;
    private Integer unitPrice;
    private Integer quantity;
    private Integer amount;

    public CartItemRes(CartItem cartItem) {
        this.cartItemNo = cartItem.getCartItemNo();
        this.productNo = cartItem.getProduct().getProductNo();
        this.productName = cartItem.getProduct().getProductName();
        this.unitPrice = cartItem.getProduct().getPrice();
        this.quantity = cartItem.getQuantity();
        this.amount = this.unitPrice * this.quantity;
    }
}
