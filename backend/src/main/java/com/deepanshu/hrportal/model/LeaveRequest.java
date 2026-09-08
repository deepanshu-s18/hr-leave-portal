package com.deepanshu.hrportal.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "leave_requests", indexes = {
    @Index(name = "idx_leave_employee", columnList = "employee_id"),
    @Index(name = "idx_leave_status", columnList = "status"),
    @Index(name = "idx_leave_dates", columnList = "start_date, end_date")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Employee approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 30)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "manager_comment", length = 500)
    private String managerComment;

    /**
     * State machine: PENDING → APPROVED / REJECTED
     *                PENDING → CANCELLED (by employee)
     *                APPROVED → CANCELLED (by employee, if future)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum LeaveType {
        ANNUAL, SICK, CASUAL, MATERNITY, PATERNITY, UNPAID, COMPENSATORY
    }

    public enum Status {
        PENDING, APPROVED, REJECTED, CANCELLED
    }

    /** Business logic: number of working days (Mon-Fri) for this leave request */
    public long getWorkingDays() {
        if (startDate == null || endDate == null) return 0;
        long days = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (current.getDayOfWeek().getValue() < 6) days++;
            current = current.plusDays(1);
        }
        return days;
    }
}
