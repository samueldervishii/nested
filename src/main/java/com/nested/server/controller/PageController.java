package com.nested.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/submit")
    public String submit() {
        return "submit";
    }

    @GetMapping("/n/{subs}")
    public String subs(@PathVariable String subs) {
        return "sub";
    }

    @GetMapping("/n/{subs}/comments/{postId}")
    public String post(@PathVariable String subs, @PathVariable String postId) {
        return "post";
    }

    @GetMapping("/n/{subs}/comments/{postId}/{title}")
    public String postWithTitle(@PathVariable String subs, @PathVariable String postId, @PathVariable String title) {
        return "post";
    }

    @GetMapping("/u/{username}")
    public String userProfile(@PathVariable String username) {
        return "user";
    }

    @GetMapping("/sub/create")
    public String createSub() {
        return "create-sub";
    }

    @GetMapping("/search")
    public String search() {
        return "search";
    }

    @GetMapping("/saved")
    public String savedPosts() {
        return "saved";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword() {
        return "reset-password";
    }

    @GetMapping("/my-communities")
    public String myCommunities() {
        return "my-communities";
    }

    @GetMapping("/c/popular")
    public String popular() {
        return "index";
    }

    @GetMapping("/c/all")
    public String all() {
        return "index";
    }
}
