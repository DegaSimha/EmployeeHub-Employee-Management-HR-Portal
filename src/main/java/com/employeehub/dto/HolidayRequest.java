package com.employeehub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record HolidayRequest(@NotBlank @Size(max=120) String name,
        @NotNull LocalDate holidayDate, @Size(max=500) String description, Boolean paid) {}
