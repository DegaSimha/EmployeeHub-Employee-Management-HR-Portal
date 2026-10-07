package com.employeehub.service;

import com.employeehub.dto.*;
import com.employeehub.entity.*;
import com.employeehub.exception.ResourceNotFoundException;
import com.employeehub.exception.BadRequestException;
import com.employeehub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class ProfileService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    public ProfileService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users; this.passwordEncoder = passwordEncoder;
    }
    @Transactional(readOnly = true)
    public ProfileResponse get(String email) { return response(requireUser(email)); }
    @Transactional
    public ProfileResponse update(String email, ProfileUpdateRequest request) {
        UserAccount user = requireUser(email);
        Employee employee = user.getEmployee();
        if (employee != null) employee.update(employee.getFirstName(), employee.getLastName(), employee.getEmail(),
                request.phone() == null ? employee.getPhone() : request.phone(),
                employee.getDateOfBirth(), employee.getGender(), employee.getDepartment(), employee.getDesignation(),
                employee.getSalary(), employee.getJoiningDate(), employee.getEmploymentType(), employee.getStatus(),
                request.address() == null ? employee.getAddress() : request.address(),
                request.city() == null ? employee.getCity() : request.city(),
                request.state() == null ? employee.getState() : request.state(),
                request.country() == null ? employee.getCountry() : request.country());
        return response(user);
    }
    @Transactional
    public void changePassword(String email, PasswordChangeRequest request) {
        UserAccount user = requireUser(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword()))
            throw new BadRequestException("Your current password is incorrect.");
        if (request.currentPassword().equals(request.newPassword()))
            throw new BadRequestException("Choose a new password that is different from your current password.");
        user.updatePassword(passwordEncoder.encode(request.newPassword()));
    }
    private UserAccount requireUser(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
    private ProfileResponse response(UserAccount user) {
        Employee e = user.getEmployee();
        return new ProfileResponse(e == null ? null : e.getId(), e == null ? null : e.getEmployeeCode(),
                e == null ? "HR" : e.getFirstName(), e == null ? "Administrator" : e.getLastName(),
                user.getEmail(), e == null ? null : e.getPhone(),
                e == null ? null : e.getDepartment().getName(), e == null ? "Human Resources" : e.getDesignation(),
                e == null ? null : e.getJoiningDate(), e == null ? null : e.getAddress(),
                e == null ? null : e.getCity(), e == null ? null : e.getState(), e == null ? null : e.getCountry(),
                user.getRole());
    }
}
