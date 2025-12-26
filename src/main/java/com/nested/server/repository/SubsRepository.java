package com.nested.server.repository;

import com.nested.server.model.Subs;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubsRepository extends MongoRepository<Subs, String> {

    Optional<Subs> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Subs> findByNameContainingIgnoreCase(String query);

    List<Subs> findTop10ByOrderBySubscriberCountDesc();

    List<Subs> findByIdIn(List<String> ids);

    // Find subs where user is a moderator
    List<Subs> findByModeratorIdsContaining(String moderatorId);

    // Find subs created by user
    List<Subs> findByCreatorId(String creatorId);

    /**
     * Atomic subscribe - add user to subscriberIds and increment count
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$addToSet': { 'subscriberIds': ?1 }, '$inc': { 'subscriberCount': 1 } }")
    void addSubscriber(String subsId, String userId);

    /**
     * Atomic unsubscribe - remove user from subscriberIds and decrement count
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$pull': { 'subscriberIds': ?1 }, '$inc': { 'subscriberCount': -1 } }")
    void removeSubscriber(String subsId, String userId);

    /**
     * Check if user is subscribed (efficient existence check)
     */
    @Query(value = "{ '_id': ?0, 'subscriberIds': ?1 }", exists = true)
    boolean isUserSubscribed(String subsId, String userId);

    /**
     * Atomic add moderator
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$addToSet': { 'moderatorIds': ?1 } }")
    void addModerator(String subsId, String userId);

    /**
     * Atomic remove moderator
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$pull': { 'moderatorIds': ?1 } }")
    void removeModerator(String subsId, String userId);

    /**
     * Find communities where user is NOT a subscriber, moderator, or creator
     */
    @Query("{ 'subscriberIds': { '$ne': ?0 }, 'moderatorIds': { '$ne': ?0 }, 'creatorId': { '$ne': ?0 } }")
    List<Subs> findCommunitiesUserNotPartOf(String userId);

    /**
     * Find all communities ordered by subscriber count
     */
    List<Subs> findAllByOrderBySubscriberCountDesc();
}
