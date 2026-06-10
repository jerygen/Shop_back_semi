package web.mvc.controller;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import web.mvc.dto.request.CartAddReq;
import web.mvc.dto.request.CartOrderCreateReq;
import web.mvc.dto.request.OrderCreateReq;
import web.mvc.dto.response.*;
import web.mvc.security.CustomUserDetails;
import web.mvc.service.CustomerService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductRes>>> getAllProducts() {
        List<ProductRes> products = customerService.findProductAll();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/products/{productNo}")
    public ResponseEntity<ApiResponse<ProductRes>> getProduct(@PathVariable Long productNo) {
        ProductRes product = customerService.findProductByNo(productNo);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderRes>> createOrder(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody OrderCreateReq orderCreateReq) {
        String userId = userDetails.getUser().getUserId();
        OrderRes orderRes = customerService.createOrder(userId, orderCreateReq);
        return ResponseEntity.ok(ApiResponse.success(orderRes));
    }

    @GetMapping("/orders/me")
    public ResponseEntity<ApiResponse<List<OrderRes>>> getOrders(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails.getUser().getUserId();
        List<OrderRes> orders = customerService.findOrdersByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PostMapping("/cart/item")
    public ResponseEntity<ApiResponse<CartItemRes>> addIntoCart(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody CartAddReq cartAddReq) {
        String userId = userDetails.getUser().getUserId();
        CartItemRes cartItemRes = customerService.addIntoCart(userId, cartAddReq);
        return ResponseEntity.ok(ApiResponse.success(cartItemRes));
    }

    @GetMapping("/cart/items")
    public ResponseEntity<ApiResponse<CartRes>> getCartItems(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails.getUser().getUserId();
        CartRes cartRes = customerService.findCartByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(cartRes));
    }

    @PostMapping("/cart/orders")
    public ResponseEntity<ApiResponse<OrderRes>> createCartOrders(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody CartOrderCreateReq orderCreateReq) {
        String userId = userDetails.getUser().getUserId();
        OrderRes orderRes = customerService.createCartOrders(userId, orderCreateReq);
        return ResponseEntity.ok(ApiResponse.success(orderRes));
    }
}
