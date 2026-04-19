package com.leavemanagement.service;

import com.leavemanagement.exception.LeaveBusinessException;
import com.leavemanagement.exception.ResourceNotFoundException;
import com.leavemanagement.model.Employee;
import com.leavemanagement.model.LeaveRequest;
import com.leavemanagement.model.LeaveRequest.LeaveStatus;
import com.leavemanagement.model.LeaveRequest.LeaveType;
import com.leavemanagement.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeaveRequestService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private EmployeeService employeeService;

    @Transactional
    public LeaveRequest applyLeave(Long employeeId, LeaveRequest request) {
        Employee employee = employeeService.getEmployeeById(employeeId);

        // Rule 1: Employee must be active
        if (employee.getStatus() == Employee.EmployeeStatus.INACTIVE) {
            throw new LeaveBusinessException("Inactive employees cannot apply for leave.");
        }

        // Rule 2: Start date must not be in the past
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new LeaveBusinessException("Start date cannot be in the past.");
        }

        // Rule 3: End date must be after or equal to start date
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new LeaveBusinessException("End date must be on or after the start date.");
        }

        // Rule 4: Check for overlapping approved/pending leaves
        List<LeaveRequest> overlaps = leaveRequestRepository.findOverlappingLeaves(
                employeeId, request.getStartDate(), request.getEndDate());
        if (!overlaps.isEmpty()) {
            throw new LeaveBusinessException("You already have a leave request overlapping these dates.");
        }

        // Rule 5: Check leave balance
        long requestedDays = request.getLeaveDays();
        validateLeaveBalance(employee, request.getLeaveType(), requestedDays);

        request.setEmployee(employee);
        request.setStatus(LeaveStatus.PENDING);
        request.setAppliedOn(LocalDateTime.now());

        return leaveRequestRepository.save(request);
    }

    @Transactional
    public LeaveRequest approveLeave(Long requestId, String reviewerComment) {
        LeaveRequest leaveRequest = getLeaveRequestById(requestId);

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new LeaveBusinessException("Only PENDING requests can be approved. Current status: " + leaveRequest.getStatus());
        }

        // Deduct leave balance
        Employee employee = leaveRequest.getEmployee();
        long days = leaveRequest.getLeaveDays();
        deductLeaveBalance(employee, leaveRequest.getLeaveType(), days);

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setReviewedOn(LocalDateTime.now());
        leaveRequest.setReviewerComment(reviewerComment);

        return leaveRequestRepository.save(leaveRequest);
    }

    @Transactional
    public LeaveRequest rejectLeave(Long requestId, String reviewerComment) {
        LeaveRequest leaveRequest = getLeaveRequestById(requestId);

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new LeaveBusinessException("Only PENDING requests can be rejected. Current status: " + leaveRequest.getStatus());
        }

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setReviewedOn(LocalDateTime.now());
        leaveRequest.setReviewerComment(reviewerComment);

        return leaveRequestRepository.save(leaveRequest);
    }

    @Transactional
    public LeaveRequest cancelLeave(Long requestId, Long employeeId) {
        LeaveRequest leaveRequest = getLeaveRequestById(requestId);

        if (!leaveRequest.getEmployee().getId().equals(employeeId)) {
            throw new LeaveBusinessException("You can only cancel your own leave requests.");
        }

        if (leaveRequest.getStatus() == LeaveStatus.CANCELLED) {
            throw new LeaveBusinessException("This leave request is already cancelled.");
        }

        if (leaveRequest.getStatus() == LeaveStatus.REJECTED) {
            throw new LeaveBusinessException("Rejected leave requests cannot be cancelled.");
        }

        // Restore balance if it was approved
        if (leaveRequest.getStatus() == LeaveStatus.APPROVED) {
            restoreLeaveBalance(leaveRequest.getEmployee(), leaveRequest.getLeaveType(), leaveRequest.getLeaveDays());
        }

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        leaveRequest.setReviewedOn(LocalDateTime.now());

        return leaveRequestRepository.save(leaveRequest);
    }

    public List<LeaveRequest> getLeavesByEmployee(Long employeeId) {
        employeeService.getEmployeeById(employeeId); // validate exists
        return leaveRequestRepository.findByEmployeeId(employeeId);
    }

    public List<LeaveRequest> getLeavesByStatus(LeaveStatus status) {
        return leaveRequestRepository.findByStatus(status);
    }

    public LeaveRequest getLeaveRequestById(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + id));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void validateLeaveBalance(Employee employee, LeaveType type, long requestedDays) {
        int balance = getBalance(employee, type);
        if (requestedDays > balance) {
            throw new LeaveBusinessException(
                    "Insufficient " + type + " leave balance. Available: " + balance + " days, Requested: " + requestedDays + " days.");
        }
    }

    private void deductLeaveBalance(Employee employee, LeaveType type, long days) {
        switch (type) {
            case ANNUAL  -> employee.setAnnualLeaveBalance(employee.getAnnualLeaveBalance() - (int) days);
            case SICK    -> employee.setSickLeaveBalance(employee.getSickLeaveBalance() - (int) days);
            case CASUAL  -> employee.setCasualLeaveBalance(employee.getCasualLeaveBalance() - (int) days);
        }
    }

    private void restoreLeaveBalance(Employee employee, LeaveType type, long days) {
        switch (type) {
            case ANNUAL  -> employee.setAnnualLeaveBalance(employee.getAnnualLeaveBalance() + (int) days);
            case SICK    -> employee.setSickLeaveBalance(employee.getSickLeaveBalance() + (int) days);
            case CASUAL  -> employee.setCasualLeaveBalance(employee.getCasualLeaveBalance() + (int) days);
        }
    }

    private int getBalance(Employee employee, LeaveType type) {
        return switch (type) {
            case ANNUAL  -> employee.getAnnualLeaveBalance();
            case SICK    -> employee.getSickLeaveBalance();
            case CASUAL  -> employee.getCasualLeaveBalance();
        };
    }
}
