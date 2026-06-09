package web.mvc.dto.request;

import lombok.*;
import web.mvc.domain.Product;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReq {

    private String productId;
    private String productName;
    private Integer price;
    private Integer stock;
    private String description;

    public Product toProduct() {
        return Product.builder()
                .productId(productId)
                .productName(productName)
                .price(price)
                .stock(stock)
                .description(description)
        .build();
    }

}
