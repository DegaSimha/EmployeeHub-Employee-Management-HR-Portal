package com.employeehub.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
public class Employee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 20)
    private String employeeCode;
    @Column(nullable = false, length = 80)
    private String firstName;
    @Column(nullable = false, length = 80)
    private String lastName;
    @Column(nullable = false, unique = true, length = 160)
    private String email;
    @Column(nullable = false, length = 30)
    private String phone;
    private LocalDate dateOfBirth;
    @Enumerated(EnumType.STRING) private Gender gender;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
    @Column(nullable = false, length = 120)
    private String designation;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salary;
    @Column(nullable = false)
    private LocalDate joiningDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EmploymentType employmentType;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EmployeeStatus status;
    @Column(length = 240) private String address;
    @Column(length = 100) private String city;
    @Column(length = 100) private String state;
    @Column(length = 100) private String country;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    protected Employee() {}
    public Employee(String employeeCode, String firstName, String lastName, String email, String phone,
                    LocalDate dateOfBirth, Gender gender, Department department, String designation,
                    BigDecimal salary, LocalDate joiningDate, EmploymentType employmentType,
                    EmployeeStatus status, String address, String city, String state, String country) {
        this.employeeCode = employeeCode; this.firstName = firstName; this.lastName = lastName;
        this.email = email; this.phone = phone; this.dateOfBirth = dateOfBirth; this.gender = gender;
        this.department = department; this.designation = designation; this.salary = salary;
        this.joiningDate = joiningDate; this.employmentType = employmentType; this.status = status;
        this.address = address; this.city = city; this.state = state; this.country = country;
    }
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public void update(String firstName, String lastName, String email, String phone, LocalDate dateOfBirth,
                       Gender gender, Department department, String designation, BigDecimal salary,
                       LocalDate joiningDate, EmploymentType employmentType, EmployeeStatus status,
                       String address, String city, String state, String country) {
        this.firstName = firstName; this.lastName = lastName; this.email = email; this.phone = phone;
        this.dateOfBirth = dateOfBirth; this.gender = gender; this.department = department;
        this.designation = designation; this.salary = salary; this.joiningDate = joiningDate;
        this.employmentType = employmentType; this.status = status; this.address = address;
        this.city = city; this.state = state; this.country = country;
    }
    public Long getId() { return id; }
    public String getEmployeeCode() { return employeeCode; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public Gender getGender() { return gender; }
    public Department getDepartment() { return department; }
    public String getDesignation() { return designation; }
    public BigDecimal getSalary() { return salary; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public EmploymentType getEmploymentType() { return employmentType; }
    public EmployeeStatus getStatus() { return status; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getCountry() { return country; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
