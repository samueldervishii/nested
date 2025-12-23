package com.nested.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFlairResponse {

    private String id;
    private String subId;
    private String userId;
    private String username;
    private String text;
    private String textColor;
    private String backgroundColor;
    private String assignedByUsername;
    private boolean userEditable;
    private String createdAt;
}
