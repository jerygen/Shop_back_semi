package web.mvc.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateReq {
    private Long productNo;
    private Integer quantity;
    private String address;
}
