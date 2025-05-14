package com.hotelreservation.service;

import com.hotelreservation.dto.request.LoginRequest;
import com.hotelreservation.dto.request.PasswordChangeRequest;
import com.hotelreservation.dto.request.ProfileUpdateRequest;
import com.hotelreservation.dto.request.RegisterRequest;
import com.hotelreservation.dto.response.AuthResponse;
import com.hotelreservation.dto.response.UserResponse;
import com.hotelreservation.exception.BadRequestException;
import com.hotelreservation.exception.ResourceNotFoundException;
import com.hotelreservation.exception.UnauthorizedException;
import com.hotelreservation.exception.UserAlreadyExistsException;
import com.hotelreservation.model.User;
import com.hotelreservation.model.UserRole;
import com.hotelreservation.repository.UserRepository;
import com.hotelreservation.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        try {
            log.info("Attempting to register user with email: {}", request.getEmail());

            if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("Registration failed: Email already in use: {}", request.getEmail());
                throw new UserAlreadyExistsException("Email already in use");
            }

            UserRole role = UserRole.CUSTOMER;
            if (request.getRole() != null && !request.getRole().isEmpty()) {
                try {
                    role = UserRole.valueOf(request.getRole().toUpperCase());
                    log.info("Setting user role to: {}", role);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid role provided: {}. Defaulting to CUSTOMER", request.getRole());
                    // Default to CUSTOMER if invalid role
                }
            }

            User user = User.builder()
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phone(request.getPhone())
                    .role(role)
                    .build();

            log.debug("Saving user to database: {}", user.getEmail());
            userRepository.save(user);
            log.info("User registered successfully: {}", user.getEmail());

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPasswordHash())
                    .authorities("ROLE_" + user.getRole().name())
                    .build();

            String token = jwtTokenProvider.generateToken(userDetails);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

            return AuthResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .build();
        } catch (Exception e) {
            log.error("Error during user registration", e);
            throw e; // Re-throw to be handled by global exception handler
        }
    }

    public AuthResponse login(LoginRequest request) {
        try {
            log.info("Attempting to login user: {}", request.getEmail());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            String token = jwtTokenProvider.generateToken(userDetails);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

            log.info("User logged in successfully: {}", user.getEmail());
            return AuthResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .build();
        } catch (Exception e) {
            log.error("Error during user login", e);
            throw e; // Re-throw to be handled by global exception handler
        }
    }

    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToUserResponse(user);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(ProfileUpdateRequest request) {
        log.info("Updating profile for current user");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if email is being changed and if it's already in use
        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            log.warn("Profile update failed: Email already in use: {}", request.getEmail());
            throw new UserAlreadyExistsException("Email already in use");
        }

        // Update user information
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        // Handle password change if requested
        if (request.getCurrentPassword() != null && !request.getCurrentPassword().isEmpty() &&
                request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {

            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                log.warn("Password change failed: Current password is incorrect");
                throw new UnauthorizedException("Current password is incorrect");
            }

            // Verify password confirmation
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                log.warn("Password change failed: New passwords do not match");
                throw new BadRequestException("New passwords do not match");
            }

            // Update password
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            log.info("Password updated successfully for user: {}", user.getEmail());
        }

        userRepository.save(user);
        log.info("Profile updated successfully for user: {}", user.getEmail());

        return mapToUserResponse(user);
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request) {
        log.info("Changing password for current user");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Password change failed: Current password is incorrect");
            throw new UnauthorizedException("Current password is incorrect");
        }

        // Verify password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("Password change failed: New passwords do not match");
            throw new BadRequestException("New passwords do not match");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getEmail());
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
