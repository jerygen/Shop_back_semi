package web.mvc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.mvc.domain.*;
import web.mvc.dto.request.CartAddReq;
import web.mvc.dto.request.OrderCreateReq;
import web.mvc.dto.response.CartItemRes;
import web.mvc.dto.response.CartRes;
import web.mvc.dto.response.OrderRes;
import web.mvc.dto.response.ProductRes;
import web.mvc.exception.*;
import web.mvc.repository.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService{

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductRes> findProductAll() {
        List<Product> products = productRepository.findAll();
        if(products.isEmpty()){
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return products.stream()
                .map(ProductRes::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductRes findProductByNo(Long productNo) {
        Product product = productRepository.findById(productNo)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
        return new ProductRes(product);
    }

    @Override
    @Transactional
    public OrderRes createOrder(String userId, OrderCreateReq orderCreateReq) {
        User user = userRepository.findByUserId(userId).orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(orderCreateReq.getProductNo()).orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        if(product.getStock() < orderCreateReq.getQuantity()){
            throw new ProductException(ErrorCode.INSUFFICIENT_STOCK);
        }

        int unitPrice = product.getPrice();
        int amount = unitPrice * orderCreateReq.getQuantity();

        Orders orders = Orders.builder()
                .user(user)
                .address(orderCreateReq.getAddress())
                .totalAmount(amount)
                .build();


        OrderLine orderLine = OrderLine.builder()
                .amount(amount)
                .product(product)
                .quantity(orderCreateReq.getQuantity())
                .unitPrice(unitPrice)
                .build();

        orders.addOrderLine(orderLine);

        product.setStock(product.getStock() - orderCreateReq.getQuantity());

        Orders savedOrder = ordersRepository.save(orders);

        return new OrderRes(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderRes> findOrdersByUserId(String userId) {
        List<Orders> orders = ordersRepository.findAllByUserIdWithOrderLines(userId);
        if (orders.isEmpty()) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }

        return orders.stream()
                .map(OrderRes::new)
                .toList();
    }

    @Override
    @Transactional
    public CartItemRes addIntoCart(String userId, CartAddReq cartAddReq) {
        User user = userRepository.findByUserId(userId).orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findById(cartAddReq.getProductNo()).orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
        if(product.getStock() < cartAddReq.getQuantity()){
            throw new ProductException(ErrorCode.INSUFFICIENT_STOCK);
        }

        Cart cart = cartRepository.findCartsByUserId(userId)
                .orElseGet(()->cartRepository.save(Cart.builder()
                .user(user)
                .build()));

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElse(null);

        if(cartItem == null){
            cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(cartAddReq.getQuantity())
                    .build();

            cart.addCartItem(cartItem);
        }else {
            cartItem.setQuantity(cartItem.getQuantity() + cartAddReq.getQuantity());
        }

        CartItem savedCartItem = cartItemRepository.save(cartItem);

        return new CartItemRes(savedCartItem);
    }

    @Override
    @Transactional(readOnly = true)
    public CartRes findCartByUserId(String userId) {
        Cart cart = cartRepository.findCartsByUserId(userId).orElseThrow(() -> new CartException(ErrorCode.CART_EMPTY));
        return new CartRes(cart);
    }

    @Override
    @Transactional
    public OrderRes createCartOrders(String userId, OrderCreateReq orderCreateReq) {
        User user = userRepository.findByUserId(userId).orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        Cart cart = cartRepository.findCartsByUserId(userId).orElseThrow(() -> new CartException(ErrorCode.CART_EMPTY));

        if(cart.getCartItems().isEmpty()){
            throw new CartException(ErrorCode.CART_EMPTY);
        }

        Orders orders = Orders.builder()
                .address(orderCreateReq.getAddress())
                .user(user)
                .totalAmount(0)
                .build();

        int totalAmount = 0;

        for(CartItem cartItem : cart.getCartItems()){
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();
            if(product.getStock() < quantity){
                throw new ProductException(ErrorCode.INSUFFICIENT_STOCK);
            }

            int unitPrice = product.getPrice();
            int amount = unitPrice * quantity;

            OrderLine orderLine = OrderLine.builder()
                    .amount(amount)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .build();

            orders.addOrderLine(orderLine);

            product.setStock(product.getStock() - quantity);
            totalAmount += amount;
        }
        orders.setTotalAmount(totalAmount);
        Orders savedOrder = ordersRepository.save(orders);
        cart.getCartItems().clear();

        return new OrderRes(savedOrder);
    }


}
