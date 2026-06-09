package web.mvc.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import web.mvc.dto.request.ProductReq;
import web.mvc.dto.response.ApiResponse;
import web.mvc.dto.response.ProductRes;
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

    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductRes>> getProduct(@PathVariable String productId) {
        ProductReq productReq = new ProductReq();
        productReq.setProductId(productId);

        ProductRes product = customerService.findProduct(productReq);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

}
