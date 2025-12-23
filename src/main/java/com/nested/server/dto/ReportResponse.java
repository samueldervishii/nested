package com.nested.server.dto;

import com.nested.server.model.Report;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private String id;
    private String reporterUsername;
    private Report.TargetType targetType;
    private String targetId;
    private Report.ReportReason reason;
    private String description;
    private String subId;
    private String subName;
    private Report.ReportStatus status;
    private String reviewedByUsername;
    private String reviewedAt;
    private String modNote;
    private String createdAt;

    // Additional context about the reported content
    private String targetTitle;
    private String targetContent;
    private String targetAuthorUsername;
}
