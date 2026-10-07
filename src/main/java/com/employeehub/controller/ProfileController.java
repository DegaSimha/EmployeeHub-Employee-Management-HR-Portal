package com.employeehub.controller;

import com.employeehub.dto.*;
import com.employeehub.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService service;
    public ProfileController(ProfileService service) { this.service = service; }
    @GetMapping public ProfileResponse get(Authentication auth) { return service.get(auth.getName()); }
    @PutMapping public ProfileResponse update(Authentication auth, @Valid @RequestBody ProfileUpdateRequest request) {
        return service.update(auth.getName(), request);
    }
    @PutMapping("/password")
    public org.springframework.http.ResponseEntity<Void> changePassword(Authentication auth,
            @Valid @RequestBody PasswordChangeRequest request) {
        service.changePassword(auth.getName(), request);
        return org.springframework.http.ResponseEntity.noContent().build();
    }
}
