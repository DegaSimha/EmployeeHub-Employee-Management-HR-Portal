package com.employeehub.dto;

import java.time.LocalDate;

public record HolidayResponse(Long id, String name, LocalDate holidayDate, String description, boolean paid) {}
