package com.deepanshu.hrportal.service;

import com.deepanshu.hrportal.model.Employee;
import com.deepanshu.hrportal.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findActiveByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Active employee not found: " + username));

        return User.builder()
            .username(employee.getUsername())
            .password(employee.getPassword())
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name())))
            .accountNonExpired(true)
            .credentialsNonExpired(true)
            .accountNonLocked(employee.getIsActive())
            .build();
    }
}
