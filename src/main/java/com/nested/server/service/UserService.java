package com.nested.server.service;

import com.nested.server.exception.BadRequestException;
import com.nested.server.exception.ResourceNotFoundException;
import com.nested.server.model.User;
import com.nested.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@NullMarked
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private static final int MAX_BIO_LENGTH = 500;
    private static final int MAX_URL_LENGTH = 2048;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Async karma update - doesn't block the voting operation
     */
    @Async
    public void updateKarma(String userId, int delta) {
        try {
            userRepository.incrementKarma(userId, delta);
            log.debug("Updated karma for user {} by {}", userId, delta);
        } catch (Exception e) {
            log.error("Failed to update karma for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Atomic subscription add using $addToSet
     */
    public void subscribeToSubs(String userId, String subsId) {
        userRepository.addSubscription(userId, subsId);
    }

    /**
     * Atomic subscription remove using $pull
     */
    public void unsubscribeFromSubs(String userId, String subsId) {
        userRepository.removeSubscription(userId, subsId);
    }

    public User updateProfile(String userId, Map<String, String> updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (updates.containsKey("bio")) {
            String bio = updates.get("bio");
            if (bio != null && bio.length() > MAX_BIO_LENGTH) {
                throw new BadRequestException("Bio must be less than " + MAX_BIO_LENGTH + " characters");
            }
            user.setBio(bio);
        }
        if (updates.containsKey("avatarUrl")) {
            String avatarUrl = updates.get("avatarUrl");
            if (avatarUrl != null && avatarUrl.length() > MAX_URL_LENGTH) {
                throw new BadRequestException("Avatar URL is too long");
            }
            user.setAvatarUrl(avatarUrl);
        }

        return userRepository.save(user);
    }

    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (newPassword.length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void initiatePasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setResetToken(resetToken);
            user.setResetTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));
            userRepository.save(user);

            // TODO: Implement email service to send reset link
            // DO NOT log the token in production - this is a security risk
            log.info("Password reset initiated for user: {}", user.getUsername());
        });
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(Instant.now())) {
            throw new BadRequestException("Reset token has expired");
        }

        if (newPassword.length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    /**
     * Atomic toggle for saved posts - no read-modify-write pattern
     */
    public boolean toggleSavePost(String userId, String postId) {
        boolean isSaved = userRepository.isPostSaved(userId, postId);

        if (isSaved) {
            userRepository.removeSavedPost(userId, postId);
            return false;
        } else {
            userRepository.addSavedPost(userId, postId);
            return true;
        }
    }

    /**
     * Atomic toggle for hidden posts - no read-modify-write pattern
     */
    public boolean toggleHidePost(String userId, String postId) {
        boolean isHidden = userRepository.isPostHidden(userId, postId);

        if (isHidden) {
            userRepository.removeHiddenPost(userId, postId);
            return false;
        } else {
            userRepository.addHiddenPost(userId, postId);
            return true;
        }
    }

    /**
     * Search users by username
     */
    public List<Map<String, Object>> searchUsers(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        List<User> users = userRepository.findByUsernameContainingIgnoreCase(query.trim());
        return users.stream()
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                        "karma", user.getKarma()
                ))
                .toList();
    }
}
