package com.employeehub.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_holidays")
public class CompanyHoliday {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 120)
    private String name;
    @Column(nullable = false)
    private LocalDate holidayDate;
    @Column(length = 500)
    private String description;
    @Column(nullable = false)
    private boolean paid = true;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected CompanyHoliday() {}
    public CompanyHoliday(String name, LocalDate holidayDate, String description, boolean paid) {
        this.name = name; this.holidayDate = holidayDate; this.description = description; this.paid = paid;
    }
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
    public void update(String name, LocalDate holidayDate, String description, boolean paid) {
        this.name = name; this.holidayDate = holidayDate; this.description = description; this.paid = paid;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDate getHolidayDate() { return holidayDate; }
    public String getDescription() { return description; }
    public boolean isPaid() { return paid; }
}
