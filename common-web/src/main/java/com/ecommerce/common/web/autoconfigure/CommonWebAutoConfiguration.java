package com.ecommerce.common.web.autoconfigure;

import com.ecommerce.common.web.response.ApiResponseFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CommonWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ApiResponseFactory apiResponseFactory() {
        return new ApiResponseFactory();
    }
}
