package web.mvc.dto.request;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
public class CartAddReq {
    private Long productNo;
    private Integer quantity;
}
