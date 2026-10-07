package com.employeehub.controller;

import com.employeehub.dto.*;
import com.employeehub.entity.AttendanceStatus;
import com.employeehub.repository.UserRepository;
import com.employeehub.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceService service;
    public AttendanceController(AttendanceService service) { this.service = service; }
    @GetMapping public List<AttendanceResponse> all(@RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to, @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) AttendanceStatus status) { return service.all(from, to, employeeId, status); }
    @GetMapping("/mine") public List<AttendanceResponse> mine(Authentication authentication) {
        return service.mine(authentication.getName());
    }
    @PostMapping("/mine/check-in")
    public AttendanceResponse checkIn(Authentication authentication) { return service.checkIn(authentication.getName()); }
    @PostMapping("/mine/check-out")
    public AttendanceResponse checkOut(Authentication authentication) { return service.checkOut(authentication.getName()); }
    @GetMapping("/{id}") public AttendanceResponse get(@PathVariable Long id) { return service.get(id); }
    @GetMapping("/employee/{employeeId}") public List<AttendanceResponse> byEmployee(@PathVariable Long employeeId) {
        return service.byEmployee(employeeId);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public AttendanceResponse create(@Valid @RequestBody AttendanceRequest request) { return service.create(request); }
    @PutMapping("/{id}")
    public AttendanceResponse update(@PathVariable Long id, @Valid @RequestBody AttendanceRequest request) { return service.update(id, request); }
}
