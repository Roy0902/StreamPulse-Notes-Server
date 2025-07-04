package com.example.repository;

import com.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    User saveUser(User user);
    
    User findByEmail(String email);
    User findByUsername(String username);
    User findByProviderId(String providerId);
    User findByProvider(String provider);
} 