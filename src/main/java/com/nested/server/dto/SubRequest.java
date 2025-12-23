package com.nested.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubRequest {

    @NotBlank(message = "sub name is required")
    @Size(min = 3, max = 21, message = "sub name must be between 3 and 21 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "sub name can only contain letters, numbers, and underscores")
    private String name;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    private String bannerUrl;

    private String iconUrl;
}
