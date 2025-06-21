package com.example.service.impl;

import com.example.dto.VideoDTO;
import com.example.entity.User;
import com.example.entity.Video;
import com.example.repository.UserRepository;
import com.example.repository.VideoRepository;
import com.example.service.VideoService;
import com.github.rholder.fauxflake.IdGenerators;
import com.github.rholder.fauxflake.api.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final IdGenerator snowflake = IdGenerators.newSnowflakeIdGenerator();

    @Override
    public List<VideoDTO> getAllVideos() {
        return videoRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public VideoDTO getVideoById(String videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        return convertToDTO(video);
    }

    @Override
    @Transactional
    public VideoDTO createVideo(VideoDTO videoDTO) {
        User user = userRepository.findById(videoDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Video video = new Video();
        try {
            video.setVideoId(snowflake.generateId(1000).asString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to generate video ID", e);
        }
        video.setTitle(videoDTO.getTitle());
        video.setDescription(videoDTO.getDescription());
        video.setUrl(videoDTO.getUrl());
        video.setThumbnailUrl(videoDTO.getThumbnailUrl());
        video.setDuration(videoDTO.getDuration());
        video.setViewCount(videoDTO.getViewCount() != null ? videoDTO.getViewCount() : 0L);
        video.setUser(user);

        return convertToDTO(videoRepository.save(video));
    }

    @Override
    @Transactional
    public VideoDTO updateVideo(String videoId, VideoDTO videoDTO) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        video.setTitle(videoDTO.getTitle());
        video.setDescription(videoDTO.getDescription());
        video.setUrl(videoDTO.getUrl());
        video.setThumbnailUrl(videoDTO.getThumbnailUrl());
        video.setDuration(videoDTO.getDuration());
        if (videoDTO.getViewCount() != null) {
            video.setViewCount(videoDTO.getViewCount());
        }

        return convertToDTO(videoRepository.save(video));
    }

    @Override
    @Transactional
    public void deleteVideo(String videoId) {
        videoRepository.deleteById(videoId);
    }

    @Override
    public List<VideoDTO> getVideosByUserId(String userId) {
        return videoRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VideoDTO> searchVideosByTitle(String title) {
        return videoRepository.findByTitleContainingIgnoreCase(title).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private VideoDTO convertToDTO(Video video) {
        VideoDTO dto = new VideoDTO();
        dto.setVideoId(video.getVideoId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setUrl(video.getUrl());
        dto.setThumbnailUrl(video.getThumbnailUrl());
        dto.setDuration(video.getDuration());
        dto.setViewCount(video.getViewCount());
        dto.setUserId(video.getUser().getUserId());
        return dto;
    }
} 