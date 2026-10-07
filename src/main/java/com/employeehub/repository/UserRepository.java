package com.employeehub.repository;

import com.employeehub.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    Optional<UserAccount> findByEmployeeId(Long employeeId);
}
