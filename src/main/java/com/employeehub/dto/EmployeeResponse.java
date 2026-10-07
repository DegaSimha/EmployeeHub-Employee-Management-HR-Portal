package com.employeehub.dto;

import com.employeehub.entity.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeResponse(Long id, String employeeCode, String firstName, String lastName,
        String email, String phone, LocalDate dateOfBirth, Gender gender, Long departmentId,
        String departmentName, String designation, BigDecimal salary, LocalDate joiningDate,
        EmploymentType employmentType, EmployeeStatus status, String address, String city,
        String state, String country) {}
