package com.employeehub.service;

import com.employeehub.dto.*;
import com.employeehub.entity.Department;
import com.employeehub.exception.*;
import com.employeehub.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentRepository departments;
    private final EmployeeRepository employees;
    public DepartmentService(DepartmentRepository departments, EmployeeRepository employees) {
        this.departments = departments; this.employees = employees;
    }
    @Transactional(readOnly = true)
    public List<DepartmentResponse> all() { return departments.findAll().stream().map(this::response).toList(); }
    @Transactional(readOnly = true)
    public DepartmentResponse get(Long id) { return response(requireDepartment(id)); }
    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        if (departments.existsByNameIgnoreCase(request.name().trim())) throw new BadRequestException("A department with this name already exists.");
        Department department = departments.save(new Department(request.name().trim(), request.description(), request.managerName()));
        return response(department);
    }
    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = requireDepartment(id);
        if (departments.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) throw new BadRequestException("A department with this name already exists.");
        department.update(request.name().trim(), request.description(), request.managerName(),
                request.active() == null ? department.isActive() : request.active());
        return response(department);
    }
    @Transactional
    public void delete(Long id) {
        Department department = requireDepartment(id);
        if (employees.countByDepartmentId(id) > 0) throw new BadRequestException("Move or remove this department's employees before deleting it.");
        departments.delete(department);
    }
    private Department requireDepartment(Long id) {
        return departments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department not found."));
    }
    private DepartmentResponse response(Department department) {
        return new DepartmentResponse(department.getId(), department.getName(), department.getDescription(),
                department.getManagerName(), department.isActive(), employees.countByDepartmentId(department.getId()));
    }
}
