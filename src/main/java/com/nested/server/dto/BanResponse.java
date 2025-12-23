package com.nested.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BanResponse {

    private String id;
    private String subId;
    private String subName;
    private String userId;
    private String username;
    private String bannedByUsername;
    private String reason;
    private boolean permanent;
    private String expiresAt;
    private String createdAt;
    private boolean active;
}
