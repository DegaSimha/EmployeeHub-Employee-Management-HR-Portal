package com.employeehub.dto;

public record DepartmentResponse(Long id, String name, String description, String managerName,
                                 boolean active, long employeeCount) {}
