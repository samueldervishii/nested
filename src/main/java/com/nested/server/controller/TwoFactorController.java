package com.nested.server.controller;

import com.nested.server.dto.TwoFactorSetupResponse;
import com.nested.server.dto.TwoFactorStatusResponse;
import com.nested.server.dto.TwoFactorVerifyRequest;
import com.nested.server.exception.BadRequestException;
import com.nested.server.model.User;
import com.nested.server.repository.UserRepository;
import com.nested.server.service.TwoFactorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final UserRepository userRepository;

    /**
     * Get current 2FA status for the authenticated user
     */
    @GetMapping("/status")
    public ResponseEntity<TwoFactorStatusResponse> getStatus(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);

        return ResponseEntity.ok(TwoFactorStatusResponse.builder()
                .enabled(user.isTwoFactorEnabled())
                .message(user.isTwoFactorEnabled() ? "2FA is enabled" : "2FA is not enabled")
                .build());
    }

    /**
     * Generate a new 2FA setup (secret + QR code)
     * This doesn't enable 2FA yet - user must verify the code first
     */
    @PostMapping("/setup")
    public ResponseEntity<TwoFactorSetupResponse> setup(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);

        if (user.isTwoFactorEnabled()) {
            throw new BadRequestException("2FA is already enabled. Disable it first to set up again.");
        }

        String secret = twoFactorService.generateSecret();
        String qrCodeDataUrl = twoFactorService.generateQrCodeDataUrl(secret, user.getUsername());
        String manualEntryKey = twoFactorService.getManualEntryKey(secret);

        log.info("2FA setup initiated for user: {}", user.getUsername());

        return ResponseEntity.ok(TwoFactorSetupResponse.builder()
                .secret(secret)
                .qrCodeDataUrl(qrCodeDataUrl)
                .manualEntryKey(manualEntryKey)
                .build());
    }

    /**
     * Verify and enable 2FA
     * User must provide the secret from setup and a valid TOTP code
     */
    @PostMapping("/enable")
    public ResponseEntity<TwoFactorStatusResponse> enable(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TwoFactorVerifyRequest verifyRequest) {

        User user = getAuthenticatedUser(userDetails);

        if (user.isTwoFactorEnabled()) {
            throw new BadRequestException("2FA is already enabled");
        }

        if (verifyRequest.getSecret() == null || verifyRequest.getSecret().isEmpty()) {
            throw new BadRequestException("Secret is required for initial 2FA setup");
        }

        // Verify the code with the provided secret
        if (!twoFactorService.verifyCode(verifyRequest.getSecret(), verifyRequest.getCode())) {
            throw new BadRequestException("Invalid verification code. Please try again.");
        }

        // Enable 2FA and store the secret
        user.setTwoFactorEnabled(true);
        user.setTwoFactorSecret(verifyRequest.getSecret());
        userRepository.save(user);

        log.info("2FA enabled for user: {}", user.getUsername());

        return ResponseEntity.ok(TwoFactorStatusResponse.builder()
                .enabled(true)
                .message("2FA has been enabled successfully")
                .build());
    }

    /**
     * Disable 2FA
     * User must provide a valid TOTP code to disable
     */
    @PostMapping("/disable")
    public ResponseEntity<TwoFactorStatusResponse> disable(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TwoFactorVerifyRequest verifyRequest) {

        User user = getAuthenticatedUser(userDetails);

        if (!user.isTwoFactorEnabled()) {
            throw new BadRequestException("2FA is not enabled");
        }

        // Verify the code before disabling
        if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), verifyRequest.getCode())) {
            throw new BadRequestException("Invalid verification code");
        }

        // Disable 2FA
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);

        log.info("2FA disabled for user: {}", user.getUsername());

        return ResponseEntity.ok(TwoFactorStatusResponse.builder()
                .enabled(false)
                .message("2FA has been disabled")
                .build());
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BadRequestException("Not authenticated");
        }
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BadRequestException("User not found"));
    }
}
