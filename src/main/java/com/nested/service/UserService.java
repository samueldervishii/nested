package com.nested.service;

import com.nested.model.User;
import com.nested.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

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

    public User save(User user) {
        return userRepository.save(user);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public void updateKarma(String userId, int delta) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setKarma(user.getKarma() + delta);
            userRepository.save(user);
        });
    }

    public void subscribeToSubnested(String userId, String subnestedId) {
        userRepository.findById(userId).ifPresent(user -> {
            if (!user.getSubscribedSubnested().contains(subnestedId)) {
                user.getSubscribedSubnested().add(subnestedId);
                userRepository.save(user);
            }
        });
    }

    public void unsubscribeFromSubnested(String userId, String subnestedId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.getSubscribedSubnested().remove(subnestedId);
            userRepository.save(user);
        });
    }

    public User updateProfile(String userId, Map<String, String> updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updates.containsKey("bio")) {
            user.setBio(updates.get("bio"));
        }
        if (updates.containsKey("avatarUrl")) {
            user.setAvatarUrl(updates.get("avatarUrl"));
        }

        return userRepository.save(user);
    }

    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
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

            // In a real application, send email with reset link
            // For now, just log it (token can be retrieved via API for testing)
            System.out.println("Password reset token for " + email + ": " + resetToken);
        });
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(Instant.now())) {
            throw new RuntimeException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    public boolean toggleSavePost(String userId, String postId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getSavedPosts() == null) {
            user.setSavedPosts(new java.util.HashSet<>());
        }

        boolean saved;
        if (user.getSavedPosts().contains(postId)) {
            user.getSavedPosts().remove(postId);
            saved = false;
        } else {
            user.getSavedPosts().add(postId);
            saved = true;
        }

        userRepository.save(user);
        return saved;
    }

    public boolean toggleHidePost(String userId, String postId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getHiddenPosts() == null) {
            user.setHiddenPosts(new java.util.HashSet<>());
        }

        boolean hidden;
        if (user.getHiddenPosts().contains(postId)) {
            user.getHiddenPosts().remove(postId);
            hidden = false;
        } else {
            user.getHiddenPosts().add(postId);
            hidden = true;
        }

        userRepository.save(user);
        return hidden;
    }
}
