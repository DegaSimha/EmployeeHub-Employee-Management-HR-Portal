package com.employeehub.dto;

import java.util.List;
import java.util.Map;

public record DashboardStatistics(long totalEmployees, long activeEmployees, long inactiveEmployees,
        long departments, long pendingLeaves, List<Map<String, Object>> departmentDistribution) {}
