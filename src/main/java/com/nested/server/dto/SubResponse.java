package com.nested.server.dto;

import com.nested.server.model.Subs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubResponse {
    private String id;
    private String name;
    private String description;
    private String bannerUrl;
    private String iconUrl;
    private String creatorUsername;
    private int subscriberCount;
    private String createdAt;
    private boolean isSubscribed;
    private List<String> rules;
    private List<Subs.Flair> flairs;
    private List<String> moderatorIds;
    private boolean isModerator;
}
