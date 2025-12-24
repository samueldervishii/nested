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
@Document(collection = "subs")
public class Subs {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String description;

    private String bannerUrl;

    private String iconUrl;

    @Indexed
    private String creatorId;

    private String creatorUsername;

    @Builder.Default
    private List<String> moderatorIds = new ArrayList<>();

    @Builder.Default
    private List<String> rules = new ArrayList<>();

    @Builder.Default
    private List<Flair> flairs = new ArrayList<>();

    @Builder.Default
    private Set<String> subscriberIds = new HashSet<>();

    @Builder.Default
    private Set<String> bannedUserIds = new HashSet<>();

    @Indexed
    @Builder.Default
    private int subscriberCount = 0;

    @Builder.Default
    private Instant createdAt = Instant.now();

    // Helper method to get accurate subscriber count
    public int getSubscriberCount() {
        return subscriberIds != null ? subscriberIds.size() : subscriberCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Flair {
        private String name;
        private String color;
        private String backgroundColor;
    }
}
