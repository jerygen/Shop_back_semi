package web.mvc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import web.mvc.domain.Product;
import web.mvc.exception.ErrorCode;
import web.mvc.exception.ProductException;
import web.mvc.repository.ProductRepository;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final ProductRepository productRepository;
}
