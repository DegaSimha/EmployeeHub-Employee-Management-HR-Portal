package com.employeehub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 160)
    private String email;
    @Column(nullable = false)
    private String password;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Role role;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", unique = true)
    private Employee employee;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false, columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)") private LocalDateTime updatedAt;

    protected UserAccount() {}
    public UserAccount(String email, String password, Role role, Employee employee) {
        this.email = email; this.password = password; this.role = role; this.employee = employee;
    }
    public void updateEmail(String email) { this.email = email; }
    public void updatePassword(String password) { this.password = password; }
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public Employee getEmployee() { return employee; }
}
