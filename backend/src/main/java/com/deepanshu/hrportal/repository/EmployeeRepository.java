package com.deepanshu.hrportal.repository;

import com.deepanshu.hrportal.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username);
    Optional<Employee> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT e FROM Employee e WHERE e.isActive = true AND e.username = :username")
    Optional<Employee> findActiveByUsername(String username);

    @EntityGraph(attributePaths = {"manager"})
    Page<Employee> findByIsActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"manager"})
    Page<Employee> findByManagerId(Long managerId, Pageable pageable);

    @EntityGraph(attributePaths = {"manager"})
    Page<Employee> findByDepartment(String department, Pageable pageable);
}
