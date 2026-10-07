package com.employeehub.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(@Pattern(regexp="^(?=(?:\\D*\\d){7,})[+()0-9 .-]{7,30}$") String phone,
        @Size(max=240) String address, @Size(max=100) String city,
        @Size(max=100) String state, @Size(max=100) String country) {}
