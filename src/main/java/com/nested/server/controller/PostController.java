package com.nested.server.controller;

import com.nested.server.dto.PostRequest;
import com.nested.server.dto.PostResponse;
import com.nested.server.model.User;
import com.nested.server.service.PostService;
import com.nested.server.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        PostResponse response = postService.createPost(request, user);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable String id,
            @RequestBody Map<String, String> updates,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        PostResponse response = postService.updatePost(id, updates, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        postService.deletePost(id, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> getPosts(
            @RequestParam(defaultValue = "hot") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userDetails != null ?
                userService.findByUsername(userDetails.getUsername()).orElse(null) : null;

        List<PostResponse> posts = postService.getHomeFeed(user, sort, page, size);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/hot")
    public ResponseEntity<List<PostResponse>> getHotPosts(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userDetails != null ?
                userService.findByUsername(userDetails.getUsername()).orElse(null) : null;
        return ResponseEntity.ok(postService.getHotPosts(user));
    }

    @GetMapping("/new")
    public ResponseEntity<List<PostResponse>> getNewPosts(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userDetails != null ?
                userService.findByUsername(userDetails.getUsername()).orElse(null) : null;
        return ResponseEntity.ok(postService.getNewPosts(user));
    }

    @GetMapping("/top")
    public ResponseEntity<List<PostResponse>> getTopPosts(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userDetails != null ?
                userService.findByUsername(userDetails.getUsername()).orElse(null) : null;
        return ResponseEntity.ok(postService.getPopularPosts(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userDetails != null ?
                userService.findByUsername(userDetails.getUsername()).orElse(null) : null;

        return postService.findById(id)
                .map(post -> ResponseEntity.ok(postService.mapToResponseWithUserData(post, user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/subs/{subName}")
    public ResponseEntity<List<PostResponse>> getPostsBySub(
            @PathVariable String subName,
            @RequestParam(defaultValue = "hot") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userDetails != null ?
                userService.findByUsername(userDetails.getUsername()).orElse(null) : null;

        List<PostResponse> posts = postService.getPostsBySubs(subName, sort, page, size, user);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostResponse>> getPostsByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userDetails != null ?
                userService.findByUsername(userDetails.getUsername()).orElse(null) : null;

        List<PostResponse> posts = postService.getPostsByUser(userId, page, size, currentUser);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PostResponse>> searchPosts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userDetails != null ?
                userService.findByUsername(userDetails.getUsername()).orElse(null) : null;

        List<PostResponse> posts = postService.searchPosts(q, page, size, user);
        return ResponseEntity.ok(posts);
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<Map<String, Boolean>> savePost(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean saved = userService.toggleSavePost(user.getId(), id);
        return ResponseEntity.ok(Map.of("saved", saved));
    }

    @PostMapping("/{id}/hide")
    public ResponseEntity<Map<String, Boolean>> hidePost(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean hidden = userService.toggleHidePost(user.getId(), id);
        return ResponseEntity.ok(Map.of("hidden", hidden));
    }

    @GetMapping("/saved")
    public ResponseEntity<List<PostResponse>> getSavedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<PostResponse> posts = postService.getSavedPosts(user, page, size);
        return ResponseEntity.ok(posts);
    }
}
