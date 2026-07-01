package com.example.hexagonal.application.port.out;

import java.util.Map;

public interface GenerateTokenPort {

    String generate(String subject, Map<String, Object> claims);
}

