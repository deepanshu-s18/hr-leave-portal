package com.deepanshu.hrportal.repository;

import com.deepanshu.hrportal.model.LeaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface LeaveRepository extends JpaRepository<LeaveRequest, Long> {

    @EntityGraph(attributePaths = {"employee", "approvedBy"})
    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    @EntityGraph(attributePaths = {"employee", "approvedBy"})
    Page<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveRequest.Status status, Pageable pageable);

    /** Pending requests for a specific manager's direct reports */
    @EntityGraph(attributePaths = {"employee", "approvedBy"})
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.manager.id = :managerId AND lr.status = 'PENDING'")
    Page<LeaveRequest> findPendingByManagerId(Long managerId, Pageable pageable);

    /** Admin: all leave requests with optional status filter */
    @EntityGraph(attributePaths = {"employee", "approvedBy"})
    Page<LeaveRequest> findByStatus(LeaveRequest.Status status, Pageable pageable);

    @EntityGraph(attributePaths = {"employee", "approvedBy"})
    Optional<LeaveRequest> findByIdAndEmployeeId(Long id, Long employeeId);

    /** Overlap check: any APPROVED or PENDING leave overlapping the given date range for the employee */
    @Query("""
        SELECT COUNT(lr) FROM LeaveRequest lr
        WHERE lr.employee.id = :employeeId
          AND lr.status IN ('PENDING', 'APPROVED')
          AND lr.startDate <= :endDate
          AND lr.endDate >= :startDate
        """)
    long countOverlapping(Long employeeId, LocalDate startDate, LocalDate endDate);

    /** Count working-day leaves used this year per type */
    @Query("""
        SELECT COALESCE(SUM(
            FUNCTION('julianday', lr.endDate) - FUNCTION('julianday', lr.startDate) + 1
        ), 0)
        FROM LeaveRequest lr
        WHERE lr.employee.id = :employeeId
          AND lr.leaveType = :leaveType
          AND lr.status = 'APPROVED'
          AND YEAR(lr.startDate) = :year
        """)
    long sumApprovedDaysByTypeAndYear(Long employeeId, LeaveRequest.LeaveType leaveType, int year);
}
