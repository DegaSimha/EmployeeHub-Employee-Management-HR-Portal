package com.employeehub.controller;

import com.employeehub.dto.*;
import com.employeehub.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboard;
    private final EmployeeService employees;
    private final LeaveService leaves;
    public DashboardController(DashboardService dashboard, EmployeeService employees, LeaveService leaves) {
        this.dashboard = dashboard; this.employees = employees; this.leaves = leaves;
    }
    @GetMapping("/statistics") public DashboardStatistics statistics() { return dashboard.statistics(); }
    @GetMapping("/recent-employees") public List<EmployeeResponse> recentEmployees() { return employees.recent(); }
    @GetMapping("/recent-leaves") public List<LeaveResponse> recentLeaves() { return leaves.recent(); }
}
