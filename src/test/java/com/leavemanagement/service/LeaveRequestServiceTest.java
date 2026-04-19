package com.leavemanagement.service;

import com.leavemanagement.exception.LeaveBusinessException;
import com.leavemanagement.exception.ResourceNotFoundException;
import com.leavemanagement.model.Employee;
import com.leavemanagement.model.LeaveRequest;
import com.leavemanagement.model.LeaveRequest.LeaveStatus;
import com.leavemanagement.model.LeaveRequest.LeaveType;
import com.leavemanagement.repository.LeaveRequestRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveRequestService Unit Tests")
class LeaveRequestServiceTest {

    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private EmployeeService employeeService;

    @InjectMocks private LeaveRequestService leaveRequestService;

    private Employee activeEmployee;
    private Employee inactiveEmployee;

    @BeforeEach
    void setUp() {
        activeEmployee = Employee.builder()
                .id(1L).name("Abdul Basith").email("basith@test.com")
                .department("IT").role("QA Engineer")
                .annualLeaveBalance(20).sickLeaveBalance(10).casualLeaveBalance(7)
                .status(Employee.EmployeeStatus.ACTIVE)
                .build();

        inactiveEmployee = Employee.builder()
                .id(2L).name("Jane Doe").email("jane@test.com")
                .department("IT").role("Developer")
                .annualLeaveBalance(20).sickLeaveBalance(10).casualLeaveBalance(7)
                .status(Employee.EmployeeStatus.INACTIVE)
                .build();
    }

    // ── TC-001: Apply leave successfully ───────────────────────────────────
    @Test
    @DisplayName("TC-001: Should apply annual leave successfully when balance is sufficient")
    void applyLeave_Success() {
        LeaveRequest request = LeaveRequest.builder()
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .reason("Family vacation trip planned")
                .build();

        when(employeeService.getEmployeeById(1L)).thenReturn(activeEmployee);
        when(leaveRequestRepository.findOverlappingLeaves(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveRequestService.applyLeave(1L, request);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(result.getEmployee()).isEqualTo(activeEmployee);
        verify(leaveRequestRepository).save(request);
    }

    // ── TC-002: Inactive employee cannot apply ─────────────────────────────
    @Test
    @DisplayName("TC-002: Should throw exception when inactive employee applies for leave")
    void applyLeave_InactiveEmployee_ThrowsException() {
        LeaveRequest request = LeaveRequest.builder()
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .reason("Testing inactive block rule")
                .build();

        when(employeeService.getEmployeeById(2L)).thenReturn(inactiveEmployee);

        assertThatThrownBy(() -> leaveRequestService.applyLeave(2L, request))
                .isInstanceOf(LeaveBusinessException.class)
                .hasMessageContaining("Inactive employees cannot apply for leave");
    }

    // ── TC-003: Start date in the past ─────────────────────────────────────
    @Test
    @DisplayName("TC-003: Should reject leave with start date in the past")
    void applyLeave_PastStartDate_ThrowsException() {
        LeaveRequest request = LeaveRequest.builder()
                .leaveType(LeaveType.SICK)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .reason("Backdated leave request test")
                .build();

        when(employeeService.getEmployeeById(1L)).thenReturn(activeEmployee);

        assertThatThrownBy(() -> leaveRequestService.applyLeave(1L, request))
                .isInstanceOf(LeaveBusinessException.class)
                .hasMessageContaining("Start date cannot be in the past");
    }

    // ── TC-004: End date before start date ────────────────────────────────
    @Test
    @DisplayName("TC-004: Should reject leave when end date is before start date")
    void applyLeave_EndDateBeforeStartDate_ThrowsException() {
        LeaveRequest request = LeaveRequest.builder()
                .leaveType(LeaveType.CASUAL)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(2))
                .reason("Invalid date range test case")
                .build();

        when(employeeService.getEmployeeById(1L)).thenReturn(activeEmployee);

        assertThatThrownBy(() -> leaveRequestService.applyLeave(1L, request))
                .isInstanceOf(LeaveBusinessException.class)
                .hasMessageContaining("End date must be on or after");
    }

    // ── TC-005: Insufficient leave balance ────────────────────────────────
    @Test
    @DisplayName("TC-005: Should reject leave when employee has insufficient balance")
    void applyLeave_InsufficientBalance_ThrowsException() {
        activeEmployee.setCasualLeaveBalance(1); // Only 1 day left

        LeaveRequest request = LeaveRequest.builder()
                .leaveType(LeaveType.CASUAL)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5)) // 5 days requested
                .reason("Requesting more days than available balance")
                .build();

        when(employeeService.getEmployeeById(1L)).thenReturn(activeEmployee);
        when(leaveRequestRepository.findOverlappingLeaves(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> leaveRequestService.applyLeave(1L, request))
                .isInstanceOf(LeaveBusinessException.class)
                .hasMessageContaining("Insufficient CASUAL leave balance");
    }

    // ── TC-006: Overlapping leave rejected ───────────────────────────────
    @Test
    @DisplayName("TC-006: Should reject leave when dates overlap with existing request")
    void applyLeave_OverlappingDates_ThrowsException() {
        LeaveRequest existing = LeaveRequest.builder()
                .id(99L).status(LeaveStatus.APPROVED)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(4))
                .build();

        LeaveRequest newRequest = LeaveRequest.builder()
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(6))
                .reason("Overlapping leave application test")
                .build();

        when(employeeService.getEmployeeById(1L)).thenReturn(activeEmployee);
        when(leaveRequestRepository.findOverlappingLeaves(any(), any(), any()))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> leaveRequestService.applyLeave(1L, newRequest))
                .isInstanceOf(LeaveBusinessException.class)
                .hasMessageContaining("overlapping these dates");
    }

    // ── TC-007: Approve pending leave ────────────────────────────────────
    @Test
    @DisplayName("TC-007: Should approve PENDING leave and deduct balance")
    void approveLeave_Success() {
        LeaveRequest pending = LeaveRequest.builder()
                .id(1L).employee(activeEmployee)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(LeaveStatus.PENDING)
                .reason("Approved leave test scenario")
                .build();

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveRequestService.approveLeave(1L, "Approved by manager");

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(result.getReviewerComment()).isEqualTo("Approved by manager");
        assertThat(activeEmployee.getAnnualLeaveBalance()).isEqualTo(17); // 20 - 3
    }

    // ── TC-008: Cannot approve non-PENDING leave ──────────────────────────
    @Test
    @DisplayName("TC-008: Should throw exception when approving already approved leave")
    void approveLeave_AlreadyApproved_ThrowsException() {
        LeaveRequest approved = LeaveRequest.builder()
                .id(1L).status(LeaveStatus.APPROVED)
                .build();

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> leaveRequestService.approveLeave(1L, "comment"))
                .isInstanceOf(LeaveBusinessException.class)
                .hasMessageContaining("Only PENDING requests can be approved");
    }

    // ── TC-009: Reject leave ──────────────────────────────────────────────
    @Test
    @DisplayName("TC-009: Should reject PENDING leave with reason")
    void rejectLeave_Success() {
        LeaveRequest pending = LeaveRequest.builder()
                .id(1L).status(LeaveStatus.PENDING)
                .build();

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveRequestService.rejectLeave(1L, "Insufficient team coverage");

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.REJECTED);
    }

    // ── TC-010: Cancel approved leave restores balance ───────────────────
    @Test
    @DisplayName("TC-010: Should cancel APPROVED leave and restore leave balance")
    void cancelLeave_ApprovedLeave_RestoresBalance() {
        activeEmployee.setAnnualLeaveBalance(17); // Already deducted 3 days

        LeaveRequest approved = LeaveRequest.builder()
                .id(1L).employee(activeEmployee)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(LeaveStatus.APPROVED)
                .build();

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(approved));
        when(leaveRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveRequestService.cancelLeave(1L, 1L);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
        assertThat(activeEmployee.getAnnualLeaveBalance()).isEqualTo(20); // Restored
    }

    // ── TC-011: Leave not found ───────────────────────────────────────────
    @Test
    @DisplayName("TC-011: Should throw ResourceNotFoundException for invalid leave ID")
    void getLeaveById_NotFound_ThrowsException() {
        when(leaveRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveRequestService.getLeaveRequestById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Leave request not found with ID: 999");
    }

    // ── TC-012: Cannot cancel another employee's leave ───────────────────
    @Test
    @DisplayName("TC-012: Should throw exception when cancelling another employee's leave")
    void cancelLeave_WrongEmployee_ThrowsException() {
        LeaveRequest pending = LeaveRequest.builder()
                .id(1L).employee(activeEmployee) // belongs to employee ID 1
                .status(LeaveStatus.PENDING)
                .build();

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> leaveRequestService.cancelLeave(1L, 99L)) // wrong employee
                .isInstanceOf(LeaveBusinessException.class)
                .hasMessageContaining("You can only cancel your own leave requests");
    }
}
