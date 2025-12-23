package com.nested.server.service;

import com.nested.server.dto.*;
import com.nested.server.exception.BadRequestException;
import com.nested.server.exception.DuplicateResourceException;
import com.nested.server.exception.ResourceNotFoundException;
import com.nested.server.exception.UnauthorizedException;
import com.nested.server.model.*;
import com.nested.server.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ReportRepository reportRepository;
    private final BanRepository banRepository;
    private final SubsRepository subsRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;

    // ==================== REPORT METHODS ====================

    public ReportResponse createReport(ReportRequest request, User reporter) {
        // Determine which sub this report belongs to
        String subId = null;
        String subName = null;

        if (request.getTargetType() == Report.TargetType.POST) {
            Post post = postRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Post", "id", request.getTargetId()));
            subId = post.getSubId();
            subName = post.getSubName();
        } else if (request.getTargetType() == Report.TargetType.COMMENT) {
            Comment comment = commentRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.getTargetId()));
            Post post = postRepository.findById(comment.getPostId())
                    .orElseThrow(() -> new ResourceNotFoundException("Post", "id", comment.getPostId()));
            subId = post.getSubId();
            subName = post.getSubName();
        }

        // Check if user already reported this target
        if (reportRepository.existsByReporterIdAndTargetIdAndTargetType(
                reporter.getId(), request.getTargetId(), request.getTargetType())) {
            throw new DuplicateResourceException("Report", "target", request.getTargetId());
        }

        Report report = Report.builder()
                .reporterId(reporter.getId())
                .reporterUsername(reporter.getUsername())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .description(request.getDescription())
                .subId(subId)
                .subName(subName)
                .status(Report.ReportStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        report = reportRepository.save(report);
        return mapToReportResponse(report);
    }

    public List<ReportResponse> getModQueue(String subId, User moderator, int page, int size) {
        Subs sub = subsRepository.findById(subId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", subId));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can view the mod queue");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> reports = reportRepository.findBySubIdAndStatus(subId, Report.ReportStatus.PENDING, pageable);

        return reports.getContent().stream()
                .map(this::mapToReportResponseWithContext)
                .collect(Collectors.toList());
    }

    public List<ReportResponse> getAllReportsForSub(String subId, User moderator, int page, int size) {
        Subs sub = subsRepository.findById(subId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", subId));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can view reports");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> reports = reportRepository.findBySubId(subId, pageable);

        return reports.getContent().stream()
                .map(this::mapToReportResponseWithContext)
                .collect(Collectors.toList());
    }

    public long getPendingReportCount(String subId, User moderator) {
        Subs sub = subsRepository.findById(subId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", subId));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can view report counts");
        }

        return reportRepository.countBySubIdAndStatus(subId, Report.ReportStatus.PENDING);
    }

    public ReportResponse resolveReport(String reportId, Report.ReportStatus status, String modNote, User moderator) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));

        String subId = report.getSubId();
        Subs sub = subsRepository.findById(subId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", subId));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can resolve reports");
        }

        report.setStatus(status);
        report.setReviewedBy(moderator.getId());
        report.setReviewedByUsername(moderator.getUsername());
        report.setReviewedAt(Instant.now());
        report.setModNote(modNote);

        Report savedReport = reportRepository.save(report);
        return mapToReportResponse(savedReport);
    }

    // ==================== BAN METHODS ====================

    public BanResponse banUser(String subId, BanRequest request, User moderator) {
        Subs sub = subsRepository.findById(subId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", subId));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can ban users");
        }

        User targetUser = userService.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        // Can't ban moderators
        if (sub.getModeratorIds().contains(targetUser.getId())) {
            throw new BadRequestException("Cannot ban a moderator");
        }

        // Check if already banned
        if (banRepository.existsBySubIdAndUserId(subId, request.getUserId())) {
            throw new DuplicateResourceException("Ban", "user", request.getUserId());
        }

        Instant expiresAt = null;
        if (!request.isPermanent() && request.getDurationDays() != null) {
            expiresAt = Instant.now().plus(request.getDurationDays(), ChronoUnit.DAYS);
        }

        Ban ban = Ban.builder()
                .subId(subId)
                .subName(sub.getName())
                .userId(targetUser.getId())
                .username(targetUser.getUsername())
                .bannedById(moderator.getId())
                .bannedByUsername(moderator.getUsername())
                .reason(request.getReason())
                .permanent(request.isPermanent())
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .build();

        ban = banRepository.save(ban);

        // Also add to sub's banned list for quick lookup
        sub.getBannedUserIds().add(targetUser.getId());
        subsRepository.save(sub);

        return mapToBanResponse(ban);
    }

    public void unbanUser(String subId, String userId, User moderator) {
        Subs sub = subsRepository.findById(subId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", subId));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can unban users");
        }

        banRepository.deleteBySubIdAndUserId(subId, userId);

        // Remove from sub's banned list
        sub.getBannedUserIds().remove(userId);
        subsRepository.save(sub);
    }

    public List<BanResponse> getBannedUsers(String subId, User moderator, int page, int size) {
        Subs sub = subsRepository.findById(subId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", subId));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can view banned users");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Ban> bans = banRepository.findBySubId(subId, pageable);

        return bans.getContent().stream()
                .map(this::mapToBanResponse)
                .collect(Collectors.toList());
    }

    public boolean isUserBanned(String subId, String userId) {
        return banRepository.findBySubIdAndUserId(subId, userId)
                .map(Ban::isActive)
                .orElse(false);
    }

    // ==================== CONTENT REMOVAL METHODS ====================

    public void removePost(String postId, String reason, User moderator) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Subs sub = subsRepository.findById(post.getSubId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", post.getSubId()));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can remove posts");
        }

        post.setRemoved(true);
        post.setRemovedById(moderator.getId());
        post.setRemovedByUsername(moderator.getUsername());
        post.setRemovalReason(reason);
        postRepository.save(post);
    }

    public void approvePost(String postId, User moderator) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Subs sub = subsRepository.findById(post.getSubId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", post.getSubId()));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can approve posts");
        }

        post.setRemoved(false);
        post.setRemovedById(null);
        post.setRemovedByUsername(null);
        post.setRemovalReason(null);
        postRepository.save(post);
    }

    public void removeComment(String commentId, String reason, User moderator) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        Post post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", comment.getPostId()));

        Subs sub = subsRepository.findById(post.getSubId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", post.getSubId()));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can remove comments");
        }

        comment.setRemoved(true);
        comment.setRemovedById(moderator.getId());
        comment.setRemovedByUsername(moderator.getUsername());
        comment.setRemovalReason(reason);
        commentRepository.save(comment);
    }

    public void approveComment(String commentId, User moderator) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        Post post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", comment.getPostId()));

        Subs sub = subsRepository.findById(post.getSubId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", post.getSubId()));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can approve comments");
        }

        comment.setRemoved(false);
        comment.setRemovedById(null);
        comment.setRemovedByUsername(null);
        comment.setRemovalReason(null);
        commentRepository.save(comment);
    }

    // ==================== PIN METHODS ====================

    public void pinPost(String postId, User moderator) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Subs sub = subsRepository.findById(post.getSubId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", post.getSubId()));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can pin posts");
        }

        // Check how many posts are already pinned in this sub (limit to 2)
        // We'll need to add a query for this
        post.setPinned(true);
        post.setPinnedAt(Instant.now());
        post.setPinnedById(moderator.getId());
        postRepository.save(post);
    }

    public void unpinPost(String postId, User moderator) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Subs sub = subsRepository.findById(post.getSubId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", post.getSubId()));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can unpin posts");
        }

        post.setPinned(false);
        post.setPinnedAt(null);
        post.setPinnedById(null);
        postRepository.save(post);
    }

    public void lockPost(String postId, User moderator) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Subs sub = subsRepository.findById(post.getSubId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", post.getSubId()));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can lock posts");
        }

        post.setLocked(true);
        postRepository.save(post);
    }

    public void unlockPost(String postId, User moderator) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Subs sub = subsRepository.findById(post.getSubId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", post.getSubId()));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can unlock posts");
        }

        post.setLocked(false);
        postRepository.save(post);
    }

    // ==================== HELPER METHODS ====================

    private ReportResponse mapToReportResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reporterUsername(report.getReporterUsername())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .description(report.getDescription())
                .subId(report.getSubId())
                .subName(report.getSubName())
                .status(report.getStatus())
                .reviewedByUsername(report.getReviewedByUsername())
                .reviewedAt(report.getReviewedAt() != null ? formatDate(report.getReviewedAt()) : null)
                .modNote(report.getModNote())
                .createdAt(formatDate(report.getCreatedAt()))
                .build();
    }

    private ReportResponse mapToReportResponseWithContext(Report report) {
        ReportResponse response = mapToReportResponse(report);

        // Add context about the reported content
        if (report.getTargetType() == Report.TargetType.POST) {
            postRepository.findById(report.getTargetId()).ifPresent(post -> {
                response.setTargetTitle(post.getTitle());
                response.setTargetContent(post.getContent());
                response.setTargetAuthorUsername(post.getAuthorUsername());
            });
        } else if (report.getTargetType() == Report.TargetType.COMMENT) {
            commentRepository.findById(report.getTargetId()).ifPresent(comment -> {
                response.setTargetContent(comment.getContent());
                response.setTargetAuthorUsername(comment.getAuthorName());
            });
        }

        return response;
    }

    private BanResponse mapToBanResponse(Ban ban) {
        return BanResponse.builder()
                .id(ban.getId())
                .subId(ban.getSubId())
                .subName(ban.getSubName())
                .userId(ban.getUserId())
                .username(ban.getUsername())
                .bannedByUsername(ban.getBannedByUsername())
                .reason(ban.getReason())
                .permanent(ban.isPermanent())
                .expiresAt(ban.getExpiresAt() != null ? formatDate(ban.getExpiresAt()) : null)
                .createdAt(formatDate(ban.getCreatedAt()))
                .active(ban.isActive())
                .build();
    }

    private String formatDate(Instant instant) {
        return DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}
