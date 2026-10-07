package com.employeehub.repository;

import com.employeehub.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByEmployeeCode(String employeeCode);
    long countByStatus(EmployeeStatus status);
    long countByDepartmentId(Long departmentId);
    List<Employee> findTop5ByOrderByCreatedAtDesc();

    @Query("""
        select e from Employee e join e.department d
        where (:keyword is null or lower(concat(e.firstName, ' ', e.lastName)) like lower(concat('%', :keyword, '%'))
          or lower(e.email) like lower(concat('%', :keyword, '%'))
          or lower(e.employeeCode) like lower(concat('%', :keyword, '%'))
          or lower(e.designation) like lower(concat('%', :keyword, '%'))
          or lower(d.name) like lower(concat('%', :keyword, '%')))
          and (:designation is null or lower(e.designation) like lower(concat('%', :designation, '%')))
          and (:departmentId is null or d.id = :departmentId)
          and (:status is null or e.status = :status)
        """)
    Page<Employee> search(@Param("keyword") String keyword, @Param("departmentId") Long departmentId,
                          @Param("status") EmployeeStatus status, @Param("designation") String designation,
                          Pageable pageable);
}
