package com.cloud.repository;

import com.cloud.document.UserCredits;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserCreditsRepository extends MongoRepository<UserCredits,String> {
    UserCredits findByClerkId(String clerkId);
}
