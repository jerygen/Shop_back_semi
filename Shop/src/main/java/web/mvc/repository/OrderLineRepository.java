package web.mvc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import web.mvc.domain.OrderLine;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
}
