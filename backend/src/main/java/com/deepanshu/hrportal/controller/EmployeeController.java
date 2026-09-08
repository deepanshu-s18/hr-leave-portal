package com.deepanshu.hrportal.controller;

import com.deepanshu.hrportal.dto.response.LeaveResponse;
import com.deepanshu.hrportal.exception.ResourceNotFoundException;
import com.deepanshu.hrportal.model.Employee;
import com.deepanshu.hrportal.repository.EmployeeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Employees", description = "Employee profile and management endpoints")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;

    @GetMapping("/me")
    @Operation(summary = "Get current employee profile")
    public ResponseEntity<LeaveResponse.EmployeeSummary> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        Employee emp = employeeRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "username", userDetails.getUsername()));
        return ResponseEntity.ok(LeaveResponse.EmployeeSummary.from(emp));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','HR')")
    @Operation(summary = "List all active employees (Manager/Admin/HR)")
    public ResponseEntity<Page<LeaveResponse.EmployeeSummary>> getAll(
            @RequestParam(required = false) String department,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<Employee> page = department != null
            ? employeeRepository.findByDepartment(department, pageable)
            : employeeRepository.findByIsActiveTrue(pageable);
        return ResponseEntity.ok(page.map(LeaveResponse.EmployeeSummary::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','HR')")
    @Operation(summary = "Get employee by ID")
    public ResponseEntity<LeaveResponse.EmployeeSummary> getById(@PathVariable Long id) {
        Employee emp = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        return ResponseEntity.ok(LeaveResponse.EmployeeSummary.from(emp));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate employee account (ADMIN only)")
    public ResponseEntity<LeaveResponse.MessageResponse> deactivate(@PathVariable Long id) {
        Employee emp = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        emp.setIsActive(false);
        employeeRepository.save(emp);
        return ResponseEntity.ok(new LeaveResponse.MessageResponse("Employee deactivated: " + emp.getFullName()));
    }
}
