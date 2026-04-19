package com.leavemanagement.service;

import com.leavemanagement.exception.LeaveBusinessException;
import com.leavemanagement.exception.ResourceNotFoundException;
import com.leavemanagement.model.Employee;
import com.leavemanagement.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public Employee createEmployee(Employee employee) {
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new LeaveBusinessException("Employee with email '" + employee.getEmail() + "' already exists.");
        }
        // Set default leave balances
        employee.setAnnualLeaveBalance(20);
        employee.setSickLeaveBalance(10);
        employee.setCasualLeaveBalance(7);
        employee.setStatus(Employee.EmployeeStatus.ACTIVE);
        return employeeRepository.save(employee);
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
    }

    public Employee updateEmployee(Long id, Employee updatedEmployee) {
        Employee existing = getEmployeeById(id);

        // Check email uniqueness if changed
        if (!existing.getEmail().equals(updatedEmployee.getEmail())
                && employeeRepository.existsByEmail(updatedEmployee.getEmail())) {
            throw new LeaveBusinessException("Email '" + updatedEmployee.getEmail() + "' is already in use.");
        }

        existing.setName(updatedEmployee.getName());
        existing.setEmail(updatedEmployee.getEmail());
        existing.setDepartment(updatedEmployee.getDepartment());
        existing.setRole(updatedEmployee.getRole());
        return employeeRepository.save(existing);
    }

    public void deactivateEmployee(Long id) {
        Employee employee = getEmployeeById(id);
        employee.setStatus(Employee.EmployeeStatus.INACTIVE);
        employeeRepository.save(employee);
    }

    public List<Employee> getEmployeesByDepartment(String department) {
        return employeeRepository.findByDepartment(department);
    }
}
