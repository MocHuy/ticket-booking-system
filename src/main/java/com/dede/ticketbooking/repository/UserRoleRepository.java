package com.dede.ticketbooking.repository;

import com.dede.ticketbooking.model.UserRole;
import com.dede.ticketbooking.model.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    List<UserRole> findByUserId(Long userId);
}
