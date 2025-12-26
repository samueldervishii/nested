package com.nested.server.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String avatarUrl;

    private String bio;

    private int karma;

    @Builder.Default
    private List<String> subscribedSubs = new ArrayList<>();

    @Builder.Default
    private Set<String> savedPosts = new HashSet<>();

    @Builder.Default
    private Set<String> hiddenPosts = new HashSet<>();

    @Builder.Default
    private Instant createdAt = Instant.now();

    private boolean enabled;

    @Builder.Default
    private String role = "USER";

    // For password reset
    @Indexed(sparse = true)  // Sparse index: only indexes documents where the field exists
    private String resetToken;
    private Instant resetTokenExpiry;

    // Two-Factor Authentication
    @Builder.Default
    private boolean twoFactorEnabled = false;
    private String twoFactorSecret;  // Encrypted TOTP secret
}
