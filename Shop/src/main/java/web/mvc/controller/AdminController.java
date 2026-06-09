package web.mvc.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import web.mvc.dto.response.AdminOrderRes;
import web.mvc.dto.response.AdminUserRes;
import web.mvc.dto.request.ProductReq;
import web.mvc.dto.request.ProductUpdateReq;
import web.mvc.dto.response.ApiResponse;
import web.mvc.dto.response.ProductRes;
import web.mvc.service.AdminService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductRes>> addProduct(@RequestBody ProductReq productReq) {
        ProductRes productRes = adminService.addProduct(productReq);
        return ResponseEntity.ok(ApiResponse.success(productRes));
    }

    @PatchMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductRes>> updateProduct(@PathVariable String productId, @RequestBody ProductUpdateReq productUpdateReq) {
        ProductRes productRes = adminService.updateProduct(productId, productUpdateReq);
        return ResponseEntity.ok(ApiResponse.success(productRes));
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable String productId) {
        adminService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<AdminOrderRes>>> findAllOrders() {
        List<AdminOrderRes> orders = adminService.findAllOrders();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserRes>>> findAllUsersWithOrderCount() {
        List<AdminUserRes> users = adminService.findAllUsersWithOrderCount();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

}
