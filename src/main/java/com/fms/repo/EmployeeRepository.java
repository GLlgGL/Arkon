package com.fms.repo;

import com.fms.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    java.util.List<Employee> findAllByOrderByActiveDescNameAsc();
}
