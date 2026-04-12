package com.demo.servicea.service;

import com.demo.servicea.entity.Product;
import com.demo.servicea.messaging.aop.PublishDomainChanges;
import com.demo.servicea.repository.ProductRepository;
import com.demo.servicea.dto.UpdateProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    @PublishDomainChanges(aggregateType = "product", entityClass = Product.class, idParameterIndex = 0)
    public Product updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPriceCents(request.priceCents());
        return productRepository.save(product);
    }
}
