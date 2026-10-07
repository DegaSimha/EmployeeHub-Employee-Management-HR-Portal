package com.employeehub.dto;

import com.employeehub.entity.AttendanceStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceResponse(Long id, Long employeeId, String employeeName, String employeeCode,
        LocalDate attendanceDate, LocalTime checkIn, LocalTime checkOut, String workingHours,
        AttendanceStatus status) {}
