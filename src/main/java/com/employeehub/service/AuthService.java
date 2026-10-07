package com.employeehub.service;

import com.employeehub.dto.*;
import com.employeehub.entity.UserAccount;
import com.employeehub.exception.BadRequestException;
import com.employeehub.repository.UserRepository;
import com.employeehub.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserAccount user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(candidate -> encoder.matches(request.password(), candidate.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Email or password is incorrect."));
        String name = user.getEmployee() == null ? "HR Administrator"
                : user.getEmployee().getFirstName() + " " + user.getEmployee().getLastName();
        return new LoginResponse(jwt.createToken(user.getEmail(), user.getRole().name()), "Bearer",
                user.getEmail(), user.getRole().name(), name);
    }
}
