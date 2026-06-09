package web.mvc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import web.mvc.domain.Orders;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class AdminOrderRes {
    private Long orderNo;
    private Long userNo;
    private String userId;
    private String userName;
    private LocalDateTime orderDate;
    private Integer totalAmount;
    private String address;
    private List<OrderLineRes> orderLines;

    public AdminOrderRes(Orders orders) {
        this.orderNo = orders.getOrderNo();
        this.userNo = orders.getUser().getUserNo();
        this.userId = orders.getUser().getUserId();
        this.userName = orders.getUser().getUserName();
        this.orderDate = orders.getOrderDate();
        this.totalAmount = orders.getTotalAmount();
        this.address = orders.getAddress();
        this.orderLines = orders.getOrderLines().stream()
                .map(OrderLineRes::new)
                .toList();
    }
}
