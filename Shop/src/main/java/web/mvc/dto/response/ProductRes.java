package web.mvc.dto.response;

import lombok.*;
import web.mvc.domain.Product;

@Getter
@AllArgsConstructor
@ToString
public class ProductRes {
    private Long productNo;
    private String productName;
    private Integer stock;
    private Integer price;
    private String description;

    public ProductRes(Product  product) {
        this.productNo = product.getProductNo();
        this.productName = product.getProductName();
        this.stock = product.getStock();
        this.price = product.getPrice();
        this.description = product.getDescription();
    }

}
