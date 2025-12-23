package com.nested.server.controller;

import com.nested.server.dto.*;
import com.nested.server.exception.ResourceNotFoundException;
import com.nested.server.model.Report;
import com.nested.server.model.User;
import com.nested.server.service.ModerationService;
import com.nested.server.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mod")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;
    private final UserService userService;

    // ==================== REPORT ENDPOINTS ====================

    @PostMapping("/reports")
    public ResponseEntity<ReportResponse> createReport(
            @Valid @RequestBody ReportRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        ReportResponse response = moderationService.createReport(request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/subs/{subId}/queue")
    public ResponseEntity<List<ReportResponse>> getModQueue(
            @PathVariable String subId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        List<ReportResponse> reports = moderationService.getModQueue(subId, user, page, size);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/subs/{subId}/reports")
    public ResponseEntity<List<ReportResponse>> getAllReports(
            @PathVariable String subId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        List<ReportResponse> reports = moderationService.getAllReportsForSub(subId, user, page, size);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/subs/{subId}/queue/count")
    public ResponseEntity<Map<String, Long>> getPendingReportCount(
            @PathVariable String subId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        long count = moderationService.getPendingReportCount(subId, user);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/reports/{reportId}/resolve")
    public ResponseEntity<ReportResponse> resolveReport(
            @PathVariable String reportId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        String statusStr = body.getOrDefault("status", "RESOLVED");
        String modNote = body.get("modNote");

        Report.ReportStatus status = Report.ReportStatus.valueOf(statusStr.toUpperCase());
        ReportResponse response = moderationService.resolveReport(reportId, status, modNote, user);
        return ResponseEntity.ok(response);
    }

    // ==================== BAN ENDPOINTS ====================

    @PostMapping("/subs/{subId}/bans")
    public ResponseEntity<BanResponse> banUser(
            @PathVariable String subId,
            @Valid @RequestBody BanRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        BanResponse response = moderationService.banUser(subId, request, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/subs/{subId}/bans/{userId}")
    public ResponseEntity<Map<String, String>> unbanUser(
            @PathVariable String subId,
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        moderationService.unbanUser(subId, userId, user);
        return ResponseEntity.ok(Map.of("message", "User unbanned successfully"));
    }

    @GetMapping("/subs/{subId}/bans")
    public ResponseEntity<List<BanResponse>> getBannedUsers(
            @PathVariable String subId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        List<BanResponse> bans = moderationService.getBannedUsers(subId, user, page, size);
        return ResponseEntity.ok(bans);
    }

    // ==================== CONTENT REMOVAL ENDPOINTS ====================

    @PostMapping("/posts/{postId}/remove")
    public ResponseEntity<Map<String, String>> removePost(
            @PathVariable String postId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        String reason = body.get("reason");
        moderationService.removePost(postId, reason, user);
        return ResponseEntity.ok(Map.of("message", "Post removed successfully"));
    }

    @PostMapping("/posts/{postId}/approve")
    public ResponseEntity<Map<String, String>> approvePost(
            @PathVariable String postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        moderationService.approvePost(postId, user);
        return ResponseEntity.ok(Map.of("message", "Post approved successfully"));
    }

    @PostMapping("/comments/{commentId}/remove")
    public ResponseEntity<Map<String, String>> removeComment(
            @PathVariable String commentId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        String reason = body.get("reason");
        moderationService.removeComment(commentId, reason, user);
        return ResponseEntity.ok(Map.of("message", "Comment removed successfully"));
    }

    @PostMapping("/comments/{commentId}/approve")
    public ResponseEntity<Map<String, String>> approveComment(
            @PathVariable String commentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        moderationService.approveComment(commentId, user);
        return ResponseEntity.ok(Map.of("message", "Comment approved successfully"));
    }

    // ==================== PIN ENDPOINTS ====================

    @PostMapping("/posts/{postId}/pin")
    public ResponseEntity<Map<String, String>> pinPost(
            @PathVariable String postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        moderationService.pinPost(postId, user);
        return ResponseEntity.ok(Map.of("message", "Post pinned successfully"));
    }

    @DeleteMapping("/posts/{postId}/pin")
    public ResponseEntity<Map<String, String>> unpinPost(
            @PathVariable String postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        moderationService.unpinPost(postId, user);
        return ResponseEntity.ok(Map.of("message", "Post unpinned successfully"));
    }

    // ==================== LOCK ENDPOINTS ====================

    @PostMapping("/posts/{postId}/lock")
    public ResponseEntity<Map<String, String>> lockPost(
            @PathVariable String postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        moderationService.lockPost(postId, user);
        return ResponseEntity.ok(Map.of("message", "Post locked successfully"));
    }

    @DeleteMapping("/posts/{postId}/lock")
    public ResponseEntity<Map<String, String>> unlockPost(
            @PathVariable String postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        moderationService.unlockPost(postId, user);
        return ResponseEntity.ok(Map.of("message", "Post unlocked successfully"));
    }

    // ==================== HELPER METHODS ====================

    private User getUser(UserDetails userDetails) {
        return userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", userDetails.getUsername()));
    }
}
