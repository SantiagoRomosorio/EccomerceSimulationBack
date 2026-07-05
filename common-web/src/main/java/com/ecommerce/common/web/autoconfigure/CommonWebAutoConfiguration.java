package com.ecommerce.common.web.autoconfigure;

import com.ecommerce.common.web.openapi.OpenApiSecurity;
import com.ecommerce.common.web.response.ApiResponseFactory;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@SecurityScheme(
        name = OpenApiSecurity.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class CommonWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ApiResponseFactory apiResponseFactory() {
        return new ApiResponseFactory();
    }
}
