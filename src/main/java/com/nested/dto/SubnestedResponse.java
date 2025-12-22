package com.nested.dto;

import com.nested.model.Subnested;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubnestedResponse {
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
    private List<Subnested.Flair> flairs;
    private List<String> moderatorIds;
    private boolean isModerator;
}
