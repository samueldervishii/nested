package com.nested.server.controller;

import com.nested.server.model.User;
import com.nested.server.service.FileStorageService;
import com.nested.server.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ImageController {

    private final FileStorageService fileStorageService;
    private final UserService userService;

    @PostMapping("/api/users/me/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete old avatar if it's a local file
        String oldAvatarUrl = user.getAvatarUrl();
        if (oldAvatarUrl != null && oldAvatarUrl.startsWith("/uploads/")) {
            fileStorageService.deleteFile(oldAvatarUrl);
        }

        // Store new avatar
        String avatarUrl = fileStorageService.storeAvatar(file, user.getId());

        // Update user profile
        userService.updateProfile(user.getId(), Map.of("avatarUrl", avatarUrl));

        return ResponseEntity.ok(Map.of(
                "avatarUrl", avatarUrl,
                "message", "Avatar uploaded successfully"
        ));
    }

    @DeleteMapping("/api/users/me/avatar")
    public ResponseEntity<Map<String, String>> deleteAvatar(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete old avatar if it's a local file
        String oldAvatarUrl = user.getAvatarUrl();
        if (oldAvatarUrl != null && oldAvatarUrl.startsWith("/uploads/")) {
            fileStorageService.deleteFile(oldAvatarUrl);
        }

        // Set avatar to null (will use default)
        userService.updateProfile(user.getId(), Map.of("avatarUrl", ""));

        return ResponseEntity.ok(Map.of("message", "Avatar deleted successfully"));
    }

    @PostMapping("/api/images/post")
    public ResponseEntity<Map<String, String>> uploadPostImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "postId", required = false) String postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        // Generate temp ID if no postId provided (for new posts)
        String id = postId != null ? postId : "temp_" + System.currentTimeMillis();

        String imageUrl = fileStorageService.storePostImage(file, id);

        return ResponseEntity.ok(Map.of(
                "imageUrl", imageUrl,
                "message", "Image uploaded successfully"
        ));
    }

    @GetMapping("/uploads/{type}/{filename:.+}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String type,
            @PathVariable String filename) {

        try {
            Path filePath = fileStorageService.getFilePath(type + "/" + filename);

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("Could not read file: {}", filename, e);
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Could not determine content type for: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
