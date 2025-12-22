package com.nested.dto;

import com.nested.model.Vote;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteRequest {

    @NotBlank(message = "Target ID is required")
    private String targetId;

    @NotNull(message = "Target type is required")
    private Vote.VoteTargetType targetType;

    @NotNull(message = "Vote type is required")
    private Vote.VoteType voteType;
}
