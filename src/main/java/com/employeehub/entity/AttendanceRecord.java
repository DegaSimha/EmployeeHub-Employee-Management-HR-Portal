package com.employeehub.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "attendance", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "attendance_date"}))
public class AttendanceRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    @Column(name = "attendance_date", nullable = false) private LocalDate attendanceDate;
    private LocalTime checkIn;
    private LocalTime checkOut;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AttendanceStatus status;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;

    protected AttendanceRecord() {}
    public AttendanceRecord(Employee employee, LocalDate attendanceDate, LocalTime checkIn, LocalTime checkOut, AttendanceStatus status) {
        this.employee = employee; this.attendanceDate = attendanceDate; this.checkIn = checkIn;
        this.checkOut = checkOut; this.status = status;
    }
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
    public void update(LocalDate date, LocalTime in, LocalTime out, AttendanceStatus status) {
        attendanceDate = date; checkIn = in; checkOut = out; this.status = status;
    }
    public void checkIn(LocalTime time, AttendanceStatus status) {
        this.checkIn = time;
        this.status = status;
    }
    public void checkOut(LocalTime time) { this.checkOut = time; }
    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public LocalTime getCheckIn() { return checkIn; }
    public LocalTime getCheckOut() { return checkOut; }
    public AttendanceStatus getStatus() { return status; }
}
