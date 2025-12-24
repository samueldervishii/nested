package com.nested.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteResult {
    private int voteCount;
    private int userVote; // 1, -1, or 0 (no vote)
}
