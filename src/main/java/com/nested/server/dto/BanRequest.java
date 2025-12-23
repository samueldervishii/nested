package com.nested.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BanRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    private String reason;

    @Builder.Default
    private boolean permanent = true;

    private Integer durationDays;
}
