package com.nested.controller;

import com.nested.model.User;
import com.nested.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "bio", user.getBio() != null ? user.getBio() : "",
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "karma", user.getKarma(),
                "createdAt", user.getCreatedAt().toString(),
                "savedPosts", user.getSavedPosts() != null ? user.getSavedPosts() : java.util.Set.of(),
                "hiddenPosts", user.getHiddenPosts() != null ? user.getHiddenPosts() : java.util.Set.of()
        ));
    }

    @GetMapping("/{username}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(Map.of(
                        "id", (Object) user.getId(),
                        "username", user.getUsername(),
                        "bio", user.getBio() != null ? user.getBio() : "",
                        "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                        "karma", user.getKarma(),
                        "createdAt", user.getCreatedAt().toString()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody Map<String, String> updates,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        User updatedUser = userService.updateProfile(user.getId(), updates);

        return ResponseEntity.ok(Map.of(
                "id", updatedUser.getId(),
                "username", updatedUser.getUsername(),
                "email", updatedUser.getEmail(),
                "bio", updatedUser.getBio() != null ? updatedUser.getBio() : "",
                "avatarUrl", updatedUser.getAvatarUrl() != null ? updatedUser.getAvatarUrl() : "",
                "karma", updatedUser.getKarma(),
                "message", "Profile updated successfully"
        ));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody Map<String, String> passwords,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String currentPassword = passwords.get("currentPassword");
        String newPassword = passwords.get("newPassword");

        userService.changePassword(user.getId(), currentPassword, newPassword);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        userService.initiatePasswordReset(email);
        return ResponseEntity.ok(Map.of("message", "If an account exists with this email, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        userService.resetPassword(token, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }
}
