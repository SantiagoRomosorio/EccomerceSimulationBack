package com.ecommerce.catalog.adapter.port.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 140) String slug,
        @Size(max = 500) String description
) {
}
