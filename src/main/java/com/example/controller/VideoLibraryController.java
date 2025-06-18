package com.example.controller;

import com.example.dto.VideoLibraryDTO;
import com.example.service.VideoLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VideoLibraryController {
    private final VideoLibraryService videoLibraryService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<VideoLibraryDTO>> getUserLibrary(@PathVariable String userId) {
        try {
            List<VideoLibraryDTO> library = videoLibraryService.getUserLibrary(userId);
            return ResponseEntity.ok(library);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/user/{userId}/video/{videoId}")
    public ResponseEntity<VideoLibraryDTO> addVideoToLibrary(
            @PathVariable String userId,
            @PathVariable Long videoId) {
        try {
            VideoLibraryDTO addedVideo = videoLibraryService.addVideoToLibrary(userId, videoId);
            return ResponseEntity.status(HttpStatus.CREATED).body(addedVideo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{libraryId}/status")
    public ResponseEntity<VideoLibraryDTO> updateVideoStatus(
            @PathVariable Long libraryId,
            @RequestParam String status) {
        try {
            VideoLibraryDTO updatedLibrary = videoLibraryService.updateVideoStatus(libraryId, status);
            return ResponseEntity.ok(updatedLibrary);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{libraryId}/progress")
    public ResponseEntity<VideoLibraryDTO> updateVideoProgress(
            @PathVariable Long libraryId,
            @RequestParam Integer progress) {
        try {
            VideoLibraryDTO updatedLibrary = videoLibraryService.updateVideoProgress(libraryId, progress);
            return ResponseEntity.ok(updatedLibrary);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{libraryId}")
    public ResponseEntity<Void> removeVideoFromLibrary(@PathVariable Long libraryId) {
        try {
            videoLibraryService.removeVideoFromLibrary(libraryId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<VideoLibraryDTO>> getVideosByStatus(
            @PathVariable String userId,
            @PathVariable String status) {
        try {
            List<VideoLibraryDTO> videos = videoLibraryService.getVideosByStatus(userId, status);
            return ResponseEntity.ok(videos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 