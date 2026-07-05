package com.ecommerce.common.web.autoconfigure;

import com.ecommerce.common.web.error.CommonNotFoundExceptionHandler;
import com.ecommerce.common.web.response.ApiResultFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureAfter(CommonWebAutoConfiguration.class)
public class CommonExceptionHandlerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CommonNotFoundExceptionHandler commonNotFoundExceptionHandler(ApiResultFactory responseFactory) {
        return new CommonNotFoundExceptionHandler(responseFactory);
    }
}
