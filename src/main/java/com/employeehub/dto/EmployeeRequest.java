package com.employeehub.dto;

import com.employeehub.entity.*;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.Past;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeRequest(
        @NotBlank @Size(max=80) String firstName,
        @NotBlank @Size(max=80) String lastName,
        @NotBlank @Email @Size(max=160) String email,
        @NotBlank @Pattern(regexp="^(?=(?:\\D*\\d){7,})[+()0-9 .-]{7,30}$") String phone,
        @Past LocalDate dateOfBirth,
        Gender gender,
        @NotNull Long departmentId,
        @NotBlank @Size(max=120) String designation,
        @NotNull @DecimalMin("0.00") BigDecimal salary,
        @NotNull @PastOrPresent LocalDate joiningDate,
        @NotNull EmploymentType employmentType,
        @NotNull EmployeeStatus status,
        @Size(max=240) String address,
        @Size(max=100) String city,
        @Size(max=100) String state,
        @Size(max=100) String country,
        @Size(min=8, max=72) String initialPassword
) {}
