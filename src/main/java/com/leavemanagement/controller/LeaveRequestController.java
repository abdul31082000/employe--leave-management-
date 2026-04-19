package com.leavemanagement.controller;

import com.leavemanagement.model.LeaveRequest;
import com.leavemanagement.model.LeaveRequest.LeaveStatus;
import com.leavemanagement.service.LeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
public class LeaveRequestController {

    @Autowired
    private LeaveRequestService leaveRequestService;

    // POST /api/leaves/apply/{employeeId} — Apply for leave
    @PostMapping("/apply/{employeeId}")
    public ResponseEntity<LeaveRequest> applyLeave(@PathVariable Long employeeId,
                                                    @Valid @RequestBody LeaveRequest request) {
        return new ResponseEntity<>(leaveRequestService.applyLeave(employeeId, request), HttpStatus.CREATED);
    }

    // GET /api/leaves/employee/{employeeId} — Get all leaves for employee
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveRequest>> getLeavesByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveRequestService.getLeavesByEmployee(employeeId));
    }

    // GET /api/leaves/{id} — Get single leave request
    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequest> getLeaveById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestById(id));
    }

    // GET /api/leaves?status=PENDING — Get leaves by status
    @GetMapping
    public ResponseEntity<List<LeaveRequest>> getLeavesByStatus(
            @RequestParam(defaultValue = "PENDING") LeaveStatus status) {
        return ResponseEntity.ok(leaveRequestService.getLeavesByStatus(status));
    }

    // PATCH /api/leaves/{id}/approve — Approve a leave
    @PatchMapping("/{id}/approve")
    public ResponseEntity<LeaveRequest> approveLeave(@PathVariable Long id,
                                                      @RequestBody Map<String, String> body) {
        String comment = body.getOrDefault("comment", "Approved");
        return ResponseEntity.ok(leaveRequestService.approveLeave(id, comment));
    }

    // PATCH /api/leaves/{id}/reject — Reject a leave
    @PatchMapping("/{id}/reject")
    public ResponseEntity<LeaveRequest> rejectLeave(@PathVariable Long id,
                                                     @RequestBody Map<String, String> body) {
        String comment = body.getOrDefault("comment", "Rejected by manager");
        return ResponseEntity.ok(leaveRequestService.rejectLeave(id, comment));
    }

    // PATCH /api/leaves/{id}/cancel/{employeeId} — Cancel a leave
    @PatchMapping("/{id}/cancel/{employeeId}")
    public ResponseEntity<LeaveRequest> cancelLeave(@PathVariable Long id,
                                                     @PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveRequestService.cancelLeave(id, employeeId));
    }
}
