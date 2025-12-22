package com.nested.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubnestedRequest {

    @NotBlank(message = "Subnested name is required")
    @Size(min = 3, max = 21, message = "Subnested name must be between 3 and 21 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Subnested name can only contain letters, numbers, and underscores")
    private String name;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    private String bannerUrl;

    private String iconUrl;
}
