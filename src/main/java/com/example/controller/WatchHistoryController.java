package com.example.controller;

import com.example.dto.WatchHistoryDTO;
import com.example.service.WatchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WatchHistoryController {
    private final WatchHistoryService watchHistoryService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WatchHistoryDTO>> getUserHistory(@PathVariable String userId) {
        try {
            List<WatchHistoryDTO> history = watchHistoryService.getUserHistory(userId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/user/{userId}/video/{videoId}")
    public ResponseEntity<WatchHistoryDTO> addToHistory(
            @PathVariable String userId,
            @PathVariable Long videoId) {
        try {
            WatchHistoryDTO history = watchHistoryService.addToHistory(userId, videoId);
            return ResponseEntity.status(HttpStatus.CREATED).body(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{historyId}")
    public ResponseEntity<Void> removeFromHistory(@PathVariable Long historyId) {
        try {
            watchHistoryService.removeFromHistory(historyId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> clearUserHistory(@PathVariable String userId) {
        try {
            watchHistoryService.clearUserHistory(userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<List<WatchHistoryDTO>> getRecentHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<WatchHistoryDTO> history = watchHistoryService.getRecentHistory(userId, limit);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 