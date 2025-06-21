package com.example.service;

import com.example.dto.VideoDTO;

import java.util.List;

public interface VideoService {
    List<VideoDTO> getAllVideos();
    VideoDTO getVideoById(String videoId);
    VideoDTO createVideo(VideoDTO videoDTO);
    VideoDTO updateVideo(String videoId, VideoDTO videoDTO);
    void deleteVideo(String videoId);
    List<VideoDTO> getVideosByUserId(String userId);
    List<VideoDTO> searchVideosByTitle(String title);
} 