package com.example.repository;

import com.example.entity.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, String> {
    List<WatchHistory> findByUserId(String userId);
    List<WatchHistory> findByUserIdOrderByLastWatchedAtDesc(String userId);
    Optional<WatchHistory> findByUserIdAndVideoId(String userId, String videoId);
} 