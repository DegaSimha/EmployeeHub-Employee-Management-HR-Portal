package com.employeehub.dto;

import jakarta.validation.constraints.Size;

public record LeaveDecisionRequest(@Size(max=500) String comment) {}
