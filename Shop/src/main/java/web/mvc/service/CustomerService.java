package web.mvc.service;

import web.mvc.domain.Product;
import web.mvc.dto.request.ProductReq;

import java.util.List;

public interface CustomerService   {

    List<ProductReq> findProductAll();

}
