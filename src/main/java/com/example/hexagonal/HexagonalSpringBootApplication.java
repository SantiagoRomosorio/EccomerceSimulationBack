package com.example.hexagonal;

import com.example.hexagonal.config.properties.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class HexagonalSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(HexagonalSpringBootApplication.class, args);
    }
}

