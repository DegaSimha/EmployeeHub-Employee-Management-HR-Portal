package com.employeehub.controller;

import com.employeehub.dto.*;
import com.employeehub.entity.LeaveStatus;
import com.employeehub.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {
    private final LeaveService service;
    public LeaveController(LeaveService service) { this.service = service; }
    @GetMapping public List<LeaveResponse> all() { return service.all(); }
    @GetMapping("/mine") public List<LeaveResponse> mine(Authentication authentication) { return service.mine(authentication.getName()); }
    @GetMapping("/{id}") public LeaveResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public LeaveResponse apply(Authentication authentication, @Valid @RequestBody LeaveRequestDto request) {
        return service.apply(authentication.getName(), request);
    }
    @DeleteMapping("/mine/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(Authentication authentication, @PathVariable Long id) {
        service.withdraw(authentication.getName(), id);
    }
    @PutMapping("/{id}/approve")
    public LeaveResponse approve(@PathVariable Long id, @RequestBody(required = false) LeaveDecisionRequest request) {
        return service.decide(id, LeaveStatus.APPROVED, request == null ? null : request.comment());
    }
    @PutMapping("/{id}/reject")
    public LeaveResponse reject(@PathVariable Long id, @Valid @RequestBody LeaveDecisionRequest request) {
        return service.decide(id, LeaveStatus.REJECTED, request.comment());
    }
}
