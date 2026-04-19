package com.leavemanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Department is required")
    private String department;

    @NotBlank(message = "Role is required")
    private String role;

    @Min(value = 0, message = "Annual leave balance cannot be negative")
    @Column(name = "annual_leave_balance")
    private int annualLeaveBalance = 20;

    @Min(value = 0)
    @Column(name = "sick_leave_balance")
    private int sickLeaveBalance = 10;

    @Min(value = 0)
    @Column(name = "casual_leave_balance")
    private int casualLeaveBalance = 7;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    public enum EmployeeStatus {
        ACTIVE, INACTIVE
    }
}
