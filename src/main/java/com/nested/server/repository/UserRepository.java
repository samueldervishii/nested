package com.nested.server.repository;

import com.nested.server.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Atomic karma increment using MongoDB's $inc operator
     * This is more efficient than read-modify-write pattern
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'karma': ?1 } }")
    void incrementKarma(String userId, int delta);

    /**
     * Add subscription atomically using $addToSet (prevents duplicates)
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$addToSet': { 'subscribedSubs': ?1 } }")
    void addSubscription(String userId, String subsId);

    /**
     * Remove subscription atomically using $pull
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$pull': { 'subscribedSubs': ?1 } }")
    void removeSubscription(String userId, String subsId);
}