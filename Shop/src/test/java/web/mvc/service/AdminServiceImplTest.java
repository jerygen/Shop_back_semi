package web.mvc.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import web.mvc.domain.OrderLine;
import web.mvc.domain.Orders;
import web.mvc.domain.Product;
import web.mvc.domain.User;
import web.mvc.dto.request.ProductReq;
import web.mvc.dto.request.ProductUpdateReq;
import web.mvc.dto.response.AdminOrderRes;
import web.mvc.dto.response.AdminUserRes;
import web.mvc.dto.response.ProductRes;
import web.mvc.exception.ErrorCode;
import web.mvc.exception.ProductException;
import web.mvc.repository.OrdersRepository;
import web.mvc.repository.ProductRepository;
import web.mvc.repository.UserRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrdersRepository ordersRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void addProductSavesNewProductAndReturnsResponse() {
        ProductReq request = ProductReq.builder()
                .productId("P001")
                .productName("Keyboard")
                .price(30000)
                .stock(10)
                .description("Mechanical keyboard")
                .build();

        when(productRepository.findByProductId("P001")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setProductNo(1L);
            return product;
        });

        ProductRes result = adminService.addProduct(request);

        assertThat(result.getProductNo()).isEqualTo(1L);
        assertThat(result.getProductName()).isEqualTo("Keyboard");
        assertThat(result.getPrice()).isEqualTo(30000);
        assertThat(result.getStock()).isEqualTo(10);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void addProductRejectsDuplicateProductId() {
        Product existingProduct = product("P001", "Keyboard", 30000, 10);
        ProductReq request = ProductReq.builder()
                .productId("P001")
                .productName("Keyboard")
                .price(30000)
                .stock(10)
                .build();

        when(productRepository.findByProductId("P001")).thenReturn(Optional.of(existingProduct));

        assertThatThrownBy(() -> adminService.addProduct(request))
                .isInstanceOf(ProductException.class)
                .satisfies(exception ->
                        assertThat(((ProductException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.DUPLICATE_PRODUCT_ID));
    }

    @Test
    void updateProductChangesOnlyProvidedFields() {
        Product product = product("P001", "Keyboard", 30000, 10);
        product.setDescription("Old description");

        ProductUpdateReq request = ProductUpdateReq.builder()
                .price(35000)
                .stock(8)
                .build();

        when(productRepository.findByProductId("P001")).thenReturn(Optional.of(product));

        ProductRes result = adminService.updateProduct("P001", request);

        assertThat(result.getProductName()).isEqualTo("Keyboard");
        assertThat(result.getDescription()).isEqualTo("Old description");
        assertThat(result.getPrice()).isEqualTo(35000);
        assertThat(result.getStock()).isEqualTo(8);
        assertThat(product.getPrice()).isEqualTo(35000);
        assertThat(product.getStock()).isEqualTo(8);
    }

    @Test
    void updateProductRejectsMissingProduct() {
        when(productRepository.findByProductId("P404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateProduct("P404", ProductUpdateReq.builder().price(1000).build()))
                .isInstanceOf(ProductException.class)
                .satisfies(exception ->
                        assertThat(((ProductException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Test
    void deleteProductDeletesByProductId() {
        when(productRepository.deleteByProductId("P001")).thenReturn(1L);

        adminService.deleteProduct("P001");

        verify(productRepository).deleteByProductId("P001");
    }

    @Test
    void deleteProductRejectsMissingProduct() {
        when(productRepository.deleteByProductId("P404")).thenReturn(0L);

        assertThatThrownBy(() -> adminService.deleteProduct("P404"))
                .isInstanceOf(ProductException.class)
                .satisfies(exception ->
                        assertThat(((ProductException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Test
    void findAllOrdersReturnsAdminOrderResponses() {
        User user = user("customer01", "Customer", "ROLE_USER");
        Product product = product("P001", "Keyboard", 30000, 10);
        Orders order = order(user, product, 2);

        when(ordersRepository.findAllWithUserAndOrderLines()).thenReturn(List.of(order));

        List<AdminOrderRes> result = adminService.findAllOrders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderNo()).isEqualTo(100L);
        assertThat(result.get(0).getUserId()).isEqualTo("customer01");
        assertThat(result.get(0).getTotalAmount()).isEqualTo(60000);
        assertThat(result.get(0).getOrderLines()).hasSize(1);
    }

    @Test
    void findAllUsersWithOrderCountReturnsUserStatistics() {
        User user = user("customer01", "Customer", "ROLE_USER");
        user.setRegDate(Timestamp.valueOf("2026-06-09 10:00:00"));

        when(userRepository.findAllWithOrderCount()).thenReturn(List.<Object[]>of(new Object[]{user, 3L}));

        List<AdminUserRes> result = adminService.findAllUsersWithOrderCount();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("customer01");
        assertThat(result.get(0).getUserName()).isEqualTo("Customer");
        assertThat(result.get(0).getOrderCount()).isEqualTo(3L);
    }

    private Product product(String productId, String productName, int price, int stock) {
        Product product = Product.builder()
                .productId(productId)
                .productName(productName)
                .price(price)
                .stock(stock)
                .description("description")
                .build();
        product.setProductNo(1L);
        return product;
    }

    private User user(String userId, String userName, String role) {
        User user = User.builder()
                .userId(userId)
                .userName(userName)
                .role(role)
                .build();
        user.setUserNo(10L);
        return user;
    }

    private Orders order(User user, Product product, int quantity) {
        Orders order = Orders.builder()
                .user(user)
                .address("Seoul")
                .totalAmount(product.getPrice() * quantity)
                .orderDate(LocalDateTime.of(2026, 6, 9, 10, 0))
                .build();
        order.setOrderNo(100L);

        OrderLine orderLine = OrderLine.builder()
                .product(product)
                .unitPrice(product.getPrice())
                .quantity(quantity)
                .amount(product.getPrice() * quantity)
                .build();
        orderLine.setOrderLineNo(1000L);
        order.addOrderLine(orderLine);
        return order;
    }
}
