package com.employeehub.service;

import com.employeehub.dto.HolidayRequest;
import com.employeehub.dto.HolidayResponse;
import com.employeehub.entity.CompanyHoliday;
import com.employeehub.exception.BadRequestException;
import com.employeehub.exception.ResourceNotFoundException;
import com.employeehub.repository.CompanyHolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CompanyHolidayService {
    private final CompanyHolidayRepository holidays;
    public CompanyHolidayService(CompanyHolidayRepository holidays) { this.holidays = holidays; }
    @Transactional(readOnly = true)
    public List<HolidayResponse> all() {
        return holidays.findAllByOrderByHolidayDateAsc().stream().map(this::response).toList();
    }
    @Transactional(readOnly = true)
    public HolidayResponse get(Long id) { return response(requireHoliday(id)); }
    @Transactional
    public HolidayResponse create(HolidayRequest request) {
        if (holidays.existsByNameIgnoreCase(request.name().trim()))
            throw new BadRequestException("A company holiday with this name already exists.");
        return response(holidays.save(new CompanyHoliday(request.name().trim(), request.holidayDate(),
                request.description(), request.paid() == null || request.paid())));
    }
    @Transactional
    public HolidayResponse update(Long id, HolidayRequest request) {
        CompanyHoliday holiday = requireHoliday(id);
        if (holidays.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id))
            throw new BadRequestException("A company holiday with this name already exists.");
        holiday.update(request.name().trim(), request.holidayDate(), request.description(),
                request.paid() == null ? holiday.isPaid() : request.paid());
        return response(holiday);
    }
    @Transactional
    public void delete(Long id) { holidays.delete(requireHoliday(id)); }
    private CompanyHoliday requireHoliday(Long id) {
        return holidays.findById(id).orElseThrow(() -> new ResourceNotFoundException("Company holiday not found."));
    }
    private HolidayResponse response(CompanyHoliday holiday) {
        return new HolidayResponse(holiday.getId(), holiday.getName(), holiday.getHolidayDate(),
                holiday.getDescription(), holiday.isPaid());
    }
}
