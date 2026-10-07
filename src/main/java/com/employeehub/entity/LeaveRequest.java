package com.employeehub.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leaves")
public class LeaveRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private LeaveType leaveType;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    @Column(nullable = false) private int days;
    @Column(nullable = false, length = 1000) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private LeaveStatus status = LeaveStatus.PENDING;
    @Column(length = 500) private String adminComment;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false, columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)") private LocalDateTime updatedAt;

    protected LeaveRequest() {}
    public LeaveRequest(Employee employee, LeaveType leaveType, LocalDate startDate, LocalDate endDate, String reason) {
        this.employee = employee; this.leaveType = leaveType; this.startDate = startDate; this.endDate = endDate;
        this.days = (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1; this.reason = reason;
    }
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public void decide(LeaveStatus status, String comment) { this.status = status; this.adminComment = comment; }
    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public LeaveType getLeaveType() { return leaveType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getDays() { return days; }
    public String getReason() { return reason; }
    public LeaveStatus getStatus() { return status; }
    public String getAdminComment() { return adminComment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
