package com.inmobiliaria.identity_service.service;

import com.inmobiliaria.identity_service.domain.UserDocument;
import com.inmobiliaria.identity_service.domain.UserStatus;
import com.inmobiliaria.identity_service.dto.request.ChangePasswordRequest;
import com.inmobiliaria.identity_service.dto.request.LoginRequest;
import com.inmobiliaria.identity_service.dto.response.AuthResponse;
import com.inmobiliaria.identity_service.exception.TemporaryPasswordExpiredException;
import com.inmobiliaria.identity_service.exception.UnauthorizedException;
import com.inmobiliaria.identity_service.security.JwtService;
import com.inmobiliaria.identity_service.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RoleClientService roleClientService;

    public AuthResponse login(LoginRequest request) {
        UserDocument user = userService.findByEmailNormalized(request.email().trim().toLowerCase());

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User is not active");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (Boolean.TRUE.equals(user.getTemporaryPassword())
                && user.getTemporaryPasswordExpiresAt() != null
                && Instant.now().isAfter(user.getTemporaryPasswordExpiresAt())) {
            throw new TemporaryPasswordExpiredException("Temporary password has expired");
        }

        String refreshToken = UUID.randomUUID().toString();
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        user.setLastLoginAt(Instant.now());
        userService.save(user);

        // Resolve role IDs (e.g. "rol_admin") to their codes (e.g. "ADMIN")
        // to ensure Spring Security authorities match @PreAuthorize("hasRole('ADMIN')")
        List<String> roleCodes = roleClientService.resolveRoleCodes(user.getPrimaryRoleIds());

        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getEmailNormalized(),
                roleCodes,
                user.getUserType().name(),
                user.getStatus().name()
        );

        return new AuthResponse(
                jwtService.generateAccessToken(principal),
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                Boolean.TRUE.equals(user.getMustChangePassword())
        );
    }

    public AuthResponse refresh(String refreshToken) {
        UserDocument user = userService.findByRefreshToken(refreshToken);
        
        if (user == null || user.getRefreshTokenExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        // Rotate Refresh Token
        String newRefreshToken = UUID.randomUUID().toString();
        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        userService.save(user);

        List<String> roleCodes = roleClientService.resolveRoleCodes(user.getPrimaryRoleIds());

        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getEmailNormalized(),
                roleCodes,
                user.getUserType().name(),
                user.getStatus().name()
        );

        return new AuthResponse(
                jwtService.generateAccessToken(principal),
                newRefreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                Boolean.TRUE.equals(user.getMustChangePassword())
        );
    }

    public void logout(String refreshToken) {
        UserDocument user = userService.findByRefreshToken(refreshToken);
        if (user != null) {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiresAt(null);
            userService.save(user);
        }
    }

    public void changePassword(ChangePasswordRequest request) {
        UserDocument user = userService.findByEmailNormalized(request.email().trim().toLowerCase());

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is invalid");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setTemporaryPassword(false);
        user.setTemporaryPasswordExpiresAt(null);
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(Instant.now());

        userService.save(user);
    }
}