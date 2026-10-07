package com.employeehub.repository;

import com.employeehub.entity.LeaveRequest;
import com.employeehub.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveRepository extends JpaRepository<LeaveRequest, Long> {
    void deleteByEmployeeId(Long employeeId);
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
    List<LeaveRequest> findTop5ByOrderByCreatedAtDesc();
    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    long countByStatus(LeaveStatus status);
    Optional<LeaveRequest> findByIdAndEmployeeId(Long id, Long employeeId);
    @Query("""
        select count(l) > 0 from LeaveRequest l
        where l.employee.id = :employeeId
          and l.status in (com.employeehub.entity.LeaveStatus.PENDING, com.employeehub.entity.LeaveStatus.APPROVED)
          and l.startDate <= :endDate and l.endDate >= :startDate
        """)
    boolean hasOverlappingActiveRequest(@Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
