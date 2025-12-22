package com.nested.repository;

import com.nested.model.Subnested;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubnestedRepository extends MongoRepository<Subnested, String> {

    Optional<Subnested> findByName(String name);

    Optional<Subnested> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Subnested> findByNameContainingIgnoreCase(String query);

    List<Subnested> findTop10ByOrderBySubscriberCountDesc();

    List<Subnested> findByIdIn(List<String> ids);
}
