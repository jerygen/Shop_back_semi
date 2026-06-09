package web.mvc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import web.mvc.domain.Orders;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrderRes {
    private Long orderNo;
    private LocalDateTime orderDate;
    private Integer totalAmount;
    private String address;
    private List<OrderLineRes> orderLines;

    public OrderRes(Orders orders) {
        this.orderNo = orders.getOrderNo();
        this.orderDate = orders.getOrderDate();
        this.address = orders.getAddress();
        this.totalAmount = orders.getTotalAmount();
        this.orderLines = orders.getOrderLines().stream()
                .map(OrderLineRes::new)
                .toList();
    }
}
