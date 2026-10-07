package com.employeehub.dto;

public record LoginResponse(String token, String tokenType, String email, String role, String name) {}
