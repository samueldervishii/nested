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
@Document(collection = "subnesteds")
public class Subnested {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String description;

    private String bannerUrl;

    private String iconUrl;

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
