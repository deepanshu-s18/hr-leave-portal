package com.deepanshu.hrportal.controller;

import com.deepanshu.hrportal.dto.request.LeaveRequest;
import com.deepanshu.hrportal.dto.response.LeaveResponse;
import com.deepanshu.hrportal.model.LeaveRequest.Status;
import com.deepanshu.hrportal.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Leave Management", description = "Apply, approve, reject, cancel and query leave requests")
public class LeaveController {

    private final LeaveService leaveService;

    // ── EMPLOYEE ENDPOINTS ────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Apply for leave",
               description = "Submit a leave request. Validates balance, past-dates, and date overlaps.")
    public ResponseEntity<LeaveResponse.LeaveDetail> apply(
            @Valid @RequestBody LeaveRequest.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.apply(request));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my leave requests (paginated)",
               description = "Returns current employee's leave history with optional status filter.")
    public ResponseEntity<Page<LeaveResponse.LeaveDetail>> getMyLeaves(
            @RequestParam(required = false) Status status,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(leaveService.getMyLeaves(status, pageable));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a leave request",
               description = "Employee can cancel PENDING or future-APPROVED requests. Balance is restored on cancel.")
    public ResponseEntity<LeaveResponse.LeaveDetail> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(leaveService.cancel(id));
    }

    // ── MANAGER ENDPOINTS ─────────────────────────────────────────────────────

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','HR')")
    @Operation(summary = "Get pending leave requests for my team",
               description = "Returns PENDING leave requests from the manager's direct reports.")
    public ResponseEntity<Page<LeaveResponse.LeaveDetail>> getPendingForManager(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(leaveService.getPendingForManager(pageable));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','HR')")
    @Operation(summary = "Approve a leave request",
               description = "Approve a PENDING request. Validates manager authorization and deducts employee balance.")
    public ResponseEntity<LeaveResponse.LeaveDetail> approve(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) LeaveRequest.ApproveReject body) {
        String comment = body != null ? body.getComment() : null;
        return ResponseEntity.ok(leaveService.approve(id, comment));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','HR')")
    @Operation(summary = "Reject a leave request",
               description = "Reject a PENDING request. A rejection comment is mandatory.")
    public ResponseEntity<LeaveResponse.LeaveDetail> reject(
            @PathVariable Long id,
            @Valid @RequestBody LeaveRequest.ApproveReject body) {
        return ResponseEntity.ok(leaveService.reject(id, body.getComment()));
    }

    // ── ADMIN / HR ENDPOINTS ──────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "List all leave requests (ADMIN/HR only)",
               description = "Returns paginated, optionally status-filtered list of all leave requests.")
    public ResponseEntity<Page<LeaveResponse.LeaveDetail>> getAll(
            @RequestParam(required = false) Status status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(leaveService.getAll(status, pageable));
    }
}
