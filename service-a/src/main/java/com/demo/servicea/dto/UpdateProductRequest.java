package com.demo.servicea.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 4000) String description,
        @Min(0) long priceCents
) {
}
