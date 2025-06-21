package com.example.controller;

import com.example.common.ApiResponse;
import com.example.dto.VideoDTO;
import com.example.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VideoController {
    private final VideoService videoService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VideoDTO>>> getAllVideos() {
        List<VideoDTO> videos = videoService.getAllVideos();
        return ResponseEntity.ok(ApiResponse.success("Videos retrieved successfully", videos));
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<ApiResponse<VideoDTO>> getVideoById(@PathVariable String videoId) {
        try {
            VideoDTO video = videoService.getVideoById(videoId);
            return ResponseEntity.ok(ApiResponse.success("Video retrieved successfully", video));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VideoDTO>> createVideo(@Valid @RequestBody VideoDTO videoDTO) {
        VideoDTO createdVideo = videoService.createVideo(videoDTO);
        return ResponseEntity.ok(ApiResponse.success("Video created successfully", createdVideo));
    }

    @PutMapping("/{videoId}")
    public ResponseEntity<ApiResponse<VideoDTO>> updateVideo(@PathVariable String videoId, @Valid @RequestBody VideoDTO videoDTO) {
        try {
            VideoDTO updatedVideo = videoService.updateVideo(videoId, videoDTO);
            return ResponseEntity.ok(ApiResponse.success("Video updated successfully", updatedVideo));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(@PathVariable String videoId) {
        try {
            videoService.deleteVideo(videoId);
            return ResponseEntity.ok(ApiResponse.success("Video deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<VideoDTO>>> getVideosByUserId(@PathVariable String userId) {
        List<VideoDTO> videos = videoService.getVideosByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("User videos retrieved successfully", videos));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<VideoDTO>>> searchVideosByTitle(@RequestParam String title) {
        List<VideoDTO> videos = videoService.searchVideosByTitle(title);
        return ResponseEntity.ok(ApiResponse.success("Videos found successfully", videos));
    }
} 