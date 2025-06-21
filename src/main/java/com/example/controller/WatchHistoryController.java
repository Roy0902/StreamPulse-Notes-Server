package com.example.controller;

import com.example.common.ApiResponse;
import com.example.dto.WatchHistoryDTO;
import com.example.service.WatchHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WatchHistoryController {
    private final WatchHistoryService historyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WatchHistoryDTO>>> getAllHistory() {
        List<WatchHistoryDTO> history = historyService.getAllHistory();
        return ResponseEntity.ok(ApiResponse.success("History retrieved successfully", history));
    }

    @GetMapping("/{historyId}")
    public ResponseEntity<ApiResponse<WatchHistoryDTO>> getHistoryById(@PathVariable String historyId) {
        try {
            WatchHistoryDTO history = historyService.getHistoryById(historyId);
            return ResponseEntity.ok(ApiResponse.success("History retrieved successfully", history));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WatchHistoryDTO>> createHistory(@Valid @RequestBody WatchHistoryDTO historyDTO) {
        WatchHistoryDTO createdHistory = historyService.createHistory(historyDTO);
        return ResponseEntity.ok(ApiResponse.success("History created successfully", createdHistory));
    }

    @PutMapping("/{historyId}")
    public ResponseEntity<ApiResponse<WatchHistoryDTO>> updateHistory(@PathVariable String historyId, @Valid @RequestBody WatchHistoryDTO historyDTO) {
        try {
            WatchHistoryDTO updatedHistory = historyService.updateHistory(historyId, historyDTO);
            return ResponseEntity.ok(ApiResponse.success("History updated successfully", updatedHistory));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{historyId}")
    public ResponseEntity<ApiResponse<Void>> deleteHistory(@PathVariable String historyId) {
        try {
            historyService.deleteHistory(historyId);
            return ResponseEntity.ok(ApiResponse.success("History deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<WatchHistoryDTO>>> getHistoryByUserId(@PathVariable String userId) {
        List<WatchHistoryDTO> history = historyService.getHistoryByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("User history retrieved successfully", history));
    }

    @PutMapping("/user/{userId}/video/{videoId}/progress")
    public ResponseEntity<ApiResponse<WatchHistoryDTO>> updateProgress(
            @PathVariable String userId,
            @PathVariable String videoId,
            @RequestParam Integer progress) {
        try {
            WatchHistoryDTO history = historyService.updateProgress(userId, videoId, progress);
            return ResponseEntity.ok(ApiResponse.success("Progress updated successfully", history));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/user/{userId}/video/{videoId}/complete")
    public ResponseEntity<ApiResponse<WatchHistoryDTO>> markAsCompleted(
            @PathVariable String userId,
            @PathVariable String videoId) {
        try {
            WatchHistoryDTO history = historyService.markAsCompleted(userId, videoId);
            return ResponseEntity.ok(ApiResponse.success("Video marked as completed", history));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
} 