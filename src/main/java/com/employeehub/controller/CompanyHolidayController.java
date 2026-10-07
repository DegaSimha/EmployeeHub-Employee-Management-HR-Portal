package com.employeehub.controller;

import com.employeehub.dto.HolidayRequest;
import com.employeehub.dto.HolidayResponse;
import com.employeehub.service.CompanyHolidayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/holidays")
public class CompanyHolidayController {
    private final CompanyHolidayService service;
    public CompanyHolidayController(CompanyHolidayService service) { this.service = service; }
    @GetMapping public List<HolidayResponse> all() { return service.all(); }
    @GetMapping("/{id}") public HolidayResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public HolidayResponse create(@Valid @RequestBody HolidayRequest request) { return service.create(request); }
    @PutMapping("/{id}")
    public HolidayResponse update(@PathVariable Long id, @Valid @RequestBody HolidayRequest request) {
        return service.update(id, request);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
