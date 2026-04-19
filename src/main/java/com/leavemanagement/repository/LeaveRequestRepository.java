package com.leavemanagement.repository;

import com.leavemanagement.model.LeaveRequest;
import com.leavemanagement.model.LeaveRequest.LeaveStatus;
import com.leavemanagement.model.LeaveRequest.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeId(Long employeeId);

    List<LeaveRequest> findByStatus(LeaveStatus status);

    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    // Check for overlapping leave requests for the same employee
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.id = :employeeId " +
           "AND lr.status != 'REJECTED' AND lr.status != 'CANCELLED' " +
           "AND (lr.startDate <= :endDate AND lr.endDate >= :startDate)")
    List<LeaveRequest> findOverlappingLeaves(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Count approved leaves by type for an employee in a year
    @Query("SELECT COALESCE(SUM(DATEDIFF(lr.endDate, lr.startDate) + 1), 0) FROM LeaveRequest lr " +
           "WHERE lr.employee.id = :employeeId AND lr.leaveType = :leaveType " +
           "AND lr.status = 'APPROVED' AND YEAR(lr.startDate) = :year")
    Long countApprovedLeaveDaysByType(
            @Param("employeeId") Long employeeId,
            @Param("leaveType") LeaveType leaveType,
            @Param("year") int year);
}
