package com.employeehub.dto;

import com.employeehub.entity.Role;
import java.time.LocalDate;

public record ProfileResponse(Long employeeId, String employeeCode, String firstName, String lastName,
        String email, String phone, String departmentName, String designation, LocalDate joiningDate,
        String address, String city, String state, String country, Role role) {}
