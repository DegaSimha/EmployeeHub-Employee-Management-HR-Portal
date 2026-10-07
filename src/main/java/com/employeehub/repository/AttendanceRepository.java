package com.employeehub.repository;

import com.employeehub.entity.AttendanceRecord;
import com.employeehub.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {
    void deleteByEmployeeId(Long employeeId);
    List<AttendanceRecord> findByEmployeeIdOrderByAttendanceDateDesc(Long employeeId);
    Optional<AttendanceRecord> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);
    long countByStatus(AttendanceStatus status);
    @Query("select a from AttendanceRecord a join fetch a.employee e join fetch e.department where (:fromDate is null or a.attendanceDate >= :fromDate) and (:toDate is null or a.attendanceDate <= :toDate) and (:employeeId is null or e.id = :employeeId) and (:status is null or a.status = :status) order by a.attendanceDate desc")
    List<AttendanceRecord> findWithinDates(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate,
            @Param("employeeId") Long employeeId, @Param("status") AttendanceStatus status);
}
