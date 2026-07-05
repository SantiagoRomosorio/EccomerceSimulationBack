package com.ecommerce.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
class IdentityApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReturnsStandardApiResult() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.developerMessage").doesNotExist())
                .andExpect(jsonPath("$.message").value("Service is healthy"))
                .andExpect(jsonPath("$.path").value("/api/health"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("identity-service"));
    }

    @Test
    void swaggerDocumentsStandardResponsesByEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/users/me'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/users/me'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/users/me'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/users/me'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/users/me'].get.responses['500']").exists());
    }

    @Test
    void registerReturnsCreatedApiResult() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "fullName": "Demo User",
                                  "password": "Password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.result").value("Created"))
                .andExpect(jsonPath("$.developerMessage").doesNotExist())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.roles", hasItem("CUSTOMER")));
    }

    @Test
    void registerReturnsConflictWhenEmailAlreadyExists() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "fullName": "Duplicate User",
                                  "password": "Password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.result").value("Conflict"))
                .andExpect(jsonPath("$.developerMessage").value("ConflictException"))
                .andExpect(jsonPath("$.message").value("Email is already registered"))
                .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    void registerReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "bad",
                                  "fullName": "",
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.result").value("Bad Request"))
                .andExpect(jsonPath("$.developerMessage").value("Request body validation failed"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"))
                .andExpect(jsonPath("$.data.errors", notNullValue()));
    }

    @Test
    void registerReturnsBadRequestWhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "bad-json@example.com",
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.result").value("Bad Request"))
                .andExpect(jsonPath("$.developerMessage").value("HttpMessageNotReadableException"))
                .andExpect(jsonPath("$.message").value("Invalid request body"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    @Test
    void loginReturnsOkWithToken() throws Exception {
        String email = uniqueEmail();
        register(email);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.developerMessage").doesNotExist())
                .andExpect(jsonPath("$.message").value("Login completed successfully"))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresInMinutes").value(60))
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
        String scope = jwtClaim(token, "$.scope");

        assertThat(scope)
                .contains("catalog:read", "cart:manage", "checkout:create", "orders:read:self", "users:read:self")
                .doesNotContain("catalog:write", "stock:manage", "users:manage");
    }

    @Test
    void loginReturnsUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "WrongPassword123"
                                }
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.result").value("Unauthorized"))
                .andExpect(jsonPath("$.developerMessage").value("InvalidCredentialsException"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void meReturnsNotFoundWhenTrustedUserIdDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.result").value("Not Found"))
                .andExpect(jsonPath("$.developerMessage").value("ResourceNotFoundException"))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void meReturnsCurrentUserWhenTrustedHeaderExists() throws Exception {
        String email = uniqueEmail();
        String userId = registerAndReturnId(email);

        mockMvc.perform(get("/api/users/me")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.developerMessage").doesNotExist())
                .andExpect(jsonPath("$.message").value("Current user returned successfully"))
                .andExpect(jsonPath("$.path").value("/api/users/me"))
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.roles", hasItem("CUSTOMER")));
    }

    @Test
    void meReturnsUnauthorizedWhenTrustedHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.result").value("Unauthorized"))
                .andExpect(jsonPath("$.developerMessage")
                        .value("The request must pass through api-gateway so X-User-* headers are injected"))
                .andExpect(jsonPath("$.message").value("Missing trusted identity header"))
                .andExpect(jsonPath("$.data.headerName").value("X-User-Id"));
    }

    @Test
    void meReturnsBadRequestWhenTrustedHeaderIsNotUuid() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("X-User-Id", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.result").value("Bad Request"))
                .andExpect(jsonPath("$.developerMessage").value("MethodArgumentTypeMismatchException"))
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.path").value("/api/users/me"))
                .andExpect(jsonPath("$.data.name").value("X-User-Id"))
                .andExpect(jsonPath("$.data.requiredType").value("UUID"));
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "fullName": "Demo User",
                                  "password": "Password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private String registerAndReturnId(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "fullName": "Demo User",
                                  "password": "Password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
    }

    private String uniqueEmail() {
        return "user-%s@example.com".formatted(UUID.randomUUID());
    }

    private String jwtClaim(String token, String jsonPath) {
        String[] tokenParts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);

        return JsonPath.read(payload, jsonPath);
    }
}
