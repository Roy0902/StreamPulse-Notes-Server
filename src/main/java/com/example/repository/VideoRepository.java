package com.example.repository;

import com.example.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, String> {
    List<Video> findByUserId(String userId);
    List<Video> findByTitleContainingIgnoreCase(String title);
} 