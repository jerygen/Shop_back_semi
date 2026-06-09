package web.mvc.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderLineNo;

    @ManyToOne
    @JoinColumn(name = "order_no")
    private Orders orders;

    @ManyToOne
    @JoinColumn(name = "product_no")
    private Product product;

    private Integer unitPrice;
    private Integer quantity;
    private Integer amount;
}
