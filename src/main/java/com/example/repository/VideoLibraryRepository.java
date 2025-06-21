package com.example.repository;

import com.example.entity.VideoLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoLibraryRepository extends JpaRepository<VideoLibrary, String> {
    List<VideoLibrary> findByUserId(String userId);
    List<VideoLibrary> findByIsPublicTrue();
    List<VideoLibrary> findByUserIdAndIsPublic(String userId, Boolean isPublic);
} 