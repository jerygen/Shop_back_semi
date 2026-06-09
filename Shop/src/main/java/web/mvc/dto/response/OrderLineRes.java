package web.mvc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import web.mvc.domain.OrderLine;

@Getter
@AllArgsConstructor
public class OrderLineRes {
    private Long orderLineNo;
    private Long productNo;
    private String productName;
    private Integer unitPrice;
    private Integer quantity;
    private Integer amount;

    public OrderLineRes(OrderLine orderLine) {
        this.orderLineNo = orderLine.getOrderLineNo();
        this.productNo = orderLine.getProduct().getProductNo();
        this.productName = orderLine.getProduct().getProductName();
        this.unitPrice = orderLine.getUnitPrice();
        this.quantity = orderLine.getQuantity();
        this.amount = orderLine.getAmount();
    }
}
