package web.mvc.service;

import web.mvc.dto.request.CartAddReq;
import web.mvc.dto.request.OrderCreateReq;
import web.mvc.dto.response.CartItemRes;
import web.mvc.dto.response.CartRes;
import web.mvc.dto.response.OrderRes;
import web.mvc.dto.response.ProductRes;

import java.util.List;

public interface CustomerService   {

    List<ProductRes> findProductAll();

    ProductRes findProductByNo(Long productNo);

    OrderRes createOrder(String userId, OrderCreateReq orderCreateReq);

    List<OrderRes> findOrdersByUserId(String userId);

    CartItemRes addIntoCart(String userId, CartAddReq cartAddReq);

    CartRes findCartByUserId(String userId);

    OrderRes createCartOrders(String userId, OrderCreateReq orderCreateReq);
}
