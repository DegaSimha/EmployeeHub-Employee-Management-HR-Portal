package com.employeehub.service;

import com.employeehub.dto.*;
import com.employeehub.entity.*;
import com.employeehub.exception.*;
import com.employeehub.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;

@Service
public class EmployeeService {
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final UserRepository users;
    private final AttendanceRepository attendance;
    private final LeaveRepository leaves;
    private final PasswordEncoder passwordEncoder;
    public EmployeeService(EmployeeRepository employees, DepartmentRepository departments, UserRepository users,
                           AttendanceRepository attendance, LeaveRepository leaves, PasswordEncoder passwordEncoder) {
        this.employees = employees; this.departments = departments; this.users = users;
        this.attendance = attendance; this.leaves = leaves; this.passwordEncoder = passwordEncoder;
    }
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> search(String keyword, Long departmentId, EmployeeStatus status,
                                         String designation, Pageable pageable) {
        return employees.search(blankToNull(keyword), departmentId, status, blankToNull(designation), pageable).map(this::toResponse);
    }
    @Transactional(readOnly = true)
    public EmployeeResponse get(Long id) { return toResponse(requireEmployee(id)); }
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        if (request.initialPassword() == null || request.initialPassword().isBlank())
            throw new BadRequestException("Set an initial sign-in password for this employee.");
        if (employees.existsByEmailIgnoreCase(request.email())) throw new BadRequestException("An employee with this email already exists.");
        Department department = requireDepartment(request.departmentId());
        String code = nextCode();
        Employee employee = employees.save(new Employee(code, request.firstName(), request.lastName(),
                request.email().toLowerCase(), request.phone(), request.dateOfBirth(), request.gender(),
                department, request.designation(), request.salary(), request.joiningDate(),
                request.employmentType(), request.status(), request.address(), request.city(),
                request.state(), request.country()));
        users.save(new UserAccount(employee.getEmail(), passwordEncoder.encode(request.initialPassword()), Role.EMPLOYEE, employee));
        return toResponse(employee);
    }
    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = requireEmployee(id);
        if (employees.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) throw new BadRequestException("An employee with this email already exists.");
        String oldEmail = employee.getEmail();
        employee.update(request.firstName(), request.lastName(), request.email().toLowerCase(), request.phone(),
                request.dateOfBirth(), request.gender(), requireDepartment(request.departmentId()),
                request.designation(), request.salary(), request.joiningDate(), request.employmentType(),
                request.status(), request.address(), request.city(), request.state(), request.country());
        var account = users.findByEmployeeId(id);
        if (account.isEmpty()) {
            if (request.initialPassword() == null || request.initialPassword().isBlank())
                throw new BadRequestException("Set an initial sign-in password to create this employee's account.");
            users.save(new UserAccount(employee.getEmail(), passwordEncoder.encode(request.initialPassword()), Role.EMPLOYEE, employee));
        } else {
            UserAccount user = account.get();
            if (!oldEmail.equalsIgnoreCase(employee.getEmail())) user.updateEmail(employee.getEmail());
            if (request.initialPassword() != null && !request.initialPassword().isBlank())
                user.updatePassword(passwordEncoder.encode(request.initialPassword()));
        }
        return toResponse(employee);
    }
    @Transactional
    public void delete(Long id) {
        Employee employee = requireEmployee(id);
        attendance.deleteByEmployeeId(id);
        leaves.deleteByEmployeeId(id);
        users.findByEmployeeId(id).ifPresent(users::delete);
        employees.delete(employee);
    }
    @Transactional(readOnly = true)
    public List<EmployeeResponse> recent() { return employees.findTop5ByOrderByCreatedAtDesc().stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public Employee requireEmployee(Long id) { return employees.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found.")); }
    @Transactional(readOnly = true)
    public Department requireDepartment(Long id) { return departments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department not found.")); }
    public EmployeeResponse toResponse(Employee e) {
        return new EmployeeResponse(e.getId(), e.getEmployeeCode(), e.getFirstName(), e.getLastName(),
                e.getEmail(), e.getPhone(), e.getDateOfBirth(), e.getGender(), e.getDepartment().getId(),
                e.getDepartment().getName(), e.getDesignation(), e.getSalary(), e.getJoiningDate(),
                e.getEmploymentType(), e.getStatus(), e.getAddress(), e.getCity(), e.getState(), e.getCountry());
    }
    private String nextCode() {
        long next = employees.count() + 1;
        String code;
        do { code = "EMP" + String.format("%03d", next++); }
        while (employees.existsByEmployeeCode(code));
        return code;
    }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
