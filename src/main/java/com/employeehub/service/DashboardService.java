package com.employeehub.service;

import com.employeehub.dto.*;
import com.employeehub.entity.EmployeeStatus;
import com.employeehub.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class DashboardService {
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final LeaveRepository leaves;
    public DashboardService(EmployeeRepository employees, DepartmentRepository departments, LeaveRepository leaves) {
        this.employees = employees; this.departments = departments; this.leaves = leaves;
    }
    @Transactional(readOnly = true)
    public DashboardStatistics statistics() {
        List<Map<String, Object>> distribution = departments.findAll().stream().map(department -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("department", department.getName());
            item.put("count", employees.countByDepartmentId(department.getId()));
            return item;
        }).toList();
        return new DashboardStatistics(employees.count(), employees.countByStatus(EmployeeStatus.ACTIVE),
                employees.countByStatus(EmployeeStatus.INACTIVE), departments.count(),
                leaves.countByStatus(com.employeehub.entity.LeaveStatus.PENDING), distribution);
    }
}
