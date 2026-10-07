package com.employeehub.service;

import com.employeehub.dto.*;
import com.employeehub.entity.*;
import com.employeehub.exception.*;
import com.employeehub.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class LeaveService {
    private final LeaveRepository leaves;
    private final UserRepository users;
    private final EmployeeService employeeService;
    public LeaveService(LeaveRepository leaves, UserRepository users, EmployeeService employeeService) {
        this.leaves = leaves; this.users = users; this.employeeService = employeeService;
    }
    @Transactional(readOnly = true)
    public List<LeaveResponse> all() { return leaves.findAllByOrderByCreatedAtDesc().stream().map(this::response).toList(); }
    @Transactional(readOnly = true)
    public List<LeaveResponse> mine(String email) {
        Employee employee = currentEmployee(email);
        return leaves.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()).stream().map(this::response).toList();
    }
    @Transactional
    public LeaveResponse apply(String email, LeaveRequestDto request) {
        if (request.endDate().isBefore(request.startDate())) throw new BadRequestException("End date must be on or after start date.");
        Employee employee = currentEmployee(email);
        if (leaves.hasOverlappingActiveRequest(employee.getId(), request.startDate(), request.endDate()))
            throw new BadRequestException("This request overlaps another pending or approved leave request.");
        return response(leaves.save(new LeaveRequest(employee, request.leaveType(),
                request.startDate(), request.endDate(), request.reason().trim())));
    }
    @Transactional
    public LeaveResponse decide(Long id, LeaveStatus status, String comment) {
        LeaveRequest leave = leaves.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave request not found."));
        if (leave.getStatus() != LeaveStatus.PENDING) throw new BadRequestException("Only pending requests can be approved or rejected.");
        if (status == LeaveStatus.REJECTED && (comment == null || comment.isBlank()))
            throw new BadRequestException("Please provide a reason when rejecting a leave request.");
        leave.decide(status, comment == null || comment.isBlank() ? null : comment.trim());
        return response(leave);
    }
    @Transactional
    public void withdraw(String email, Long id) {
        Employee employee = currentEmployee(email);
        LeaveRequest leave = leaves.findByIdAndEmployeeId(id, employee.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found."));
        if (leave.getStatus() != LeaveStatus.PENDING)
            throw new BadRequestException("Only pending requests can be withdrawn.");
        leaves.delete(leave);
    }
    @Transactional(readOnly = true)
    public LeaveResponse get(Long id) {
        return response(leaves.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave request not found.")));
    }
    @Transactional(readOnly = true)
    public List<LeaveResponse> recent() { return leaves.findTop5ByOrderByCreatedAtDesc().stream().map(this::response).toList(); }
    private Employee currentEmployee(String email) {
        UserAccount user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (user.getEmployee() == null) throw new BadRequestException("This account is not linked to an employee profile.");
        return user.getEmployee();
    }
    private LeaveResponse response(LeaveRequest leave) {
        Employee employee = leave.getEmployee();
        return new LeaveResponse(leave.getId(), employee.getId(), employee.getFirstName() + " " + employee.getLastName(),
                employee.getEmployeeCode(), leave.getLeaveType(), leave.getStartDate(), leave.getEndDate(),
                leave.getDays(), leave.getReason(), leave.getStatus(), leave.getAdminComment(), leave.getCreatedAt());
    }
}
