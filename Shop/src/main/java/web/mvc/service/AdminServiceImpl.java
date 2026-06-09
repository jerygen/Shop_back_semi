package web.mvc.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final ProductRepository productRepository;
    private final OrdersRepository ordersRepository;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public ProductRes addProduct(ProductReq productReq) {
        if (productRepository.findByProductId(productReq.getProductId()).isPresent()) {
            throw new ProductException(ErrorCode.DUPLICATE_PRODUCT_ID);
        }

        Product product = productReq.toProduct();
        Product savedProduct = productRepository.save(product);
        return new ProductRes(savedProduct);
    }

    @Override
    @Transactional
    public ProductRes updateProduct(String productId, ProductUpdateReq productUpdateReq) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        if (productUpdateReq.getProductName() != null) {
            product.setProductName(productUpdateReq.getProductName());
        }

        if (productUpdateReq.getDescription() != null) {
            product.setDescription(productUpdateReq.getDescription());
        }

        if (productUpdateReq.getPrice() != null) {
            product.setPrice(productUpdateReq.getPrice());
        }

        if (productUpdateReq.getStock() != null) {
            product.setStock(productUpdateReq.getStock());
        }

        return new ProductRes(product);
    }

    @Override
    @Transactional
    public void deleteProduct(String productId) {
        long deleteCount = productRepository.deleteByProductId(productId);
        if(deleteCount == 0){
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public List<AdminOrderRes> findAllOrders() {
        return ordersRepository.findAllWithUserAndOrderLines().stream()
                .map(AdminOrderRes::new)
                .toList();
    }

    @Override
    @Transactional
    public List<AdminUserRes> findAllUsersWithOrderCount() {
        return userRepository.findAllWithOrderCount().stream()
                .map(row -> new AdminUserRes((User) row[0], (Long) row[1]))
                .toList();
    }


}
