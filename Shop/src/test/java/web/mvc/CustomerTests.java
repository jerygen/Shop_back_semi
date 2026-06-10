package web.mvc;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import web.mvc.domain.Product;
import web.mvc.domain.User;
import web.mvc.dto.request.OrderCreateReq;
import web.mvc.dto.response.OrderRes;
import web.mvc.dto.response.ProductRes;
import web.mvc.repository.ProductRepository;
import web.mvc.repository.UserRepository;
import web.mvc.service.CustomerService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
public class CustomerTests {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createProducts() {
        String suffix = uniqueSuffix();
        Product keyboard = product("A10-" + suffix, "Keyboard", 30000, 10, "Mechanical keyboard");
        Product mouse = product("B20-" + suffix, "Mouse", 15000, 20, "Wireless mouse");

        productRepository.saveAll(List.of(keyboard, mouse));
        List<Product> savedProducts = List.of(
                productRepository.findByProductId(keyboard.getProductId()).orElseThrow(),
                productRepository.findByProductId(mouse.getProductId()).orElseThrow()
        );

        log.info("saved products = {}", savedProducts);

        assertThat(savedProducts).hasSize(2);
    }

    @Test
    void getAllProducts() {
        String suffix = uniqueSuffix();
        Product keyboard = productRepository.save(product("A10-" + suffix, "Keyboard", 30000, 10, "Mechanical keyboard"));
        Product mouse = productRepository.save(product("B20-" + suffix, "Mouse", 15000, 20, "Wireless mouse"));

        List<ProductRes> products = customerService.findProductAll();
        List<ProductRes> testProducts = products.stream()
                .filter(product -> product.getProductNo().equals(keyboard.getProductNo())
                        || product.getProductNo().equals(mouse.getProductNo()))
                .toList();

        log.info("product responses = {}", testProducts);

        assertThat(testProducts).hasSize(2);
        assertThat(testProducts)
                .extracting(ProductRes::getProductName)
                .containsExactlyInAnyOrder("Keyboard", "Mouse");
    }

    @Test
    void getProduct() {
        Product keyboard = product("A10-" + uniqueSuffix(), "Keyboard", 30000, 10, "Mechanical keyboard");
        Product savedProduct = productRepository.save(keyboard);

        ProductRes product = customerService.findProductByNo(savedProduct.getProductNo());

        log.info("saved product = {}", savedProduct);
        log.info("product response = {}", product);

        assertThat(product.getProductName()).isEqualTo("Keyboard");
        assertThat(product.getPrice()).isEqualTo(30000);
        assertThat(product.getStock()).isEqualTo(10);
        assertThat(product.getDescription()).isEqualTo("Mechanical keyboard");
    }

    @Test
    void createOrder() {
        String suffix = uniqueSuffix();
        User user = User.builder()
                .userId("customer01-" + suffix)
                .password("password")
                .userName("Customer")
                .role("ROLE_CUSTOMER")
                .build();
        User savedUser = userRepository.save(user);

        Product keyboard = product("A10-" + suffix, "Keyboard", 30000, 10, "Mechanical keyboard");
        Product savedProduct = productRepository.save(keyboard);

        OrderCreateReq orderCreateReq = OrderCreateReq.builder()
                .productNo(savedProduct.getProductNo())
                .quantity(2)
                .address("Seoul")
                .build();

        OrderRes order = customerService.createOrder(user.getUserId(), orderCreateReq);

        Product orderedProduct = productRepository.findById(savedProduct.getProductNo()).orElseThrow();

        log.info("saved user = {}", savedUser);
        log.info("saved product before order = {}", savedProduct);
        log.info("order create request = {}", orderCreateReq);
        log.info("created order response = {}", order);
        log.info("ordered product after stock decrease = {}", orderedProduct);

        assertThat(order.getOrderNo()).isNotNull();
        assertThat(order.getAddress()).isEqualTo("Seoul");
        assertThat(order.getTotalAmount()).isEqualTo(60000);
        assertThat(order.getOrderLines()).hasSize(1);
        assertThat(order.getOrderLines().get(0).getOrderLineNo()).isNotNull();
        assertThat(order.getOrderLines().get(0).getProductNo()).isEqualTo(savedProduct.getProductNo());
        assertThat(order.getOrderLines().get(0).getProductName()).isEqualTo("Keyboard");
        assertThat(order.getOrderLines().get(0).getUnitPrice()).isEqualTo(30000);
        assertThat(order.getOrderLines().get(0).getQuantity()).isEqualTo(2);
        assertThat(order.getOrderLines().get(0).getAmount()).isEqualTo(60000);
        assertThat(orderedProduct.getStock()).isEqualTo(8);
    }

    @Test
    void getOrders() {
        String suffix = uniqueSuffix();
        User user = User.builder()
                .userId("customer01-" + suffix)
                .password("password")
                .userName("Customer")
                .role("ROLE_CUSTOMER")
                .build();
        User savedUser = userRepository.save(user);

        Product keyboard = product("A10-" + suffix, "Keyboard", 30000, 10, "Mechanical keyboard");
        Product mouse = product("B20-" + suffix, "Mouse", 15000, 20, "Wireless mouse");

        List<Product> savedProducts = productRepository.saveAll(List.of(keyboard, mouse));

        OrderCreateReq keyboardOrderReq = OrderCreateReq.builder()
                .productNo(savedProducts.get(0).getProductNo())
                .quantity(2)
                .address("Seoul")
                .build();

        OrderCreateReq mouseOrderReq = OrderCreateReq.builder()
                .productNo(savedProducts.get(1).getProductNo())
                .quantity(3)
                .address("Seoul")
                .build();

        customerService.createOrder(user.getUserId(), keyboardOrderReq);
        customerService.createOrder(user.getUserId(), mouseOrderReq);

        List<OrderRes> orders = customerService.findOrdersByUserId(user.getUserId());

        log.info("saved user = {}", savedUser);
        log.info("saved products = {}", savedProducts);
        log.info("keyboard order request = {}", keyboardOrderReq);
        log.info("mouse order request = {}", mouseOrderReq);
        log.info("order responses = {}", orders);

        assertThat(orders).hasSize(2);
        assertThat(orders)
                .extracting(OrderRes::getTotalAmount)
                .containsExactlyInAnyOrder(60000, 45000);
        assertThat(orders)
                .flatExtracting(OrderRes::getOrderLines)
                .hasSize(2)
                .allSatisfy(orderLine -> {
                    assertThat(orderLine.getOrderLineNo()).isNotNull();
                    assertThat(orderLine.getProductName()).isIn("Keyboard", "Mouse");
                    assertThat(orderLine.getQuantity()).isIn(2, 3);
                    assertThat(orderLine.getAmount()).isIn(60000, 45000);
                });
        assertThat(orders)
                .flatExtracting(OrderRes::getOrderLines)
                .extracting("productName", "quantity", "amount")
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Keyboard", 2, 60000),
                        org.assertj.core.groups.Tuple.tuple("Mouse", 3, 45000)
                );
    }

    private Product product(String productId, String productName, int price, int stock, String description) {
        return Product.builder()
                .productId(productId)
                .productName(productName)
                .price(price)
                .stock(stock)
                .description(description)
                .build();
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString();
    }
}
