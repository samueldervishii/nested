package com.nested.controller;

import com.nested.model.Subnested;
import com.nested.dto.SubnestedRequest;
import com.nested.dto.SubnestedResponse;
import com.nested.model.User;
import com.nested.service.SubnestedService;
import com.nested.service.UserService;
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
@RequestMapping("/api/subnested")
@RequiredArgsConstructor
public class SubnestedController {

    private final SubnestedService subnestedService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<SubnestedResponse> createSubnested(
            @Valid @RequestBody SubnestedRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        SubnestedResponse response = subnestedService.createSubnested(request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SubnestedResponse>> getPopularSubnested(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/subnested - Fetching popular communities, user: {}",
                userDetails != null ? userDetails.getUsername() : "anonymous");

        try {
            User user = userDetails != null ?
                    userService.findByUsername(userDetails.getUsername()).orElse(null) : null;

            List<SubnestedResponse> result = subnestedService.getPopularSubnested(user);
            log.info("GET /api/subnested - Returning {} communities", result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("GET /api/subnested - Error fetching popular communities", e);
            throw e;
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<SubnestedResponse>> searchSubnested(
            @RequestParam String q,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userDetails != null ?
                userService.findByUsername(userDetails.getUsername()).orElse(null) : null;

        return ResponseEntity.ok(subnestedService.searchSubnested(q, user));
    }

    @GetMapping("/{name}")
    public ResponseEntity<SubnestedResponse> getSubnested(
            @PathVariable String name,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/subnested/{} - user: {}", name,
                userDetails != null ? userDetails.getUsername() : "anonymous");

        try {
            User user = userDetails != null ?
                    userService.findByUsername(userDetails.getUsername()).orElse(null) : null;

            return subnestedService.findByName(name)
                    .map(sub -> {
                        log.info("GET /api/subnested/{} - Found community with {} subscribers",
                                name, sub.getSubscriberCount());
                        return ResponseEntity.ok(subnestedService.mapToResponse(sub,
                                user != null && user.getSubscribedSubnested().contains(sub.getId())));
                    })
                    .orElseGet(() -> {
                        log.warn("GET /api/subnested/{} - Community not found", name);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            log.error("GET /api/subnested/{} - Error", name, e);
            throw e;
        }
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubnestedResponse>> getSubscriptions(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(subnestedService.getUserSubscriptions(user));
    }

    @PostMapping("/{id}/subscribe")
    public ResponseEntity<Void> subscribe(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        subnestedService.subscribe(id, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unsubscribe")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        subnestedService.unsubscribe(id, user);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubnestedResponse> updateSubnested(
            @PathVariable String id,
            @RequestBody SubnestedRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        subnestedService.updateSubnested(id, request, user);
        return subnestedService.findById(id)
                .map(sub -> ResponseEntity.ok(subnestedService.mapToResponse(sub,
                        user.getSubscribedSubnested().contains(sub.getId()), user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/rules")
    public ResponseEntity<Map<String, String>> updateRules(
            @PathVariable String id,
            @RequestBody List<String> rules,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        subnestedService.updateRules(id, rules, user);
        return ResponseEntity.ok(Map.of("message", "Rules updated successfully"));
    }

    @PostMapping("/{id}/flairs")
    public ResponseEntity<Map<String, String>> addFlair(
            @PathVariable String id,
            @RequestBody Subnested.Flair flair,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        subnestedService.addFlair(id, flair, user);
        return ResponseEntity.ok(Map.of("message", "Flair added successfully"));
    }

    @DeleteMapping("/{id}/flairs/{flairName}")
    public ResponseEntity<Map<String, String>> removeFlair(
            @PathVariable String id,
            @PathVariable String flairName,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        subnestedService.removeFlair(id, flairName, user);
        return ResponseEntity.ok(Map.of("message", "Flair removed successfully"));
    }

    @PostMapping("/{id}/moderators")
    public ResponseEntity<Map<String, String>> addModerator(
            @PathVariable String id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newModeratorId = request.get("userId");
        subnestedService.addModerator(id, newModeratorId, user);
        return ResponseEntity.ok(Map.of("message", "Moderator added successfully"));
    }

    @DeleteMapping("/{id}/moderators/{moderatorId}")
    public ResponseEntity<Map<String, String>> removeModerator(
            @PathVariable String id,
            @PathVariable String moderatorId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        subnestedService.removeModerator(id, moderatorId, user);
        return ResponseEntity.ok(Map.of("message", "Moderator removed successfully"));
    }

    /**
     * One-time migration endpoint to add all existing users to a subnested.
     * Call: POST /api/subnested/migrate/{subnested}
     */
    @PostMapping("/migrate/{subnested}")
    public ResponseEntity<Map<String, Object>> migrateUsersToSubnested(
            @PathVariable String subnested) {

        int count = subnestedService.migrateAllUsersToSubnested(subnested);
        return ResponseEntity.ok(Map.of(
                "message", "Migration complete",
                "subnested", subnested,
                "usersAdded", count
        ));
    }
}
