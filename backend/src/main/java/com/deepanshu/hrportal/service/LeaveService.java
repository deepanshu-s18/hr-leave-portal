package com.deepanshu.hrportal.service;

import com.deepanshu.hrportal.dto.request.LeaveRequest;
import com.deepanshu.hrportal.dto.response.LeaveResponse;
import com.deepanshu.hrportal.exception.ConflictException;
import com.deepanshu.hrportal.exception.ForbiddenException;
import com.deepanshu.hrportal.exception.ResourceNotFoundException;
import com.deepanshu.hrportal.exception.ValidationException;
import com.deepanshu.hrportal.model.Employee;
import com.deepanshu.hrportal.model.LeaveRequest.LeaveType;
import com.deepanshu.hrportal.model.LeaveRequest.Status;
import com.deepanshu.hrportal.repository.EmployeeRepository;
import com.deepanshu.hrportal.repository.LeaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;

    // ── APPLY ────────────────────────────────────────────────────────────────

    @Transactional
    public LeaveResponse.LeaveDetail apply(LeaveRequest.Create request) {
        Employee employee = getCurrentEmployee();

        // Guard 1: start date must not be in the past
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new ValidationException("Start date cannot be in the past");
        }

        // Guard 2: end date must not be before start date
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("End date must be on or after start date");
        }

        // Guard 3: no overlapping pending/approved leaves
        long overlaps = leaveRepository.countOverlapping(
            employee.getId(), request.getStartDate(), request.getEndDate());
        if (overlaps > 0) {
            throw new ConflictException(
                "You already have a pending or approved leave overlapping the selected dates");
        }

        // Guard 4: check leave balance (annual and sick)
        long workingDays = countWorkingDays(request.getStartDate(), request.getEndDate());
        validateLeaveBalance(employee, request.getLeaveType(), workingDays);

        com.deepanshu.hrportal.model.LeaveRequest leave =
            com.deepanshu.hrportal.model.LeaveRequest.builder()
                .employee(employee)
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .status(Status.PENDING)
                .build();

        leave = leaveRepository.save(leave);
        log.info("Employee '{}' applied for {} leave ({} → {})",
            employee.getUsername(), leave.getLeaveType(), leave.getStartDate(), leave.getEndDate());
        return LeaveResponse.from(leave);
    }

    // ── APPROVE ──────────────────────────────────────────────────────────────

    @Transactional
    public LeaveResponse.LeaveDetail approve(Long leaveId, String managerComment) {
        Employee manager = getCurrentEmployee();
        com.deepanshu.hrportal.model.LeaveRequest leave = findLeave(leaveId);

        // Guard: only PENDING can be approved
        if (leave.getStatus() != Status.PENDING) {
            throw new ValidationException(
                "Only PENDING requests can be approved. Current status: " + leave.getStatus());
        }

        // Guard: manager can only approve their direct reports' leaves (unless ADMIN/HR)
        boolean isAdminOrHr = manager.getRole() == Employee.Role.ADMIN || manager.getRole() == Employee.Role.HR;
        boolean isDirectManager = leave.getEmployee().getManager() != null &&
            leave.getEmployee().getManager().getId().equals(manager.getId());

        if (!isAdminOrHr && !isDirectManager) {
            throw new ForbiddenException(
                "You can only approve leave for your direct reports");
        }

        // Deduct leave balance
        long workingDays = countWorkingDays(leave.getStartDate(), leave.getEndDate());
        deductBalance(leave.getEmployee(), leave.getLeaveType(), (int) workingDays);

        leave.setStatus(Status.APPROVED);
        leave.setApprovedBy(manager);
        leave.setManagerComment(managerComment);
        leave.setApprovedAt(LocalDateTime.now());

        leave = leaveRepository.save(leave);
        employeeRepository.save(leave.getEmployee()); // persist balance deduction

        log.info("Manager '{}' APPROVED leave #{} for '{}'",
            manager.getUsername(), leaveId, leave.getEmployee().getUsername());
        return LeaveResponse.from(leave);
    }

    // ── REJECT ───────────────────────────────────────────────────────────────

    @Transactional
    public LeaveResponse.LeaveDetail reject(Long leaveId, String managerComment) {
        Employee manager = getCurrentEmployee();
        com.deepanshu.hrportal.model.LeaveRequest leave = findLeave(leaveId);

        // Guard: only PENDING can be rejected
        if (leave.getStatus() != Status.PENDING) {
            throw new ValidationException(
                "Only PENDING requests can be rejected. Current status: " + leave.getStatus());
        }

        // Guard: authorization check (same as approve)
        boolean isAdminOrHr = manager.getRole() == Employee.Role.ADMIN || manager.getRole() == Employee.Role.HR;
        boolean isDirectManager = leave.getEmployee().getManager() != null &&
            leave.getEmployee().getManager().getId().equals(manager.getId());
        if (!isAdminOrHr && !isDirectManager) {
            throw new ForbiddenException("You can only reject leave for your direct reports");
        }

        if (managerComment == null || managerComment.isBlank()) {
            throw new ValidationException("A rejection reason is required");
        }

        leave.setStatus(Status.REJECTED);
        leave.setApprovedBy(manager);
        leave.setManagerComment(managerComment);

        leave = leaveRepository.save(leave);
        log.info("Manager '{}' REJECTED leave #{} for '{}'",
            manager.getUsername(), leaveId, leave.getEmployee().getUsername());
        return LeaveResponse.from(leave);
    }

    // ── CANCEL ───────────────────────────────────────────────────────────────

    @Transactional
    public LeaveResponse.LeaveDetail cancel(Long leaveId) {
        Employee employee = getCurrentEmployee();
        com.deepanshu.hrportal.model.LeaveRequest leave = findLeave(leaveId);

        // Guard: only own leaves or admin can cancel
        boolean isOwner = leave.getEmployee().getId().equals(employee.getId());
        boolean isAdminOrHr = employee.getRole() == Employee.Role.ADMIN || employee.getRole() == Employee.Role.HR;
        if (!isOwner && !isAdminOrHr) {
            throw new ForbiddenException("You can only cancel your own leave requests");
        }

        // Guard: can only cancel PENDING or APPROVED future leaves
        if (leave.getStatus() == Status.REJECTED || leave.getStatus() == Status.CANCELLED) {
            throw new ValidationException("Cannot cancel a " + leave.getStatus() + " request");
        }
        if (leave.getStatus() == Status.APPROVED && leave.getStartDate().isBefore(LocalDate.now())) {
            throw new ValidationException("Cannot cancel a leave that has already started");
        }

        // Restore balance if APPROVED
        if (leave.getStatus() == Status.APPROVED) {
            long workingDays = countWorkingDays(leave.getStartDate(), leave.getEndDate());
            restoreBalance(leave.getEmployee(), leave.getLeaveType(), (int) workingDays);
            employeeRepository.save(leave.getEmployee());
        }

        leave.setStatus(Status.CANCELLED);
        leave = leaveRepository.save(leave);
        log.info("Leave #{} cancelled by '{}'", leaveId, employee.getUsername());
        return LeaveResponse.from(leave);
    }

    // ── QUERIES ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<LeaveResponse.LeaveDetail> getMyLeaves(Status status, Pageable pageable) {
        Employee employee = getCurrentEmployee();
        if (status != null) {
            return leaveRepository.findByEmployeeIdAndStatus(employee.getId(), status, pageable)
                .map(LeaveResponse::from);
        }
        return leaveRepository.findByEmployeeId(employee.getId(), pageable)
            .map(LeaveResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<LeaveResponse.LeaveDetail> getPendingForManager(Pageable pageable) {
        Employee manager = getCurrentEmployee();
        return leaveRepository.findPendingByManagerId(manager.getId(), pageable)
            .map(LeaveResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<LeaveResponse.LeaveDetail> getAll(Status status, Pageable pageable) {
        if (status != null) {
            return leaveRepository.findByStatus(status, pageable).map(LeaveResponse::from);
        }
        return leaveRepository.findAll(pageable).map(LeaveResponse::from);
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    /** Count Mon–Fri working days (excluding weekends) */
    private long countWorkingDays(LocalDate start, LocalDate end) {
        long days = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (current.getDayOfWeek().getValue() < 6) days++;
            current = current.plusDays(1);
        }
        return days;
    }

    private void validateLeaveBalance(Employee employee, LeaveType type, long days) {
        int balance = switch (type) {
            case ANNUAL       -> employee.getAnnualLeaveBalance();
            case SICK         -> employee.getSickLeaveBalance();
            default           -> Integer.MAX_VALUE; // CASUAL, MATERNITY, etc. — no balance check
        };
        if (days > balance) {
            throw new ValidationException(
                "Insufficient leave balance. Requested: " + days + " days. Available: " + balance);
        }
    }

    private void deductBalance(Employee employee, LeaveType type, int days) {
        switch (type) {
            case ANNUAL -> employee.setAnnualLeaveBalance(employee.getAnnualLeaveBalance() - days);
            case SICK   -> employee.setSickLeaveBalance(employee.getSickLeaveBalance() - days);
            default     -> {} // No balance deduction for other types
        }
    }

    private void restoreBalance(Employee employee, LeaveType type, int days) {
        switch (type) {
            case ANNUAL -> employee.setAnnualLeaveBalance(employee.getAnnualLeaveBalance() + days);
            case SICK   -> employee.setSickLeaveBalance(employee.getSickLeaveBalance() + days);
            default     -> {}
        }
    }

    private com.deepanshu.hrportal.model.LeaveRequest findLeave(Long id) {
        return leaveRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));
    }

    private Employee getCurrentEmployee() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return employeeRepository.findActiveByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "username", username));
    }
}
