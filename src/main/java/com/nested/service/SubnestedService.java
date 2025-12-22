package com.nested.service;

import com.nested.dto.SubnestedRequest;
import com.nested.dto.SubnestedResponse;
import com.nested.exception.BadRequestException;
import com.nested.exception.DuplicateResourceException;
import com.nested.exception.ResourceNotFoundException;
import com.nested.exception.UnauthorizedException;
import com.nested.model.Subnested;
import com.nested.model.User;
import com.nested.repository.SubnestedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubnestedService {

    private final SubnestedRepository subnestedRepository;
    private final UserService userService;

    @CacheEvict(value = "popularsubnesteds", allEntries = true)
    public SubnestedResponse createSubnested(SubnestedRequest request, User creator) {
        if (subnestedRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("subnested", "name", request.getName());
        }

        Subnested subnested = Subnested.builder()
                .name(request.getName())
                .description(request.getDescription())
                .creatorId(creator.getId())
                .creatorUsername(creator.getUsername())
                .createdAt(Instant.now())
                .build();

        // Creator is automatically a moderator and subscriber
        subnested.getModeratorIds().add(creator.getId());
        subnested.getSubscriberIds().add(creator.getId());
        subnested = subnestedRepository.save(subnested);

        // Also update user's subscribed list
        userService.subscribeToSubnested(creator.getId(), subnested.getId());

        return mapToResponse(subnested, true);
    }

    public Optional<Subnested> findByName(String name) {
        return subnestedRepository.findByNameIgnoreCase(name);
    }

    public Optional<Subnested> findById(String id) {
        return subnestedRepository.findById(id);
    }

    public List<SubnestedResponse> getPopularSubnested(User currentUser) {
        List<Subnested> subnesteds = getCachedPopularsubnesteds();
        return subnesteds.stream()
                .map(sub -> mapToResponse(sub, isSubscribed(sub, currentUser)))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "popularsubnesteds", key = "'all'")
    public List<Subnested> getCachedPopularsubnesteds() {
        return subnestedRepository.findTop10ByOrderBySubscriberCountDesc();
    }

    public List<SubnestedResponse> searchSubnested(String query, User currentUser) {
        return subnestedRepository.findByNameContainingIgnoreCase(query).stream()
                .map(sub -> mapToResponse(sub, isSubscribed(sub, currentUser)))
                .collect(Collectors.toList());
    }

    public List<SubnestedResponse> getUserSubscriptions(User user) {
        return subnestedRepository.findByIdIn(user.getSubscribedSubnested()).stream()
                .map(sub -> mapToResponse(sub, true))
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "popularsubnesteds", allEntries = true)
    public void subscribe(String subnestedId, User user) {
        subnestedRepository.findById(subnestedId).ifPresent(subnested -> {
            // Add user to subscriberIds set (prevents duplicates automatically)
            if (subnested.getSubscriberIds().add(user.getId())) {
                subnestedRepository.save(subnested);
                userService.subscribeToSubnested(user.getId(), subnestedId);
            }
        });
    }

    @CacheEvict(value = "popularsubnesteds", allEntries = true)
    public void unsubscribe(String subnestedId, User user) {
        subnestedRepository.findById(subnestedId).ifPresent(subnested -> {
            // Remove user from subscriberIds set
            if (subnested.getSubscriberIds().remove(user.getId())) {
                subnestedRepository.save(subnested);
                userService.unsubscribeFromSubnested(user.getId(), subnestedId);
            }
        });
    }

    private boolean isSubscribed(Subnested subnested, User user) {
        if (user == null) return false;
        // Check directly from subnested's subscriberIds (source of truth)
        return subnested.getSubscriberIds().contains(user.getId());
    }

    private boolean isModerator(Subnested subnested, User user) {
        if (user == null) return false;
        return subnested.getModeratorIds().contains(user.getId());
    }

    public SubnestedResponse mapToResponse(Subnested subnested, boolean isSubscribed) {
        return mapToResponse(subnested, isSubscribed, null);
    }

    public SubnestedResponse mapToResponse(Subnested subnested, boolean isSubscribed, User currentUser) {
        return SubnestedResponse.builder()
                .id(subnested.getId())
                .name(subnested.getName())
                .description(subnested.getDescription())
                .bannerUrl(subnested.getBannerUrl())
                .iconUrl(subnested.getIconUrl())
                .creatorUsername(subnested.getCreatorUsername())
                .subscriberCount(subnested.getSubscriberCount())
                .createdAt(formatDate(subnested.getCreatedAt()))
                .isSubscribed(isSubscribed)
                .rules(subnested.getRules())
                .flairs(subnested.getFlairs())
                .moderatorIds(subnested.getModeratorIds())
                .isModerator(isModerator(subnested, currentUser))
                .build();
    }

    public void updateSubnested(String subnestedId, SubnestedRequest request, User user) {
        Subnested subnested = subnestedRepository.findById(subnestedId)
                .orElseThrow(() -> new ResourceNotFoundException("Subnested", "id", subnestedId));

        if (!subnested.getModeratorIds().contains(user.getId())) {
            throw new UnauthorizedException("Only moderators can edit subnested settings");
        }

        if (request.getDescription() != null) {
            subnested.setDescription(request.getDescription());
        }
        if (request.getBannerUrl() != null) {
            subnested.setBannerUrl(request.getBannerUrl());
        }
        if (request.getIconUrl() != null) {
            subnested.setIconUrl(request.getIconUrl());
        }

        subnestedRepository.save(subnested);
    }

    public void updateRules(String subnestedId, List<String> rules, User user) {
        Subnested subnested = subnestedRepository.findById(subnestedId)
                .orElseThrow(() -> new ResourceNotFoundException("Subnested", "id", subnestedId));

        if (!subnested.getModeratorIds().contains(user.getId())) {
            throw new UnauthorizedException("Only moderators can update rules");
        }

        subnested.setRules(rules);
        subnestedRepository.save(subnested);
    }

    public void addFlair(String subnestedId, Subnested.Flair flair, User user) {
        Subnested subnested = subnestedRepository.findById(subnestedId)
                .orElseThrow(() -> new ResourceNotFoundException("Subnested", "id", subnestedId));

        if (!subnested.getModeratorIds().contains(user.getId())) {
            throw new UnauthorizedException("Only moderators can add flairs");
        }

        subnested.getFlairs().add(flair);
        subnestedRepository.save(subnested);
    }

    public void removeFlair(String subnestedId, String flairName, User user) {
        Subnested subnested = subnestedRepository.findById(subnestedId)
                .orElseThrow(() -> new ResourceNotFoundException("Subnested", "id", subnestedId));

        if (!subnested.getModeratorIds().contains(user.getId())) {
            throw new UnauthorizedException("Only moderators can remove flairs");
        }

        subnested.getFlairs().removeIf(f -> f.getName().equals(flairName));
        subnestedRepository.save(subnested);
    }

    public void addModerator(String subnestedId, String newModeratorId, User user) {
        Subnested subnested = subnestedRepository.findById(subnestedId)
                .orElseThrow(() -> new ResourceNotFoundException("Subnested", "id", subnestedId));

        if (!subnested.getCreatorId().equals(user.getId())) {
            throw new UnauthorizedException("Only the creator can add moderators");
        }

        if (!subnested.getModeratorIds().contains(newModeratorId)) {
            subnested.getModeratorIds().add(newModeratorId);
            subnestedRepository.save(subnested);
        }
    }

    public void removeModerator(String subnestedId, String moderatorId, User user) {
        Subnested subnested = subnestedRepository.findById(subnestedId)
                .orElseThrow(() -> new ResourceNotFoundException("Subnested", "id", subnestedId));

        if (!subnested.getCreatorId().equals(user.getId())) {
            throw new UnauthorizedException("Only the creator can remove moderators");
        }

        if (moderatorId.equals(subnested.getCreatorId())) {
            throw new BadRequestException("Cannot remove the creator from moderators");
        }

        subnested.getModeratorIds().remove(moderatorId);
        subnestedRepository.save(subnested);
    }

    private String formatDate(Instant instant) {
        return DateTimeFormatter.ofPattern("MMM dd, yyyy")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    /**
     * Migration method: Add all existing users to a subnested's subscriberIds.
     * This is a one-time fix to populate subscriberIds for existing data.
     */
    @CacheEvict(value = "popularsubnesteds", allEntries = true)
    public int migrateAllUsersToSubnested(String subnestedName) {
        Subnested subnested = subnestedRepository.findByNameIgnoreCase(subnestedName)
                .orElseThrow(() -> new ResourceNotFoundException("Subnested", "name", subnestedName));

        List<User> allUsers = userService.findAllUsers();
        int count = 0;

        for (User user : allUsers) {
            if (subnested.getSubscriberIds().add(user.getId())) {
                count++;
            }
        }

        subnestedRepository.save(subnested);
        return count;
    }
}
