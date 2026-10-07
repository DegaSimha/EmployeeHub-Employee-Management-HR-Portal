package com.employeehub.dto;

import com.employeehub.entity.LeaveType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record LeaveRequestDto(@NotNull LeaveType leaveType, @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull @FutureOrPresent LocalDate endDate, @NotBlank @Size(max=1000) String reason) {}
