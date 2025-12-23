package com.nested.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFlairRequest {

    @NotBlank(message = "Flair text is required")
    @Size(max = 64, message = "Flair text must be at most 64 characters")
    private String text;

    private String textColor;

    private String backgroundColor;

    private boolean userEditable;
}
