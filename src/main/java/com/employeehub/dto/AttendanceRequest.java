package com.employeehub.dto;

import com.employeehub.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceRequest(@NotNull Long employeeId, @NotNull LocalDate attendanceDate,
        LocalTime checkIn, LocalTime checkOut, @NotNull AttendanceStatus status) {}
