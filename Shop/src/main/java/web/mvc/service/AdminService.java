package web.mvc.service;

import web.mvc.dto.request.ProductReq;
import web.mvc.dto.request.ProductUpdateReq;
import web.mvc.dto.response.AdminOrderRes;
import web.mvc.dto.response.AdminUserRes;
import web.mvc.dto.response.ProductRes;

import java.util.List;

public interface AdminService {
    ProductRes addProduct(ProductReq productReq);

    ProductRes updateProduct(String productId, ProductUpdateReq productUpdateReq);

    void deleteProduct(String productId);

    List<AdminOrderRes> findAllOrders();

    List<AdminUserRes> findAllUsersWithOrderCount();
}
