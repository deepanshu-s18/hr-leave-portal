package com.deepanshu.hrportal.service;

import com.deepanshu.hrportal.dto.request.LeaveRequest;
import com.deepanshu.hrportal.dto.response.LeaveResponse;
import com.deepanshu.hrportal.exception.ConflictException;
import com.deepanshu.hrportal.exception.ForbiddenException;
import com.deepanshu.hrportal.exception.ResourceNotFoundException;
import com.deepanshu.hrportal.exception.ValidationException;
import com.deepanshu.hrportal.model.Employee;
import com.deepanshu.hrportal.model.Employee.Role;
import com.deepanshu.hrportal.repository.EmployeeRepository;
import com.deepanshu.hrportal.repository.LeaveRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LeaveService} — covers all guard clauses and business logic:
 *
 * apply()   → past-date guard, date-order guard, overlap guard, balance guard
 * approve() → PENDING-only guard, manager authorization, balance deduction
 * reject()  → PENDING-only guard, required comment, auth guard
 * cancel()  → ownership guard, status guard, balance restoration
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LeaveService Unit Tests")
class LeaveServiceTest {

    @Mock private LeaveRepository leaveRepository;
    @Mock private EmployeeRepository employeeRepository;
    @InjectMocks private LeaveService leaveService;

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private Employee employee;
    private Employee manager;
    private Employee adminUser;
    private com.deepanshu.hrportal.model.LeaveRequest pendingLeave;

    @BeforeEach
    void setUp() {
        manager = Employee.builder()
            .id(1L).username("engmanager").fullName("Eng Manager")
            .role(Role.MANAGER).annualLeaveBalance(21).sickLeaveBalance(10).isActive(true)
            .build();

        employee = Employee.builder()
            .id(2L).username("deepanshu").fullName("Deepanshu Singh")
            .role(Role.EMPLOYEE).annualLeaveBalance(15).sickLeaveBalance(8)
            .manager(manager).isActive(true)
            .build();

        adminUser = Employee.builder()
            .id(3L).username("admin").fullName("Admin User")
            .role(Role.ADMIN).annualLeaveBalance(21).sickLeaveBalance(10).isActive(true)
            .build();

        pendingLeave = com.deepanshu.hrportal.model.LeaveRequest.builder()
            .id(10L)
            .employee(employee)
            .leaveType(com.deepanshu.hrportal.model.LeaveRequest.LeaveType.ANNUAL)
            .startDate(LocalDate.now().plusDays(3))
            .endDate(LocalDate.now().plusDays(5))   // 3 working days
            .reason("Family vacation")
            .status(com.deepanshu.hrportal.model.LeaveRequest.Status.PENDING)
            .build();

        // Mock Spring Security context to return "deepanshu" as current user
        mockSecurityContext("deepanshu");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPLY Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("apply() — Leave Application")
    class ApplyTests {

        @Test
        @DisplayName("✅ Valid request → creates PENDING leave")
        void apply_validRequest_createsPendingLeave() {
            LeaveRequest.Create req = LeaveRequest.Create.builder()
                .leaveType(com.deepanshu.hrportal.model.LeaveRequest.LeaveType.ANNUAL)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(7))
                .reason("Family function at home")
                .build();

            when(employeeRepository.findActiveByUsername("deepanshu")).thenReturn(Optional.of(employee));
            when(leaveRepository.countOverlapping(eq(2L), any(), any())).thenReturn(0L);
            when(leaveRepository.save(any())).thenAnswer(inv -> {
                var l = inv.<com.deepanshu.hrportal.model.LeaveRequest>getArgument(0);
                l = com.deepanshu.hrportal.model.LeaveRequest.builder()
                    .id(99L).employee(l.getEmployee()).leaveType(l.getLeaveType())
                    .startDate(l.getStartDate()).endDate(l.getEndDate())
                    .reason(l.getReason())
                    .status(com.deepanshu.hrportal.model.LeaveRequest.Status.PENDING)
                    .build();
                return l;
            });

            LeaveResponse.LeaveDetail result = leaveService.apply(req);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(com.deepanshu.hrportal.model.LeaveRequest.Status.PENDING);
            assertThat(result.getLeaveType()).isEqualTo(com.deepanshu.hrportal.model.LeaveRequest.LeaveType.ANNUAL);
            verify(leaveRepository).save(any());
        }

        @Test
        @DisplayName("❌ Guard: past start date → throws ValidationException")
        void apply_pastStartDate_throwsValidationException() {
            LeaveRequest.Create req = LeaveRequest.Create.builder()
                .leaveType(com.deepanshu.hrportal.model.LeaveRequest.LeaveType.ANNUAL)
                .startDate(LocalDate.now().minusDays(1))   // yesterday
                .endDate(LocalDate.now().plusDays(2))
                .reason("Retroactive leave request")
                .build();

            when(employeeRepository.findActiveByUsername("deepanshu")).thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> leaveService.apply(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("past");

            verify(leaveRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ Guard: end date before start date → throws ValidationException")
        void apply_endBeforeStart_throwsValidationException() {
            LeaveRequest.Create req = LeaveRequest.Create.builder()
                .leaveType(com.deepanshu.hrportal.model.LeaveRequest.LeaveType.ANNUAL)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(2))   // end before start
                .reason("Invalid date range")
                .build();

            when(employeeRepository.findActiveByUsername("deepanshu")).thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> leaveService.apply(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("End date");

            verify(leaveRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ Guard: overlapping leave exists → throws ConflictException")
        void apply_overlappingLeave_throwsConflictException() {
            LeaveRequest.Create req = LeaveRequest.Create.builder()
                .leaveType(com.deepanshu.hrportal.model.LeaveRequest.LeaveType.ANNUAL)
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(6))
                .reason("Overlapping with existing leave")
                .build();

            when(employeeRepository.findActiveByUsername("deepanshu")).thenReturn(Optional.of(employee));
            when(leaveRepository.countOverlapping(eq(2L), any(), any())).thenReturn(1L); // overlap!

            assertThatThrownBy(() -> leaveService.apply(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlapping");

            verify(leaveRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ Guard: insufficient annual leave balance → throws ValidationException")
        void apply_insufficientBalance_throwsValidationException() {
            employee.setAnnualLeaveBalance(2); // only 2 days left

            LeaveRequest.Create req = LeaveRequest.Create.builder()
                .leaveType(com.deepanshu.hrportal.model.LeaveRequest.LeaveType.ANNUAL)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5)) // 5 working days > 2 available
                .reason("Long leave with low balance")
                .build();

            when(employeeRepository.findActiveByUsername("deepanshu")).thenReturn(Optional.of(employee));
            when(leaveRepository.countOverlapping(eq(2L), any(), any())).thenReturn(0L);

            assertThatThrownBy(() -> leaveService.apply(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Insufficient");

            verify(leaveRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ SICK leave uses sick balance — not annual balance")
        void apply_sickLeave_usesSickBalance() {
            employee.setSickLeaveBalance(5);
            employee.setAnnualLeaveBalance(0); // annual empty — should NOT matter for SICK

            LeaveRequest.Create req = LeaveRequest.Create.builder()
                .leaveType(com.deepanshu.hrportal.model.LeaveRequest.LeaveType.SICK)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2)) // 2 sick days
                .reason("Fever and rest")
                .build();

            when(employeeRepository.findActiveByUsername("deepanshu")).thenReturn(Optional.of(employee));
            when(leaveRepository.countOverlapping(eq(2L), any(), any())).thenReturn(0L);
            when(leaveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Should NOT throw — sick balance is sufficient even though annual is 0
            assertThatCode(() -> leaveService.apply(req)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("✅ CASUAL/MATERNITY/PATERNITY leaves — no balance check")
        void apply_casualLeave_noBalanceCheck() {
            employee.setAnnualLeaveBalance(0);
            employee.setSickLeaveBalance(0); // all balances empty

            LeaveRequest.Create req = LeaveRequest.Create.builder()
                .leaveType(com.deepanshu.hrportal.model.LeaveRequest.LeaveType.CASUAL)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .reason("Personal reason")
                .build();

            when(employeeRepository.findActiveByUsername("deepanshu")).thenReturn(Optional.of(employee));
            when(leaveRepository.countOverlapping(eq(2L), any(), any())).thenReturn(0L);
            when(leaveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> leaveService.apply(req)).doesNotThrowAnyException();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPROVE Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("approve() — Manager Approval")
    class ApproveTests {

        @Test
        @DisplayName("✅ Direct manager approves → status APPROVED + balance deducted")
        void approve_directManager_approvesAndDeductsBalance() {
            mockSecurityContext("engmanager");
            when(employeeRepository.findActiveByUsername("engmanager")).thenReturn(Optional.of(manager));
            when(leaveRepository.findById(10L)).thenReturn(Optional.of(pendingLeave));
            when(leaveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            int balanceBefore = employee.getAnnualLeaveBalance(); // 15
            LeaveResponse.LeaveDetail result = leaveService.approve(10L, "Approved — enjoy!");

            assertThat(result.getStatus()).isEqualTo(com.deepanshu.hrportal.model.LeaveRequest.Status.APPROVED);
            assertThat(result.getManagerComment()).isEqualTo("Approved — enjoy!");
            // Balance must have been deducted (3 working days: Mon+Tue+Wed)
            assertThat(employee.getAnnualLeaveBalance()).isLessThan(balanceBefore);
            verify(employeeRepository).save(employee); // confirms balance save
        }

        @Test
        @DisplayName("✅ ADMIN can approve any employee's leave")
        void approve_admin_canApproveAnyLeave() {
            mockSecurityContext("admin");
            when(employeeRepository.findActiveByUsername("admin")).thenReturn(Optional.of(adminUser));
            when(leaveRepository.findById(10L)).thenReturn(Optional.of(pendingLeave));
            when(leaveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> leaveService.approve(10L, "Admin approved"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("❌ Guard: non-manager employee cannot approve → throws ForbiddenException")
        void approve_regularEmployee_throwsForbidden() {
            Employee otherEmployee = Employee.builder()
                .id(5L).username("other").role(Role.EMPLOYEE).isActive(true).build();
            // otherEmployee is NOT the manager of pendingLeave.employee

            mockSecurityContext("other");
            when(employeeRepository.findActiveByUsername("other")).thenReturn(Optional.of(otherEmployee));
            when(leaveRepository.findById(10L)).thenReturn(Optional.of(pendingLeave));

            assertThatThrownBy(() -> leaveService.approve(10L, "Trying to approve"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("direct reports");

            verify(leaveRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ Guard: approving already APPROVED leave → throws ValidationException")
        void approve_alreadyApproved_throwsValidationException() {
            pendingLeave.setStatus(com.deepanshu.hrportal.model.LeaveRequest.Status.APPROVED);

            mockSecurityContext("engmanager");
            when(employeeRepository.findActiveByUsername("engmanager")).thenReturn(Optional.of(manager));
            when(leaveRepository.findById(10L)).thenReturn(Optional.of(pendingLeave));

            assertThatThrownBy(() -> leaveService.approve(10L, "Double approve"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("❌ Guard: approving REJECTED leave → throws ValidationException")
        void approve_rejectedLeave_throwsValidationException() {
            pendingLeave.setStatus(com.deepanshu.hrportal.model.LeaveRequest.Status.REJECTED);

            mockSecurityContext("engmanager");
            when(employeeRepository.findActiveByUsername("engmanager")).thenReturn(Optional.of(manager));
            when(leaveRepository.findById(10L)).thenReturn(Optional.of(pendingLeave));

            assertThatThrownBy(() -> leaveService.approve(10L, null))
                .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("❌ Leave not found → throws ResourceNotFoundException")
        void approve_leaveNotFound_throwsResourceNotFoundException() {
            mockSecurityContext("engmanager");
            when(employeeRepository.findActiveByUsername("engmanager")).thenReturn(Optional.of(manager));
            when(leaveRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> leaveService.approve(999L, "comment"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REJECT Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reject() — Manager Rejection")
    class RejectTests {

        @Test
        @DisplayName("✅ Manager rejects with comment → status REJECTED, balance NOT touched")
        void reject_withComment_setsRejectedStatus() {
            mockSecurityContext("engmanager");
            when(employeeRepository.findActiveByUsername("engmanager")).thenReturn(Optional.of(manager));
            when(leaveRepository.findById(10L)).thenReturn(Optional.of(pendingLeave));
            when(leaveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            int balanceBefore = employee.getAnnualLeaveBalance();
            LeaveResponse.LeaveDetail result = leaveService.reject(10L, "Deadline conflict this sprint");

            assertThat(result.getStatus()).isEqualTo(com.deepanshu.hrportal.model.LeaveRequest.Status.REJECTED);
            assertThat(result.getManagerComment()).isEqualTo("Deadline conflict this sprint");
            // Balance must NOT be deducted on reject
            assertThat(employee.getAnnualLeaveBalance()).isEqualTo(balanceBefore);
            verify(employeeRepository, never()).save(any()); // no balance update
        }

        @Test
        @DisplayName("❌ Guard: blank rejection comment → throws ValidationException")
        void reject_blankComment_throwsValidationException() {
            mockSecurityContext("engmanager");
            when(employeeRepository.findActiveByUsername("engmanager")).thenReturn(Optional.of(manager));
            when(leaveRepository.findById(10L)).thenReturn(Optional.of(pendingLeave));

            assertThatThrownBy(() -> leaveService.reject(10L, ""))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("reason");

            assertThatThrownBy(() -> leaveService.reject(10L, "   "))
                .isInstanceOf(ValidationException.class);

            verify(leaveRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ Guard: rejecting non-PENDING leave → throws ValidationException")
        void reject_nonPendingLeave_throwsValidationException() {
            pendingLeave.setStatus(com.deepanshu.hrportal.model.LeaveRequest.Status.APPROVED);

            mockSecurityContext("engmanager");
            when(employeeRepository.findActiveByUsername("engmanager")).thenReturn(Optional.of(manager));
            when(leaveRepository.findById(10L)).thenReturn(Optional.of(pendingLeave));

            assertThatThrownBy(() -> leaveService.reject(10L, "Won't work"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("❌ Guard: unauthorized employee rejects → throws ForbiddenException")
        void reject_unauthorizedEmployee_throwsForbidden() {
            Employee stranger = Employee.builder()
                .id(9L).username("stranger").role(Role.EMPLOYEE).isActive(true).build();

            mockSecurityContext("stranger");
            when(employeeRepository.findActiveByUsername("stranger")).thenReturn(Optional.of(stranger));
            when(leaveRepository.findById(10L)).thenReturn(Optional.of(pendingLeave));

            assertThatThrownBy(() -> leaveService.reject(10L, "I reject this"))
                .isInstanceOf(ForbiddenException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancel() — Leave Cancellation")
    class CancelTests {

        @Test
        @DisplayName("✅ Employee cancels own PENDING leave → CANCELLED, balance not changed")
        void cancel_ownPendingLeave_setsCancelled() {
            when(employeeRepository.findActiveByUsername("deepanshu")).thenReturn(Optional.of(employee));
            when(leaveRepository.findById(10L)).thenReturn(Optional.of(pendingLeave));
            when(leaveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            int balanceBefore = employee.getAnnualLeaveBalance();
            LeaveResponse.LeaveDetail result = leaveService.cancel(10L);

            assertThat(result.getStatus()).isEqualTo(com.deepanshu.hrportal.model.LeaveRequest.Status.CANCELLED);
            // PENDING cancel — balance was never deducted, so nothing to restore
            assertThat(employee.getAnnualLeaveBalance()).isEqualTo(balanceBefore);
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ Employee cancels APPROVED future leave → balance RESTORED")
        void cancel_approvedFutureLeave_restoresBalance() {
            pendingLeave.setStatus(com.deepanshu.hrportal.model.LeaveRequest.Status.APPROVED);
            pendingLeave.setStartDate(LocalDate.now().plusDays(5));  // future
            pendingLeave.setEndDate(LocalDate.now().plusDays(7));    // 3 working days
            employee.setAnnualLeaveBalance(12); // was 15, 3 days deducted on approval

            when(employeeRepository.findActiveByUsername("deepanshu")).thenReturn(Optional.of(employee));
            when(leaveRepository.findById(10L)).thenReturn(Optional.of(pendingLeave));
            when(leaveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            leaveService.cancel(10L);

            // Balance should be restored (+3 days back)
            assertThat(employee.getAnnualLeaveBalance()).isEqualTo(15);
            verify(employeeRepository).save(employee); // confirms balance restoration saved
        }

        @Test
        @DisplayName("❌ Guard: employee cancels another employee's leave → ForbiddenException")
        void cancel_otherEmployeeLeave_throwsForbidden() {
            Employee attacker = Employee.builder()
                .id(99L).username("attacker").role(Role.EMPLOYEE).isActive(true).build();

            mockSecurityContext("attacker");
            when(employeeRepository.findActiveByUsername("attacker")).thenReturn(Optional.of(attacker));
            when(leaveRepository.findById(10L)).thenReturn(Optional.of(pendingLeave));

            assertThatThrownBy(() -> leaveService.cancel(10L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own");
        }

        @Test
        @DisplayName("❌ Guard: cancel already REJECTED leave → throws ValidationException")
        void cancel_rejectedLeave_throwsValidationException() {
            pendingLeave.setStatus(com.deepanshu.hrportal.model.LeaveRequest.Status.REJECTED);

            when(employeeRepository.findActiveByUsername("deepanshu")).thenReturn(Optional.of(employee));
            when(leaveRepository.findById(10L)).thenReturn(Optional.of(pendingLeave));

            assertThatThrownBy(() -> leaveService.cancel(10L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("REJECTED");
        }

        @Test
        @DisplayName("❌ Guard: cancel APPROVED leave that already started → ValidationException")
        void cancel_approvedLeaveAlreadyStarted_throwsValidationException() {
            pendingLeave.setStatus(com.deepanshu.hrportal.model.LeaveRequest.Status.APPROVED);
            pendingLeave.setStartDate(LocalDate.now().minusDays(1)); // started yesterday
            pendingLeave.setEndDate(LocalDate.now().plusDays(2));

            when(employeeRepository.findActiveByUsername("deepanshu")).thenReturn(Optional.of(employee));
            when(leaveRepository.findById(10L)).thenReturn(Optional.of(pendingLeave));

            assertThatThrownBy(() -> leaveService.cancel(10L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("started");
        }

        @Test
        @DisplayName("✅ ADMIN can cancel any employee's leave")
        void cancel_admin_canCancelAnyLeave() {
            mockSecurityContext("admin");
            when(employeeRepository.findActiveByUsername("admin")).thenReturn(Optional.of(adminUser));
            when(leaveRepository.findById(10L)).thenReturn(Optional.of(pendingLeave));
            when(leaveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> leaveService.cancel(10L)).doesNotThrowAnyException();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private void mockSecurityContext(String username) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(username);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }
}
