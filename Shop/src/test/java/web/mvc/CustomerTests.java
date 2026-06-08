package web.mvc;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import web.mvc.domain.Product;
import web.mvc.service.CustomerService;

import java.util.List;

@SpringBootTest
@RequiredArgsConstructor
public class CustomerTests {

    @Autowired
    private final CustomerService customerService;

    @Test
    void createProducts(){

    }

    @Test
    void getAll() {
        List<Product> products = customerService.findProductAll();
        System.out.println(products);
    }
}
