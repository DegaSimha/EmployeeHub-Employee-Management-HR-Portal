package com.employeehub.dto;

import java.util.List;

public record EmployeePageResponse<T>(List<T> content, int number, int size, long totalElements,
        int totalPages, boolean first, boolean last) {}
