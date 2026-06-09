package web.mvc.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateReq {
    private String productName;
    private String description;
    private Integer price;
    private Integer stock;
}
