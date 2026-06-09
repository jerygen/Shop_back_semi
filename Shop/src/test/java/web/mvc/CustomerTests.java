package web.mvc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import web.mvc.domain.Product;
import web.mvc.dto.request.ProductReq;
import web.mvc.dto.response.ProductRes;
import web.mvc.repository.ProductRepository;
import web.mvc.service.CustomerService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class CustomerTests {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void createProducts(){
        productRepository.deleteAll();

        Product keyboard = Product.builder()
                .productId("A10")
                .productName("Keyboard")
                .price(30000)
                .stock(10)
                .description("Mechanical keyboard")
                .build();

        Product mouse = Product.builder()
                .productId("B20")
                .productName("Mouse")
                .price(15000)
                .stock(20)
                .description("Wireless mouse")
                .build();

        productRepository.saveAll(List.of(keyboard, mouse));

        assertThat(productRepository.findAll()).hasSize(2);
    }

    @Test
    void getAllProducts() {
        createProducts();

        List<ProductRes> products = customerService.findProductAll();

        assertThat(products).hasSize(2);
        assertThat(products)
                .extracting(ProductRes::getProductName)
                .containsExactlyInAnyOrder("Keyboard", "Mouse");
    }

    @Test
    void getProduct() {
        createProducts();

        ProductReq productReq = ProductReq.builder()
                .productId("A10")
                .build();

        ProductRes product = customerService.findProduct(productReq);

        assertThat(product.getProductName()).isEqualTo("Keyboard");
        assertThat(product.getPrice()).isEqualTo(30000);
        assertThat(product.getStock()).isEqualTo(10);
        assertThat(product.getDescription()).isEqualTo("Mechanical keyboard");
    }
}
