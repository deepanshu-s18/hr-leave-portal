package com.deepanshu.hrportal.controller;

import com.deepanshu.hrportal.dto.response.LeaveResponse;
import com.deepanshu.hrportal.exception.ConflictException;
import com.deepanshu.hrportal.model.Employee;
import com.deepanshu.hrportal.repository.EmployeeRepository;
import com.deepanshu.hrportal.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login and token refresh")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    // ── DTOs ──────────────────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank String usernameOrEmail;
        @NotBlank String password;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank @Size(min=3, max=50) String username;
        @NotBlank @Email String email;
        @NotBlank @Size(min=8) String password;
        @NotBlank @Size(max=100) String fullName;
        String department;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RefreshRequest {
        @NotBlank String refreshToken;
    }

    @Value @Builder
    public static class AuthResponse {
        String accessToken;
        String refreshToken;
        LeaveResponse.EmployeeSummary user;
    }

    // ── ENDPOINTS ─────────────────────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Login with username/email + password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        // Resolve username from either username or email
        String username = req.getUsernameOrEmail();
        if (username.contains("@")) {
            username = employeeRepository.findByEmail(username)
                .map(Employee::getUsername)
                .orElse(username);
        }
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, req.getPassword()));
        Employee emp = employeeRepository.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(AuthResponse.builder()
            .accessToken(jwtTokenProvider.generateAccessToken(emp.getUsername()))
            .refreshToken(jwtTokenProvider.generateRefreshToken(emp.getUsername()))
            .user(LeaveResponse.EmployeeSummary.from(emp))
            .build());
    }

    @PostMapping("/register")
    @Operation(summary = "Self-registration (creates EMPLOYEE role account)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (employeeRepository.existsByUsername(req.getUsername()))
            throw new ConflictException("Username already taken: " + req.getUsername());
        if (employeeRepository.existsByEmail(req.getEmail()))
            throw new ConflictException("Email already registered: " + req.getEmail());

        Employee emp = Employee.builder()
            .username(req.getUsername())
            .email(req.getEmail())
            .password(passwordEncoder.encode(req.getPassword()))
            .fullName(req.getFullName())
            .department(req.getDepartment())
            .role(Employee.Role.EMPLOYEE)
            .build();
        emp = employeeRepository.save(emp);

        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.builder()
            .accessToken(jwtTokenProvider.generateAccessToken(emp.getUsername()))
            .refreshToken(jwtTokenProvider.generateRefreshToken(emp.getUsername()))
            .user(LeaveResponse.EmployeeSummary.from(emp))
            .build());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        if (!jwtTokenProvider.validate(req.getRefreshToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = jwtTokenProvider.getUsername(req.getRefreshToken());
        Employee emp = employeeRepository.findByUsername(username).orElseThrow();
        return ResponseEntity.ok(AuthResponse.builder()
            .accessToken(jwtTokenProvider.generateAccessToken(username))
            .refreshToken(req.getRefreshToken()) // reuse existing refresh token
            .user(LeaveResponse.EmployeeSummary.from(emp))
            .build());
    }
}
