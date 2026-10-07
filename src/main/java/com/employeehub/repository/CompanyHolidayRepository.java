package com.employeehub.repository;

import com.employeehub.entity.CompanyHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompanyHolidayRepository extends JpaRepository<CompanyHoliday, Long> {
    List<CompanyHoliday> findAllByOrderByHolidayDateAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
