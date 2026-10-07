package com.employeehub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(@NotBlank @Size(max=100) String name,
        @Size(max=500) String description, @Size(max=120) String managerName, Boolean active) {}
