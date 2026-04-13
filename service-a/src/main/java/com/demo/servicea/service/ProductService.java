package com.demo.servicea.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.servicea.dto.UpdateProductRequest;
import com.demo.servicea.entity.Product;
import com.demo.servicea.messaging.aop.PublishDomainChanges;
import com.demo.servicea.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Bắt buộc return Product — aspect đọc after state từ return value.
     * Nếu return void, aspect sẽ phải flush/reload DB (chậm hơn).
     */
    @Transactional
    @PublishDomainChanges(aggregateType = "product", entityClass = Product.class, idParameterIndex = 0)
    public Product updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPriceCents(request.priceCents());
        return productRepository.save(product);
    }
}