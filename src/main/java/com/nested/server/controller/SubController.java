package com.nested.server.controller;

import com.nested.server.exception.ResourceNotFoundException;
import com.nested.server.model.Subs;
import com.nested.server.dto.SubRequest;
import com.nested.server.dto.SubResponse;
import com.nested.server.model.User;
import com.nested.server.service.SubService;
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
@RequestMapping("/api/subs")
@RequiredArgsConstructor
public class SubController {

    private final SubService subService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<SubResponse> createSubs(
            @Valid @RequestBody SubRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", userDetails.getUsername()));

        SubResponse response = subService.createSubs(request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SubResponse>> getPopularSubs(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/subs - Fetching popular communities, user: {}",
                userDetails != null ? userDetails.getUsername() : "anonymous");

        try {
            User user = userDetails != null ?
                    userService.findByUsername(userDetails.getUsername()).orElse(null) : null;

            List<SubResponse> result = subService.getPopularSubs(user);
            log.info("GET /api/subs - Returning {} communities", result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("GET /api/subs - Error fetching popular communities", e);
            throw e;
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<SubResponse>> searchSubs(
            @RequestParam String q,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userDetails != null ?
                userService.findByUsername(userDetails.getUsername()).orElse(null) : null;

        return ResponseEntity.ok(subService.searchSubs(q, user));
    }

    @GetMapping("/{name}")
    public ResponseEntity<SubResponse> getSubs(
            @PathVariable String name,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/subs/{} - user: {}", name,
                userDetails != null ? userDetails.getUsername() : "anonymous");

        try {
            User user = userDetails != null ?
                    userService.findByUsername(userDetails.getUsername()).orElse(null) : null;

            return subService.findByName(name)
                    .map(sub -> {
                        log.info("GET /api/subs/{} - Found community with {} subscribers",
                                name, sub.getSubscriberCount());
                        return ResponseEntity.ok(subService.mapToResponse(sub,
                                user != null && user.getSubscribedSubs().contains(sub.getId()), user));
                    })
                    .orElseGet(() -> {
                        log.warn("GET /api/subs/{} - Community not found", name);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            log.error("GET /api/subs/{} - Error", name, e);
            throw e;
        }
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubResponse>> getSubscriptions(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", userDetails.getUsername()));

        return ResponseEntity.ok(subService.getUserSubscriptions(user));
    }

    @PostMapping("/{id}/subscribe")
    public ResponseEntity<Void> subscribe(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", userDetails.getUsername()));

        subService.subscribe(id, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unsubscribe")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", userDetails.getUsername()));

        subService.unsubscribe(id, user);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubResponse> updateSubs(
            @PathVariable String id,
            @RequestBody SubRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", userDetails.getUsername()));

        subService.updateSubs(id, request, user);
        return subService.findById(id)
                .map(sub -> ResponseEntity.ok(subService.mapToResponse(sub,
                        user.getSubscribedSubs().contains(sub.getId()), user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/rules")
    public ResponseEntity<Map<String, String>> updateRules(
            @PathVariable String id,
            @RequestBody List<String> rules,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", userDetails.getUsername()));

        subService.updateRules(id, rules, user);
        return ResponseEntity.ok(Map.of("message", "Rules updated successfully"));
    }

    @PostMapping("/{id}/flairs")
    public ResponseEntity<Map<String, String>> addFlair(
            @PathVariable String id,
            @RequestBody Subs.Flair flair,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", userDetails.getUsername()));

        subService.addFlair(id, flair, user);
        return ResponseEntity.ok(Map.of("message", "Flair added successfully"));
    }

    @DeleteMapping("/{id}/flairs/{flairName}")
    public ResponseEntity<Map<String, String>> removeFlair(
            @PathVariable String id,
            @PathVariable String flairName,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", userDetails.getUsername()));

        subService.removeFlair(id, flairName, user);
        return ResponseEntity.ok(Map.of("message", "Flair removed successfully"));
    }

    @PostMapping("/{id}/moderators")
    public ResponseEntity<Map<String, String>> addModerator(
            @PathVariable String id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", userDetails.getUsername()));

        String newModeratorId = request.get("userId");
        subService.addModerator(id, newModeratorId, user);
        return ResponseEntity.ok(Map.of("message", "Moderator added successfully"));
    }

    @DeleteMapping("/{id}/moderators/{moderatorId}")
    public ResponseEntity<Map<String, String>> removeModerator(
            @PathVariable String id,
            @PathVariable String moderatorId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", userDetails.getUsername()));

        subService.removeModerator(id, moderatorId, user);
        return ResponseEntity.ok(Map.of("message", "Moderator removed successfully"));
    }

    /**
     * One-time migration endpoint to add all existing users to a Subs.
     * Call: POST /api/subs/migrate/{subName}
     */
    @PostMapping("/migrate/{subName}")
    public ResponseEntity<Map<String, Object>> migrateUsersToSubs(
            @PathVariable String subName) {

        int count = subService.migrateAllUsersToSubs(subName);
        return ResponseEntity.ok(Map.of(
                "message", "Migration complete",
                "subName", subName,
                "usersAdded", count
        ));
    }
}
