package com.employeehub.dto;

import com.employeehub.entity.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveResponse(Long id, Long employeeId, String employeeName, String employeeCode,
        LeaveType leaveType, LocalDate startDate, LocalDate endDate, int days, String reason,
        LeaveStatus status, String adminComment, LocalDateTime createdAt) {}
