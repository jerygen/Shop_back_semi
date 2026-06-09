package web.mvc.service;

import web.mvc.dto.request.ProductReq;
import web.mvc.dto.response.ProductRes;

import java.util.List;

public interface CustomerService   {

    List<ProductRes> findProductAll();

    ProductRes findProduct(ProductReq productReq);

}
