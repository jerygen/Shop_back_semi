package web.mvc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import web.mvc.domain.Orders;

import java.util.List;

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    @Query("""
            select distinct o
            from Orders o
            join fetch o.orderLines ol
            join fetch ol.product
            where o.user.userId = :userId
            order by o.orderDate desc
            """)
    List<Orders> findAllByUserIdWithOrderLines(String userId);
}
