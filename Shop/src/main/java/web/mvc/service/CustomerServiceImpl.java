package web.mvc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import web.mvc.domain.Product;
import web.mvc.dto.request.ProductReq;
import web.mvc.dto.response.ProductRes;
import web.mvc.exception.ErrorCode;
import web.mvc.exception.ProductException;
import web.mvc.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService{

    private final ProductRepository productRepository;

    @Override
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
    public ProductRes findProduct(ProductReq productReq) {
        Product product = productRepository.findByProductId(productReq.getProductId())
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        return new ProductRes(product);
    }

}
