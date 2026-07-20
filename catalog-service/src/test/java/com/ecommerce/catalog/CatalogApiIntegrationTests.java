package com.ecommerce.catalog;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReturnsStandardApiResponse() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.message").value("Service is healthy"))
                .andExpect(jsonPath("$.data.service").value("catalog-service"));
    }

    @Test
    void swaggerDocumentsStandardResponsesByEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.paths['/api/categories'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/categories'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/categories'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/categories'].post.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/categories'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/categories'].get.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/brands'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/brands'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/brands'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/brands'].post.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/brands'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/brands'].get.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/products'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/products'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/products'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/products'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/products'].post.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/products'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/products'].get.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}'].get.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}/stock'].patch.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}/stock'].patch.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}/stock'].patch.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}/stock'].patch.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/categories'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/categories'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/brands'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/brands'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/products'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/products'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/products/{id}'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/products/{id}/stock'].patch.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/internal/products/stock/reservations'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/internal/products/stock/releases'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/health'].get.security").doesNotExist());
    }

    @Test
    void createCategoryReturnsCreatedApiResponse() throws Exception {
        String slug = uniqueSlug("technology");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Technology",
                                  "slug": "%s",
                                  "description": "Devices and accessories"
                                }
                                """.formatted(slug)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.result").value("Created"))
                .andExpect(jsonPath("$.message").value("Category created successfully"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.slug").value(slug));
    }

    @Test
    void listCategoriesReturnsOkApiResponse() throws Exception {
        String slug = uniqueSlug("category-list");
        createCategory(slug);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.message").value("Categories returned successfully"))
                .andExpect(jsonPath("$.data[*].slug", hasItem(slug)));
    }

    @Test
    void createCategoryReturnsConflictWhenSlugAlreadyExists() throws Exception {
        String slug = uniqueSlug("category-conflict");
        createCategory(slug);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Category Duplicate",
                                  "slug": "%s",
                                  "description": "Duplicated slug"
                                }
                                """.formatted(slug)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.result").value("Conflict"))
                .andExpect(jsonPath("$.developerMessage").value("ConflictException"))
                .andExpect(jsonPath("$.message").value("Category slug is already registered"))
                .andExpect(jsonPath("$.data.slug").value(slug));
    }

    @Test
    void createCategoryReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "slug": "",
                                  "description": "Invalid category"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.result").value("Bad Request"))
                .andExpect(jsonPath("$.developerMessage").value("Request body validation failed"))
                .andExpect(jsonPath("$.data.errors", notNullValue()));
    }

    @Test
    void createBrandReturnsCreatedApiResponse() throws Exception {
        String slug = uniqueSlug("brand-create");

        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Brand",
                                  "slug": "%s",
                                  "description": "Brand description"
                                }
                                """.formatted(slug)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.result").value("Created"))
                .andExpect(jsonPath("$.message").value("Brand created successfully"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.slug").value(slug));
    }

    @Test
    void listBrandsReturnsOkApiResponse() throws Exception {
        String slug = uniqueSlug("brand-list");
        createBrand(slug);

        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.message").value("Brands returned successfully"))
                .andExpect(jsonPath("$.data[*].slug", hasItem(slug)));
    }

    @Test
    void createBrandReturnsConflictWhenSlugAlreadyExists() throws Exception {
        String slug = uniqueSlug("acme");
        createBrand(slug);

        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "ACME Duplicate",
                                  "slug": "%s",
                                  "description": "Duplicated slug"
                                }
                                """.formatted(slug)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.result").value("Conflict"))
                .andExpect(jsonPath("$.developerMessage").value("ConflictException"))
                .andExpect(jsonPath("$.message").value("Brand slug is already registered"))
                .andExpect(jsonPath("$.data.slug").value(slug));
    }

    @Test
    void createBrandReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "slug": "",
                                  "description": "Invalid brand"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.result").value("Bad Request"))
                .andExpect(jsonPath("$.developerMessage").value("Request body validation failed"))
                .andExpect(jsonPath("$.data.errors", notNullValue()));
    }

    @Test
    void createProductReturnsCreatedApiResponse() throws Exception {
        String categoryId = createCategory(uniqueSlug("shoes"));
        String brandId = createBrand(uniqueSlug("runner"));
        String sku = uniqueSlug("sku").toUpperCase();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "%s",
                                  "name": "Running Shoes",
                                  "description": "Lightweight shoes",
                                  "price": 249900.00,
                                  "currency": "COP",
                                  "categoryId": "%s",
                                  "brandId": "%s",
                                  "stockQuantity": 12
                                }
                                """.formatted(sku, categoryId, brandId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.result").value("Created"))
                .andExpect(jsonPath("$.message").value("Product created successfully"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.sku").value(sku))
                .andExpect(jsonPath("$.data.stockQuantity").value(12));
    }

    @Test
    void listProductsReturnsOkApiResponse() throws Exception {
        String productId = createProduct();

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.message").value("Products returned successfully"))
                .andExpect(jsonPath("$.data[*].id", hasItem(productId)));
    }

    @Test
    void getProductReturnsOkApiResponse() throws Exception {
        String productId = createProduct();

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.message").value("Product returned successfully"))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.stockQuantity").value(8));
    }

    @Test
    void createProductReturnsConflictWhenSkuAlreadyExists() throws Exception {
        String categoryId = createCategory(uniqueSlug("category-sku"));
        String brandId = createBrand(uniqueSlug("brand-sku"));
        String sku = uniqueSlug("sku-conflict").toUpperCase();
        createProduct(categoryId, brandId, sku);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "%s",
                                  "name": "Duplicate Product",
                                  "description": "Duplicated SKU",
                                  "price": 100000.00,
                                  "currency": "COP",
                                  "categoryId": "%s",
                                  "brandId": "%s",
                                  "stockQuantity": 4
                                }
                                """.formatted(sku, categoryId, brandId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.result").value("Conflict"))
                .andExpect(jsonPath("$.developerMessage").value("ConflictException"))
                .andExpect(jsonPath("$.message").value("Product SKU is already registered"))
                .andExpect(jsonPath("$.data.sku").value(sku));
    }

    @Test
    void createProductReturnsNotFoundWhenCategoryDoesNotExist() throws Exception {
        String brandId = createBrand(uniqueSlug("brand-missing-category"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "%s",
                                  "name": "Orphan Product",
                                  "description": "Missing category",
                                  "price": 100000.00,
                                  "currency": "COP",
                                  "categoryId": "%s",
                                  "brandId": "%s",
                                  "stockQuantity": 4
                                }
                                """.formatted(uniqueSlug("sku").toUpperCase(), UUID.randomUUID(), brandId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.result").value("Not Found"))
                .andExpect(jsonPath("$.developerMessage").value("ResourceNotFoundException"))
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    void createProductReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "",
                                  "name": "",
                                  "price": 0,
                                  "currency": "CO",
                                  "stockQuantity": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.result").value("Bad Request"))
                .andExpect(jsonPath("$.developerMessage").value("Request body validation failed"))
                .andExpect(jsonPath("$.data.errors", notNullValue()));
    }

    @Test
    void productStockRequestsRejectValuesAboveMaximum() throws Exception {
        UUID productId = UUID.randomUUID();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "MAX-STOCK",
                                  "name": "Maximum Stock Product",
                                  "price": 1.00,
                                  "currency": "USD",
                                  "categoryId": "%s",
                                  "brandId": "%s",
                                  "stockQuantity": 1000001
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.developerMessage").value("Request body validation failed"))
                .andExpect(jsonPath("$.data.errors[0].field").value("stockQuantity"));

        mockMvc.perform(patch("/api/products/{id}/stock", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stockQuantity": 1000001
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.developerMessage").value("Request body validation failed"))
                .andExpect(jsonPath("$.data.errors[0].field").value("stockQuantity"));
    }

    @Test
    void createProductReturnsBadRequestWhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "BAD-JSON",
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.result").value("Bad Request"))
                .andExpect(jsonPath("$.developerMessage").value("HttpMessageNotReadableException"))
                .andExpect(jsonPath("$.message").value("Invalid request body"))
                .andExpect(jsonPath("$.path").value("/api/products"));
    }

    @Test
    void getProductReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/products/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.result").value("Not Found"))
                .andExpect(jsonPath("$.developerMessage").value("ResourceNotFoundException"))
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    void getProductReturnsBadRequestWhenIdIsNotUuid() throws Exception {
        mockMvc.perform(get("/api/products/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.result").value("Bad Request"))
                .andExpect(jsonPath("$.developerMessage").value("MethodArgumentTypeMismatchException"))
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.path").value("/api/products/not-a-uuid"))
                .andExpect(jsonPath("$.data.name").value("id"))
                .andExpect(jsonPath("$.data.requiredType").value("UUID"));
    }

    @Test
    void updateStockReturnsOkApiResponse() throws Exception {
        String productId = createProduct();

        mockMvc.perform(patch("/api/products/{id}/stock", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stockQuantity": 25
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("PATCH"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.message").value("Product stock updated successfully"))
                .andExpect(jsonPath("$.data.stockQuantity").value(25));
    }

    @Test
    void updateStockReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
        String productId = createProduct();

        mockMvc.perform(patch("/api/products/{id}/stock", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stockQuantity": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.method").value("PATCH"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.result").value("Bad Request"))
                .andExpect(jsonPath("$.developerMessage").value("Request body validation failed"))
                .andExpect(jsonPath("$.data.errors", notNullValue()));
    }

    @Test
    void updateStockReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(patch("/api/products/{id}/stock", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stockQuantity": 10
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.method").value("PATCH"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.result").value("Not Found"))
                .andExpect(jsonPath("$.developerMessage").value("ResourceNotFoundException"))
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    void reserveStockReturnsOkAndDecrementsProductStock() throws Exception {
        String productId = createProduct();

        mockMvc.perform(post("/api/internal/products/stock/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "quantity": 3
                                    }
                                  ]
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.message").value("Product stock reserved successfully"))
                .andExpect(jsonPath("$.data[0].id").value(productId))
                .andExpect(jsonPath("$.data[0].stockQuantity").value(5));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockQuantity").value(5));
    }

    @Test
    void reserveStockReturnsConflictWhenStockIsInsufficient() throws Exception {
        String productId = createProduct();

        mockMvc.perform(post("/api/internal/products/stock/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "quantity": 99
                                    }
                                  ]
                                }
                                """.formatted(productId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.result").value("Conflict"))
                .andExpect(jsonPath("$.developerMessage").value("ConflictException"))
                .andExpect(jsonPath("$.message").value("Insufficient product stock"))
                .andExpect(jsonPath("$.data.availableQuantity").value(8));
    }

    @Test
    void reserveStockReturnsBadRequestWhenQuantityExceedsMaximum() throws Exception {
        mockMvc.perform(post("/api/internal/products/stock/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "quantity": 1001
                                    }
                                  ]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.developerMessage").value("Request body validation failed"))
                .andExpect(jsonPath("$.data.errors[0].field").value("items[0].quantity"));
    }

    @Test
    void releaseStockReturnsOkAndIncrementsProductStock() throws Exception {
        String productId = createProduct();

        mockMvc.perform(post("/api/internal/products/stock/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "quantity": 3
                                    }
                                  ]
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stockQuantity").value(5));

        mockMvc.perform(post("/api/internal/products/stock/releases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "quantity": 3
                                    }
                                  ]
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.message").value("Product stock released successfully"))
                .andExpect(jsonPath("$.data[0].id").value(productId))
                .andExpect(jsonPath("$.data[0].stockQuantity").value(8));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockQuantity").value(8));
    }

    private String createCategory(String slug) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Category",
                                  "slug": "%s",
                                  "description": "Category description"
                                }
                                """.formatted(slug)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private String createBrand(String slug) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Brand",
                                  "slug": "%s",
                                  "description": "Brand description"
                                }
                                """.formatted(slug)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private String createProduct() throws Exception {
        String categoryId = createCategory(uniqueSlug("category"));
        String brandId = createBrand(uniqueSlug("brand"));
        return createProduct(categoryId, brandId, uniqueSlug("sku").toUpperCase());
    }

    private String createProduct(String categoryId, String brandId, String sku) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "%s",
                                  "name": "Backpack",
                                  "description": "Urban backpack",
                                  "price": 120000.00,
                                  "currency": "COP",
                                  "categoryId": "%s",
                                  "brandId": "%s",
                                  "stockQuantity": 8
                                }
                                """.formatted(sku, categoryId, brandId)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private String extractId(MvcResult result) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
    }

    private String uniqueSlug(String prefix) {
        return "%s-%s".formatted(prefix, UUID.randomUUID());
    }
}
