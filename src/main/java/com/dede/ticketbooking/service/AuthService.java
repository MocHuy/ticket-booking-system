package com.dede.ticketbooking.service;

import com.dede.ticketbooking.config.JwtUtil;
import com.dede.ticketbooking.dto.AuthDTO.*;
import com.dede.ticketbooking.model.*;
import com.dede.ticketbooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .gender(request.getGender())
                .build();
        user = userRepository.save(user);

        // Assign CUSTOMER role
        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("CUSTOMER").description("Khách hàng").isSystemRole(1).build()));

        userRoleRepository.save(UserRole.builder()
                .userId(user.getUserId())
                .roleId(customerRole.getRoleId())
                .build());

        // Create customer profile
        customerRepository.save(Customer.builder()
                .userId(user.getUserId())
                .customerCode("CUST-" + String.format("%06d", user.getUserId()))
                .build());

        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), "CUSTOMER");

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role("CUSTOMER")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid username or password");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Account is " + user.getStatus());
        }

        // Get primary role
        var userRoles = userRoleRepository.findByUserId(user.getUserId());
        String role = "CUSTOMER";
        if (!userRoles.isEmpty()) {
            var roleEntity = roleRepository.findById(userRoles.get(0).getRoleId());
            if (roleEntity.isPresent()) {
                role = roleEntity.get().getRoleName();
            }
        }

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), role);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(role)
                .build();
    }
}
