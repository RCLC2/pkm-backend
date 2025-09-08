package com.ns.user.user.repository;

import com.ns.user.user.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<UserEntity,String> {
    // id == Google sub
}
