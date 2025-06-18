package com.example.controller;

import com.example.common.ApiResponse;
import com.example.common.MessageConstants;
import com.example.dto.VideoDTO;
import com.example.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VideoController {
    private final VideoService videoService;

    @GetMapping
    public ApiResponse<List<VideoDTO>> getAllVideos() {
        try {
            List<VideoDTO> videos = videoService.getAllVideos();
            return ApiResponse.withData(200, MessageConstants.SUCCESS, videos);
        } catch (Exception e) {
            return ApiResponse.withoutData(500, String.format(MessageConstants.VIDEO_RETRIEVE_ERROR, e.getMessage()));
        }
    }

    @GetMapping("/{videoId}")
    public ApiResponse<VideoDTO> getVideoById(@PathVariable Long videoId) {
        try {
            VideoDTO video = videoService.getVideoById(videoId);
            return ApiResponse.withData(200, MessageConstants.SUCCESS, video);
        } catch (RuntimeException e) {
            return ApiResponse.withoutData(404, String.format(MessageConstants.VIDEO_NOT_FOUND, videoId));
        }
    }

    @GetMapping("/youtube/{youtubeId}")
    public ApiResponse<VideoDTO> getVideoByYoutubeId(@PathVariable String youtubeId) {
        try {
            VideoDTO video = videoService.getVideoByYoutubeId(youtubeId);
            return ApiResponse.withData(200, MessageConstants.SUCCESS, video);
        } catch (RuntimeException e) {
            return ApiResponse.withoutData(404, String.format(MessageConstants.VIDEO_YOUTUBE_NOT_FOUND, youtubeId));
        }
    }

    @PostMapping
    public ApiResponse<VideoDTO> createVideo(@RequestBody VideoDTO videoDTO) {
        try {
            VideoDTO createdVideo = videoService.createVideo(videoDTO);
            return ApiResponse.withData(201, MessageConstants.VIDEO_CREATED, createdVideo);
        } catch (Exception e) {
            return ApiResponse.withoutData(400, String.format(MessageConstants.VIDEO_CREATE_ERROR, e.getMessage()));
        }
    }

    @PutMapping("/{videoId}")
    public ApiResponse<VideoDTO> updateVideo(@PathVariable Long videoId, @RequestBody VideoDTO videoDTO) {
        try {
            VideoDTO updatedVideo = videoService.updateVideo(videoId, videoDTO);
            return ApiResponse.withData(200, MessageConstants.VIDEO_UPDATED, updatedVideo);
        } catch (RuntimeException e) {
            return ApiResponse.withoutData(404, String.format(MessageConstants.VIDEO_NOT_FOUND, videoId));
        }
    }

    @DeleteMapping("/{videoId}")
    public ApiResponse<Void> deleteVideo(@PathVariable Long videoId) {
        try {
            videoService.deleteVideo(videoId);
            return ApiResponse.withoutData(200, MessageConstants.VIDEO_DELETED);
        } catch (RuntimeException e) {
            return ApiResponse.withoutData(404, String.format(MessageConstants.VIDEO_NOT_FOUND, videoId));
        }
    }

    @GetMapping("/channel/{channelId}")
    public ApiResponse<List<VideoDTO>> getVideosByChannel(@PathVariable String channelId) {
        try {
            List<VideoDTO> videos = videoService.getVideosByChannel(channelId);
            return ApiResponse.withData(200, MessageConstants.SUCCESS, videos);
        } catch (Exception e) {
            return ApiResponse.withoutData(500, String.format(MessageConstants.VIDEO_CHANNEL_RETRIEVE_ERROR, e.getMessage()));
        }
    }
} 