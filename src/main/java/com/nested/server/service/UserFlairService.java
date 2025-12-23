package com.nested.server.service;

import com.nested.server.dto.UserFlairRequest;
import com.nested.server.dto.UserFlairResponse;
import com.nested.server.exception.ResourceNotFoundException;
import com.nested.server.exception.UnauthorizedException;
import com.nested.server.model.Subs;
import com.nested.server.model.User;
import com.nested.server.model.UserFlair;
import com.nested.server.repository.SubsRepository;
import com.nested.server.repository.UserFlairRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFlairService {

    private final UserFlairRepository userFlairRepository;
    private final SubsRepository subsRepository;
    private final UserService userService;

    public Optional<UserFlairResponse> getUserFlair(String subId, String userId) {
        return userFlairRepository.findBySubIdAndUserId(subId, userId)
                .map(this::mapToResponse);
    }

    public List<UserFlairResponse> getAllFlairsForSub(String subId) {
        return userFlairRepository.findBySubId(subId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserFlairResponse setOwnFlair(String subId, UserFlairRequest request, User user) {
        Subs sub = subsRepository.findById(subId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", subId));

        // Check if user already has a flair
        Optional<UserFlair> existingFlair = userFlairRepository.findBySubIdAndUserId(subId, user.getId());

        UserFlair flair;
        if (existingFlair.isPresent()) {
            flair = existingFlair.get();
            // Check if user is allowed to edit their flair
            if (!flair.isUserEditable() && !sub.getModeratorIds().contains(user.getId())) {
                throw new UnauthorizedException("This flair can only be changed by moderators");
            }
            flair.setText(request.getText());
            if (request.getTextColor() != null) {
                flair.setTextColor(request.getTextColor());
            }
            if (request.getBackgroundColor() != null) {
                flair.setBackgroundColor(request.getBackgroundColor());
            }
            flair.setUpdatedAt(Instant.now());
        } else {
            flair = UserFlair.builder()
                    .subId(subId)
                    .userId(user.getId())
                    .text(request.getText())
                    .textColor(request.getTextColor() != null ? request.getTextColor() : "#000000")
                    .backgroundColor(request.getBackgroundColor() != null ? request.getBackgroundColor() : "#EDEFF1")
                    .assignedById(user.getId())
                    .assignedByUsername(user.getUsername())
                    .userEditable(true)
                    .createdAt(Instant.now())
                    .build();
        }

        flair = userFlairRepository.save(flair);
        return mapToResponse(flair);
    }

    public UserFlairResponse setUserFlair(String subId, String targetUserId, UserFlairRequest request, User moderator) {
        Subs sub = subsRepository.findById(subId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", subId));

        if (!sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("Only moderators can set user flairs");
        }

        User targetUser = userService.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        Optional<UserFlair> existingFlair = userFlairRepository.findBySubIdAndUserId(subId, targetUserId);

        UserFlair flair;
        if (existingFlair.isPresent()) {
            flair = existingFlair.get();
            flair.setText(request.getText());
            if (request.getTextColor() != null) {
                flair.setTextColor(request.getTextColor());
            }
            if (request.getBackgroundColor() != null) {
                flair.setBackgroundColor(request.getBackgroundColor());
            }
            flair.setUserEditable(request.isUserEditable());
            flair.setAssignedById(moderator.getId());
            flair.setAssignedByUsername(moderator.getUsername());
            flair.setUpdatedAt(Instant.now());
        } else {
            flair = UserFlair.builder()
                    .subId(subId)
                    .userId(targetUserId)
                    .text(request.getText())
                    .textColor(request.getTextColor() != null ? request.getTextColor() : "#000000")
                    .backgroundColor(request.getBackgroundColor() != null ? request.getBackgroundColor() : "#EDEFF1")
                    .assignedById(moderator.getId())
                    .assignedByUsername(moderator.getUsername())
                    .userEditable(request.isUserEditable())
                    .createdAt(Instant.now())
                    .build();
        }

        flair = userFlairRepository.save(flair);
        return mapToResponse(flair);
    }

    public void removeUserFlair(String subId, String targetUserId, User moderator) {
        Subs sub = subsRepository.findById(subId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "id", subId));

        // Allow users to remove their own flair, or mods to remove any flair
        if (!targetUserId.equals(moderator.getId()) && !sub.getModeratorIds().contains(moderator.getId())) {
            throw new UnauthorizedException("You can only remove your own flair or be a moderator");
        }

        userFlairRepository.deleteBySubIdAndUserId(subId, targetUserId);
    }

    private UserFlairResponse mapToResponse(UserFlair flair) {
        // Get username for the flair
        String username = userService.findById(flair.getUserId())
                .map(User::getUsername)
                .orElse("unknown");

        return UserFlairResponse.builder()
                .id(flair.getId())
                .subId(flair.getSubId())
                .userId(flair.getUserId())
                .username(username)
                .text(flair.getText())
                .textColor(flair.getTextColor())
                .backgroundColor(flair.getBackgroundColor())
                .assignedByUsername(flair.getAssignedByUsername())
                .userEditable(flair.isUserEditable())
                .createdAt(formatDate(flair.getCreatedAt()))
                .build();
    }

    private String formatDate(Instant instant) {
        return DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}
