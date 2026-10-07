package com.employeehub.controller;

import com.employeehub.entity.AttendanceStatus;
import com.employeehub.entity.LeaveStatus;
import com.employeehub.repository.*;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final AttendanceRepository attendance;
    private final LeaveRepository leaves;
    public ReportsController(EmployeeRepository employees, DepartmentRepository departments,
            AttendanceRepository attendance, LeaveRepository leaves) {
        this.employees = employees; this.departments = departments; this.attendance = attendance; this.leaves = leaves;
    }
    @GetMapping("/employees") public Map<String, Long> employeeReport() {
        Map<String, Long> report = new LinkedHashMap<>();
        report.put("total", employees.count());
        report.put("active", employees.countByStatus(com.employeehub.entity.EmployeeStatus.ACTIVE));
        report.put("inactive", employees.countByStatus(com.employeehub.entity.EmployeeStatus.INACTIVE));
        return report;
    }
    @GetMapping("/departments") public Object departmentReport() {
        return departments.findAll().stream().map(d -> Map.of("department", d.getName(),
                "count", employees.countByDepartmentId(d.getId()))).toList();
    }
    @GetMapping("/attendance") public Map<String, Long> attendanceReport() {
        Map<String, Long> report = new LinkedHashMap<>();
        for (AttendanceStatus status : AttendanceStatus.values()) report.put(status.name().toLowerCase(), attendance.countByStatus(status));
        return report;
    }
    @GetMapping("/leaves") public Map<String, Long> leaveReport() {
        Map<String, Long> report = new LinkedHashMap<>();
        for (LeaveStatus status : LeaveStatus.values()) report.put(status.name().toLowerCase(), leaves.countByStatus(status));
        return report;
    }
}
