package web.mvc.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import web.mvc.domain.Product;

@Getter
@Setter
@ToString
public class ProductReq {

    private String productId;
    private String productName;
    private Integer price;
    private Integer stock;
    private String description;

    public Product toProduct(ProductReq  productReq) {
        return Product.builder()
                .productId(productReq.getProductId())
                .productName(productReq.getProductName())
                .price(productReq.getPrice())
                .stock(productReq.getStock())
                .description(productReq.getDescription())
        .build();
    }

}
