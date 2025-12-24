package com.nested.server.service;

import com.nested.server.dto.SubRequest;
import com.nested.server.dto.SubResponse;
import com.nested.server.exception.BadRequestException;
import com.nested.server.exception.DuplicateResourceException;
import com.nested.server.exception.ResourceNotFoundException;
import com.nested.server.exception.UnauthorizedException;
import com.nested.server.model.Subs;
import com.nested.server.model.User;
import com.nested.server.repository.SubsRepository;
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
public class SubService {

    private final SubsRepository subsRepository;
    private final UserService userService;

    public SubResponse createSubs(SubRequest request, User creator) {
        if (subsRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Subs", "name", request.getName());
        }

        Subs subs = Subs.builder()
                .name(request.getName())
                .description(request.getDescription())
                .creatorId(creator.getId())
                .creatorUsername(creator.getUsername())
                .createdAt(Instant.now())
                .build();

        // Creator is automatically a moderator and subscriber
        subs.getModeratorIds().add(creator.getId());
        subs.getSubscriberIds().add(creator.getId());
        subs = subsRepository.save(subs);

        // Also update user's subscribed list
        userService.subscribeToSubs(creator.getId(), subs.getId());

        return mapToResponse(subs, true, creator);
    }

    public Optional<Subs> findByName(String name) {
        return subsRepository.findByNameIgnoreCase(name);
    }

    public Optional<Subs> findById(String id) {
        return subsRepository.findById(id);
    }

    public List<SubResponse> getPopularSubs(User currentUser) {
        List<Subs> subsList = subsRepository.findTop10ByOrderBySubscriberCountDesc();
        return subsList.stream()
                .map(sub -> mapToResponse(sub, isSubscribed(sub, currentUser), currentUser))
                .collect(Collectors.toList());
    }

    public List<SubResponse> searchSubs(String query, User currentUser) {
        return subsRepository.findByNameContainingIgnoreCase(query).stream()
                .map(sub -> mapToResponse(sub, isSubscribed(sub, currentUser), currentUser))
                .collect(Collectors.toList());
    }

    public List<SubResponse> getUserSubscriptions(User user) {
        return subsRepository.findByIdIn(user.getSubscribedSubs()).stream()
                .map(sub -> mapToResponse(sub, true, user))
                .collect(Collectors.toList());
    }

    /**
     * Atomic subscribe - no race conditions
     */
    public void subscribe(String subsId, User user) {
        // Check if already subscribed to avoid unnecessary operations
        if (!subsRepository.isUserSubscribed(subsId, user.getId())) {
            subsRepository.addSubscriber(subsId, user.getId());
            userService.subscribeToSubs(user.getId(), subsId);
        }
    }

    /**
     * Atomic unsubscribe - no race conditions
     */
    public void unsubscribe(String subsId, User user) {
        // Check if subscribed before removing
        if (subsRepository.isUserSubscribed(subsId, user.getId())) {
            subsRepository.removeSubscriber(subsId, user.getId());
            userService.unsubscribeFromSubs(user.getId(), subsId);
        }
    }

    private boolean isSubscribed(Subs sub, User user) {
        if (user == null) return false;
        return sub.getSubscriberIds().contains(user.getId());
    }

    private boolean isModerator(Subs sub, User user) {
        if (user == null) return false;
        return sub.getModeratorIds().contains(user.getId());
    }

    public SubResponse mapToResponse(Subs sub, boolean isSubscribed, User currentUser) {
        return SubResponse.builder()
                .id(sub.getId())
                .name(sub.getName())
                .description(sub.getDescription())
                .bannerUrl(sub.getBannerUrl())
                .iconUrl(sub.getIconUrl())
                .creatorUsername(sub.getCreatorUsername())
                .subscriberCount(sub.getSubscriberCount())
                .createdAt(formatDate(sub.getCreatedAt()))
                .isSubscribed(isSubscribed)
                .rules(sub.getRules())
                .flairs(sub.getFlairs())
                .moderatorIds(sub.getModeratorIds())
                .isModerator(isModerator(sub, currentUser))
                .build();
    }

    public void updateSubs(String subsId, SubRequest request, User user) {
        Subs sub = subsRepository.findById(subsId)
                .orElseThrow(() -> new ResourceNotFoundException("Subs", "id", subsId));

        if (!sub.getModeratorIds().contains(user.getId())) {
            throw new UnauthorizedException("Only moderators can edit Subs settings");
        }

        if (request.getDescription() != null) {
            sub.setDescription(request.getDescription());
        }
        if (request.getBannerUrl() != null) {
            sub.setBannerUrl(request.getBannerUrl());
        }
        if (request.getIconUrl() != null) {
            sub.setIconUrl(request.getIconUrl());
        }

        subsRepository.save(sub);
    }

    public void updateRules(String subsId, List<String> rules, User user) {
        Subs sub = subsRepository.findById(subsId)
                .orElseThrow(() -> new ResourceNotFoundException("Subs", "id", subsId));

        if (!sub.getModeratorIds().contains(user.getId())) {
            throw new UnauthorizedException("Only moderators can update rules");
        }

        sub.setRules(rules);
        subsRepository.save(sub);
    }

    public void addFlair(String subsId, Subs.Flair flair, User user) {
        Subs sub = subsRepository.findById(subsId)
                .orElseThrow(() -> new ResourceNotFoundException("Subs", "id", subsId));

        if (!sub.getModeratorIds().contains(user.getId())) {
            throw new UnauthorizedException("Only moderators can add flairs");
        }

        sub.getFlairs().add(flair);
        subsRepository.save(sub);
    }

    public void removeFlair(String subsId, String flairName, User user) {
        Subs sub = subsRepository.findById(subsId)
                .orElseThrow(() -> new ResourceNotFoundException("Subs", "id", subsId));

        if (!sub.getModeratorIds().contains(user.getId())) {
            throw new UnauthorizedException("Only moderators can remove flairs");
        }

        sub.getFlairs().removeIf(f -> f.getName().equals(flairName));
        subsRepository.save(sub);
    }

    /**
     * Atomic add moderator
     */
    public void addModerator(String subsId, String newModeratorId, User user) {
        Subs sub = subsRepository.findById(subsId)
                .orElseThrow(() -> new ResourceNotFoundException("Subs", "id", subsId));

        if (!sub.getCreatorId().equals(user.getId())) {
            throw new UnauthorizedException("Only the creator can add moderators");
        }

        subsRepository.addModerator(subsId, newModeratorId);
    }

    /**
     * Atomic remove moderator
     */
    public void removeModerator(String subsId, String moderatorId, User user) {
        Subs sub = subsRepository.findById(subsId)
                .orElseThrow(() -> new ResourceNotFoundException("Subs", "id", subsId));

        if (!sub.getCreatorId().equals(user.getId())) {
            throw new UnauthorizedException("Only the creator can remove moderators");
        }

        if (moderatorId.equals(sub.getCreatorId())) {
            throw new BadRequestException("Cannot remove the creator from moderators");
        }

        subsRepository.removeModerator(subsId, moderatorId);
    }

    private String formatDate(Instant instant) {
        return DateTimeFormatter.ofPattern("MMM dd, yyyy")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    /**
     * Migration method: Add all existing users to a Subs's subscriberIds.
     * This is a one-time fix to populate subscriberIds for existing data.
     */
    public List<SubResponse> getModeratedSubs(String userId) {
        List<Subs> subs = subsRepository.findByModeratorIdsContaining(userId);
        return subs.stream()
                .map(sub -> mapToResponse(sub, false, null))
                .collect(Collectors.toList());
    }

    public List<SubResponse> getCreatedSubs(String userId) {
        List<Subs> subs = subsRepository.findByCreatorId(userId);
        return subs.stream()
                .map(sub -> mapToResponse(sub, false, null))
                .collect(Collectors.toList());
    }

    public int migrateAllUsersToSubs(String subsName) {
        Subs sub = subsRepository.findByNameIgnoreCase(subsName)
                .orElseThrow(() -> new ResourceNotFoundException("Subs", "name", subsName));

        List<User> allUsers = userService.findAllUsers();
        int count = 0;

        for (User user : allUsers) {
            if (sub.getSubscriberIds().add(user.getId())) {
                count++;
            }
        }

        sub.setSubscriberCount(sub.getSubscriberIds().size());
        subsRepository.save(sub);
        return count;
    }
}
