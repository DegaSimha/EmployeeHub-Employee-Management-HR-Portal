package com.employeehub.service;

import com.employeehub.dto.*;
import com.employeehub.entity.*;
import com.employeehub.exception.*;
import com.employeehub.repository.AttendanceRepository;
import com.employeehub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AttendanceService {
    private final AttendanceRepository attendance;
    private final EmployeeService employeeService;
    private final UserRepository users;
    private final LocalTime lateAfter;
    public AttendanceService(AttendanceRepository attendance, EmployeeService employeeService, UserRepository users,
                            @Value("${app.attendance.late-after:09:15}") LocalTime lateAfter) {
        this.attendance = attendance; this.employeeService = employeeService; this.users = users; this.lateAfter = lateAfter;
    }
    @Transactional(readOnly = true)
    public List<AttendanceResponse> all(LocalDate from, LocalDate to, Long employeeId, AttendanceStatus status) {
        if (from != null && to != null && from.isAfter(to)) throw new BadRequestException("From date must be on or before to date.");
        return attendance.findWithinDates(from, to, employeeId, status).stream().map(this::response).toList();
    }
    @Transactional(readOnly = true)
    public List<AttendanceResponse> byEmployee(Long employeeId) {
        employeeService.requireEmployee(employeeId);
        return attendance.findByEmployeeIdOrderByAttendanceDateDesc(employeeId).stream().map(this::response).toList();
    }
    @Transactional(readOnly = true)
    public List<AttendanceResponse> mine(String email) {
        UserAccount user = currentUser(email);
        if (user.getEmployee() == null) return List.of();
        return attendance.findByEmployeeIdOrderByAttendanceDateDesc(user.getEmployee().getId()).stream().map(this::response).toList();
    }
    @Transactional
    public AttendanceResponse checkIn(String email) {
        Employee employee = currentEmployee(email);
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendance.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseGet(() -> new AttendanceRecord(employee, today, null, null, AttendanceStatus.PRESENT));
        if (record.getCheckIn() != null) throw new BadRequestException("You have already checked in today.");
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        record.checkIn(now, now.isAfter(lateAfter) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);
        return response(attendance.save(record));
    }
    @Transactional
    public AttendanceResponse checkOut(String email) {
        Employee employee = currentEmployee(email);
        AttendanceRecord record = attendance.findByEmployeeIdAndAttendanceDate(employee.getId(), LocalDate.now())
                .orElseThrow(() -> new BadRequestException("Check in before checking out."));
        if (record.getCheckIn() == null) throw new BadRequestException("Check in before checking out.");
        if (record.getCheckOut() != null) throw new BadRequestException("You have already checked out today.");
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        if (now.isBefore(record.getCheckIn())) throw new BadRequestException("Check-out time cannot be earlier than check-in.");
        record.checkOut(now);
        return response(record);
    }
    @Transactional
    public AttendanceResponse create(AttendanceRequest request) {
        validateTimes(request);
        return response(attendance.save(new AttendanceRecord(employeeService.requireEmployee(request.employeeId()),
                request.attendanceDate(), request.checkIn(), request.checkOut(), request.status())));
    }
    @Transactional
    public AttendanceResponse update(Long id, AttendanceRequest request) {
        validateTimes(request);
        AttendanceRecord record = attendance.findById(id).orElseThrow(() -> new ResourceNotFoundException("Attendance record not found."));
        if (!record.getEmployee().getId().equals(request.employeeId())) throw new BadRequestException("Attendance employee cannot be changed.");
        record.update(request.attendanceDate(), request.checkIn(), request.checkOut(), request.status());
        return response(record);
    }
    @Transactional(readOnly = true)
    public AttendanceResponse get(Long id) {
        AttendanceRecord record = attendance.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found."));
        return response(record);
    }
    private void validateTimes(AttendanceRequest request) {
        if (request.checkIn() != null && request.checkOut() != null && request.checkOut().isBefore(request.checkIn()))
            throw new BadRequestException("Check-out time must be after check-in time.");
    }
    private UserAccount currentUser(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
    private Employee currentEmployee(String email) {
        Employee employee = currentUser(email).getEmployee();
        if (employee == null) throw new BadRequestException("This account is not linked to an employee profile.");
        return employee;
    }
    private AttendanceResponse response(AttendanceRecord record) {
        String hours = "—";
        if (record.getCheckIn() != null && record.getCheckOut() != null) {
            long minutes = Duration.between(record.getCheckIn(), record.getCheckOut()).toMinutes();
            hours = (minutes / 60) + "h " + (minutes % 60) + "m";
        }
        Employee e = record.getEmployee();
        return new AttendanceResponse(record.getId(), e.getId(), e.getFirstName() + " " + e.getLastName(),
                e.getEmployeeCode(), record.getAttendanceDate(), record.getCheckIn(), record.getCheckOut(),
                hours, record.getStatus());
    }
}
