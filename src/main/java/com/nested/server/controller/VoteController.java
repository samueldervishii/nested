package com.nested.server.controller;

import com.nested.server.dto.VoteRequest;
import com.nested.server.dto.VoteResult;
import com.nested.server.model.User;
import com.nested.server.service.UserService;
import com.nested.server.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<VoteResult> vote(
            @Valid @RequestBody VoteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        VoteResult result = voteService.vote(request, user);
        return ResponseEntity.ok(result);
    }
}
