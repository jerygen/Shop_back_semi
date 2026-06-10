package web.mvc.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import web.mvc.domain.*;
import web.mvc.dto.request.CartAddReq;
import web.mvc.dto.request.CartOrderCreateReq;
import web.mvc.dto.request.OrderCreateReq;
import web.mvc.dto.response.CartItemRes;
import web.mvc.dto.response.CartRes;
import web.mvc.dto.response.OrderRes;
import web.mvc.exception.*;
import web.mvc.repository.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrdersRepository ordersRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    void findProductAllRejectsEmptyProducts() {
        when(productRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> customerService.findProductAll())
                .isInstanceOf(ProductException.class)
                .satisfies(exception ->
                        assertThat(((ProductException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Test
    void createOrderRejectsInsufficientStock() {
        User user = user("customer01");
        Product product = product(1L, "Keyboard", 30000, 1);
        OrderCreateReq request = OrderCreateReq.builder()
                .productNo(1L)
                .quantity(2)
                .address("Seoul")
                .build();

        when(userRepository.findByUserId("customer01")).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> customerService.createOrder("customer01", request))
                .isInstanceOf(ProductException.class)
                .satisfies(exception ->
                        assertThat(((ProductException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK));
    }

    @Test
    void addIntoCartCreatesCartAndCartItemWhenCartDoesNotExist() {
        User user = user("customer01");
        Product product = product(1L, "Keyboard", 30000, 10);
        Cart savedCart = cart(1L, user);
        CartAddReq request = CartAddReq.builder()
                .productNo(1L)
                .quantity(2)
                .build();

        when(userRepository.findByUserId("customer01")).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.findCartsByUserId("customer01")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(savedCart);
        when(cartItemRepository.findByCartAndProduct(savedCart, product)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem cartItem = invocation.getArgument(0);
            cartItem.setCartItemNo(10L);
            return cartItem;
        });

        CartItemRes result = customerService.addIntoCart("customer01", request);

        assertThat(result.getCartItemNo()).isEqualTo(10L);
        assertThat(result.getProductName()).isEqualTo("Keyboard");
        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(result.getAmount()).isEqualTo(60000);
        verify(cartRepository).save(any(Cart.class));
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addIntoCartIncreasesQuantityWhenCartItemAlreadyExists() {
        User user = user("customer01");
        Product product = product(1L, "Keyboard", 30000, 10);
        Cart cart = cart(1L, user);
        CartItem cartItem = cartItem(10L, cart, product, 2);
        CartAddReq request = CartAddReq.builder()
                .productNo(1L)
                .quantity(3)
                .build();

        when(userRepository.findByUserId("customer01")).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.findCartsByUserId("customer01")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartAndProduct(cart, product)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(cartItem)).thenReturn(cartItem);

        CartItemRes result = customerService.addIntoCart("customer01", request);

        assertThat(result.getQuantity()).isEqualTo(5);
        assertThat(result.getAmount()).isEqualTo(150000);
    }

    @Test
    void findCartByUserIdReturnsCartSummary() {
        User user = user("customer01");
        Product keyboard = product(1L, "Keyboard", 30000, 10);
        Product mouse = product(2L, "Mouse", 15000, 20);
        Cart cart = cart(1L, user);
        cart.addCartItem(cartItem(10L, cart, keyboard, 2));
        cart.addCartItem(cartItem(11L, cart, mouse, 3));

        when(cartRepository.findCartsByUserId("customer01")).thenReturn(Optional.of(cart));

        CartRes result = customerService.findCartByUserId("customer01");

        assertThat(result.getCartNo()).isEqualTo(1L);
        assertThat(result.getUserName()).isEqualTo("Customer");
        assertThat(result.getTotalQuantity()).isEqualTo(5);
        assertThat(result.getTotalAmount()).isEqualTo(105000);
        assertThat(result.getCartItems()).hasSize(2);
    }

    @Test
    void findCartByUserIdRejectsMissingCart() {
        when(cartRepository.findCartsByUserId("customer01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findCartByUserId("customer01"))
                .isInstanceOf(CartException.class)
                .satisfies(exception ->
                        assertThat(((CartException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.CART_EMPTY));
    }

    @Test
    void createCartOrdersCreatesOrderFromCartItemsAndReducesStock() {
        User user = user("customer01");
        Product keyboard = product(1L, "Keyboard", 30000, 10);
        Product mouse = product(2L, "Mouse", 15000, 20);
        Cart cart = cart(1L, user);
        cart.addCartItem(cartItem(10L, cart, keyboard, 2));
        cart.addCartItem(cartItem(11L, cart, mouse, 3));
        CartOrderCreateReq request = CartOrderCreateReq.builder()
                .address("Seoul")
                .build();

        when(userRepository.findByUserId("customer01")).thenReturn(Optional.of(user));
        when(cartRepository.findCartsByUserId("customer01")).thenReturn(Optional.of(cart));
        when(ordersRepository.save(any(Orders.class))).thenAnswer(invocation -> {
            Orders orders = invocation.getArgument(0);
            orders.setOrderNo(100L);
            long orderLineNo = 1000L;
            for (OrderLine orderLine : orders.getOrderLines()) {
                orderLine.setOrderLineNo(orderLineNo++);
            }
            return orders;
        });

        OrderRes result = customerService.createCartOrders("customer01", request);

        assertThat(result.getOrderNo()).isEqualTo(100L);
        assertThat(result.getAddress()).isEqualTo("Seoul");
        assertThat(result.getTotalAmount()).isEqualTo(105000);
        assertThat(result.getOrderLines()).hasSize(2);
        assertThat(result.getOrderLines())
                .extracting("productName", "quantity", "amount")
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Keyboard", 2, 60000),
                        org.assertj.core.groups.Tuple.tuple("Mouse", 3, 45000)
                );
        assertThat(keyboard.getStock()).isEqualTo(8);
        assertThat(mouse.getStock()).isEqualTo(17);
        assertThat(cart.getCartItems()).isEmpty();
    }

    @Test
    void createCartOrdersRejectsEmptyCart() {
        User user = user("customer01");
        Cart cart = cart(1L, user);

        when(userRepository.findByUserId("customer01")).thenReturn(Optional.of(user));
        when(cartRepository.findCartsByUserId("customer01")).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> customerService.createCartOrders("customer01", CartOrderCreateReq.builder().address("Seoul").build()))
                .isInstanceOf(CartException.class)
                .satisfies(exception ->
                        assertThat(((CartException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.CART_EMPTY));
    }

    @Test
    void findOrdersByUserIdRejectsEmptyOrders() {
        when(ordersRepository.findAllByUserIdWithOrderLines("customer01")).thenReturn(List.of());

        assertThatThrownBy(() -> customerService.findOrdersByUserId("customer01"))
                .isInstanceOf(OrderException.class)
                .satisfies(exception ->
                        assertThat(((OrderException) exception).getErrorCode())
                                .isEqualTo(ErrorCode.ORDER_NOT_FOUND));
    }

    private User user(String userId) {
        User user = User.builder()
                .userId(userId)
                .userName("Customer")
                .role("ROLE_USER")
                .build();
        user.setUserNo(1L);
        return user;
    }

    private Product product(Long productNo, String productName, int price, int stock) {
        Product product = Product.builder()
                .productId("P" + productNo)
                .productName(productName)
                .price(price)
                .stock(stock)
                .description("description")
                .build();
        product.setProductNo(productNo);
        return product;
    }

    private Cart cart(Long cartNo, User user) {
        Cart cart = Cart.builder()
                .user(user)
                .build();
        cart.setCartNo(cartNo);
        return cart;
    }

    private CartItem cartItem(Long cartItemNo, Cart cart, Product product, int quantity) {
        CartItem cartItem = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(quantity)
                .build();
        cartItem.setCartItemNo(cartItemNo);
        return cartItem;
    }
}
