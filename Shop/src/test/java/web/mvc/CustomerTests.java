package web.mvc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import web.mvc.domain.Product;
import web.mvc.domain.User;
import web.mvc.dto.request.OrderCreateReq;
import web.mvc.dto.response.OrderRes;
import web.mvc.dto.response.ProductRes;
import web.mvc.repository.OrdersRepository;
import web.mvc.repository.ProductRepository;
import web.mvc.repository.UserRepository;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @BeforeEach
    void clearData() {
        ordersRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createProducts() {
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
        products.forEach(System.out::println);
    }

    @Test
    void getProduct() {
        Product keyboard = Product.builder()
                .productId("A10")
                .productName("Keyboard")
                .price(30000)
                .stock(10)
                .description("Mechanical keyboard")
                .build();

        Product savedProduct = productRepository.save(keyboard);

        ProductRes product = customerService.findProductByNo(savedProduct.getProductNo());

        assertThat(product.getProductName()).isEqualTo("Keyboard");
        assertThat(product.getPrice()).isEqualTo(30000);
        assertThat(product.getStock()).isEqualTo(10);
        assertThat(product.getDescription()).isEqualTo("Mechanical keyboard");

        System.out.println(product);
    }

    @Test
    void createOrder() {
        User user = User.builder()
                .userId("customer01")
                .password("password")
                .userName("Customer")
                .role("ROLE_CUSTOMER")
                .build();
        userRepository.save(user);

        Product keyboard = Product.builder()
                .productId("A10")
                .productName("Keyboard")
                .price(30000)
                .stock(10)
                .description("Mechanical keyboard")
                .build();
        Product savedProduct = productRepository.save(keyboard);

        OrderCreateReq orderCreateReq = OrderCreateReq.builder()
                .productNo(savedProduct.getProductNo())
                .quantity(2)
                .address("Seoul")
                .build();

        OrderRes order = customerService.createOrder(user.getUserId(), orderCreateReq);

        Product orderedProduct = productRepository.findById(savedProduct.getProductNo()).orElseThrow();

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
        User user = User.builder()
                .userId("customer01")
                .password("password")
                .userName("Customer")
                .role("ROLE_CUSTOMER")
                .build();
        userRepository.save(user);

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
}
