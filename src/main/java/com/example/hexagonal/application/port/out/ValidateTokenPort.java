package com.example.hexagonal.application.port.out;

public interface ValidateTokenPort {

    boolean isValid(String token);

    String getSubject(String token);
}

