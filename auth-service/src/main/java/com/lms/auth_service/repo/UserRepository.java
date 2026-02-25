//Repository = “How your system talks to the database”

package com.lms.auth_service.repo;

import com.lms.auth_service.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;


public interface UserRepository extends MongoRepository<User,String> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
}
