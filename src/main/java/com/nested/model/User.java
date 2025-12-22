package com.nested.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private List<String> subscribedSubnested = new ArrayList<>();

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
    private String resetToken;
    private Instant resetTokenExpiry;
}
