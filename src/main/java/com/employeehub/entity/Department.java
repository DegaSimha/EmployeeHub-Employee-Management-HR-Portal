package com.employeehub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "departments")
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @Column(length = 500)
    private String description;
    @Column(name = "manager_name", length = 120)
    private String managerName;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false, columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime updatedAt;

    protected Department() {}
    public Department(String name, String description, String managerName) {
        this.name = name;
        this.description = description;
        this.managerName = managerName;
    }
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getManagerName() { return managerName; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void update(String name, String description, String managerName, boolean active) {
        this.name = name; this.description = description; this.managerName = managerName; this.active = active;
    }
}
