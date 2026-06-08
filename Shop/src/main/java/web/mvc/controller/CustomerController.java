package web.mvc.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import web.mvc.domain.Product;
import web.mvc.dto.request.ProductReq;
import web.mvc.dto.response.ApiResponse;
import web.mvc.service.CustomerService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductReq>>> getAllProducts() {
        List<ProductReq> products = customerService.findProductAll();
        return ResponseEntity.ok(ApiResponse.success(products));
    }


}
