package com.nested.server.dto;

import com.nested.server.model.Report;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

    @NotNull(message = "Target type is required")
    private Report.TargetType targetType;

    @NotBlank(message = "Target ID is required")
    private String targetId;

    @NotNull(message = "Report reason is required")
    private Report.ReportReason reason;

    private String description;
}
