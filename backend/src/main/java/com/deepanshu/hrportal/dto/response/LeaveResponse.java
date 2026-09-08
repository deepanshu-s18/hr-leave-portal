package com.deepanshu.hrportal.dto.response;

import com.deepanshu.hrportal.model.Employee;
import com.deepanshu.hrportal.model.LeaveRequest;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LeaveResponse {

    // ── Employee Summary ───────────────────────────────────────────────────────

    @Value @Builder
    public static class EmployeeSummary {
        Long id;
        String username;
        String fullName;
        String department;
        String employeeId;
        Employee.Role role;
        int annualLeaveBalance;
        int sickLeaveBalance;

        public static EmployeeSummary from(Employee e) {
            return EmployeeSummary.builder()
                .id(e.getId())
                .username(e.getUsername())
                .fullName(e.getFullName())
                .department(e.getDepartment())
                .employeeId(e.getEmployeeId())
                .role(e.getRole())
                .annualLeaveBalance(e.getAnnualLeaveBalance())
                .sickLeaveBalance(e.getSickLeaveBalance())
                .build();
        }
    }

    // ── Leave Detail ───────────────────────────────────────────────────────────

    @Value @Builder
    public static class LeaveDetail {
        Long id;
        EmployeeSummary employee;
        EmployeeSummary approvedBy;
        LeaveRequest.LeaveType leaveType;
        LocalDate startDate;
        LocalDate endDate;
        long workingDays;
        String reason;
        String managerComment;
        LeaveRequest.Status status;
        LocalDateTime approvedAt;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;

        public static LeaveDetail from(LeaveRequest lr) {
            return LeaveDetail.builder()
                .id(lr.getId())
                .employee(EmployeeSummary.from(lr.getEmployee()))
                .approvedBy(lr.getApprovedBy() != null ? EmployeeSummary.from(lr.getApprovedBy()) : null)
                .leaveType(lr.getLeaveType())
                .startDate(lr.getStartDate())
                .endDate(lr.getEndDate())
                .workingDays(lr.getWorkingDays())
                .reason(lr.getReason())
                .managerComment(lr.getManagerComment())
                .status(lr.getStatus())
                .approvedAt(lr.getApprovedAt())
                .createdAt(lr.getCreatedAt())
                .updatedAt(lr.getUpdatedAt())
                .build();
        }
    }

    /**
     * Top-level factory — delegates to {@link LeaveDetail#from(LeaveRequest)}.
     * Allows LeaveService to call LeaveResponse.from(leave) cleanly.
     */
    public static LeaveDetail from(LeaveRequest lr) {
        return LeaveDetail.from(lr);
    }

    // ── Error Response ─────────────────────────────────────────────────────────

    @Value @Builder
    public static class ErrorResponse {
        int status;
        String error;
        String message;
        String path;
        LocalDateTime timestamp;
    }

    // ── Message Response ───────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageResponse {
        private String message;
    }
}
