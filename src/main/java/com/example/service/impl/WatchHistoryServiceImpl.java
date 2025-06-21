package com.example.service.impl;

import com.example.dto.WatchHistoryDTO;
import com.example.entity.User;
import com.example.entity.Video;
import com.example.entity.WatchHistory;
import com.example.repository.UserRepository;
import com.example.repository.VideoRepository;
import com.example.repository.WatchHistoryRepository;
import com.example.service.WatchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchHistoryServiceImpl implements WatchHistoryService {
    private final WatchHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    @Override
    public List<WatchHistoryDTO> getAllHistory() {
        return historyRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public WatchHistoryDTO getHistoryById(String historyId) {
        WatchHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("History not found"));
        return convertToDTO(history);
    }

    @Override
    @Transactional
    public WatchHistoryDTO createHistory(WatchHistoryDTO historyDTO) {
        User user = userRepository.findById(historyDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Video video = videoRepository.findById(historyDTO.getVideoId())
                .orElseThrow(() -> new RuntimeException("Video not found"));

        WatchHistory history = new WatchHistory();
        history.setHistoryId(UUID.randomUUID().toString());
        history.setUser(user);
        history.setVideo(video);
        history.setProgress(historyDTO.getProgress() != null ? historyDTO.getProgress() : 0);
        history.setCompleted(historyDTO.getCompleted() != null ? historyDTO.getCompleted() : false);
        history.setLastWatchedAt(historyDTO.getLastWatchedAt() != null ? historyDTO.getLastWatchedAt() : LocalDateTime.now());

        return convertToDTO(historyRepository.save(history));
    }

    @Override
    @Transactional
    public WatchHistoryDTO updateHistory(String historyId, WatchHistoryDTO historyDTO) {
        WatchHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("History not found"));

        if (historyDTO.getProgress() != null) {
            history.setProgress(historyDTO.getProgress());
        }
        if (historyDTO.getCompleted() != null) {
            history.setCompleted(historyDTO.getCompleted());
        }
        history.setLastWatchedAt(LocalDateTime.now());

        return convertToDTO(historyRepository.save(history));
    }

    @Override
    @Transactional
    public void deleteHistory(String historyId) {
        historyRepository.deleteById(historyId);
    }

    @Override
    public List<WatchHistoryDTO> getHistoryByUserId(String userId) {
        return historyRepository.findByUserIdOrderByLastWatchedAtDesc(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WatchHistoryDTO updateProgress(String userId, String videoId, Integer progress) {
        WatchHistory history = historyRepository.findByUserIdAndVideoId(userId, videoId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    Video video = videoRepository.findById(videoId)
                            .orElseThrow(() -> new RuntimeException("Video not found"));

                    WatchHistory newHistory = new WatchHistory();
                    newHistory.setHistoryId(UUID.randomUUID().toString());
                    newHistory.setUser(user);
                    newHistory.setVideo(video);
                    newHistory.setProgress(0);
                    newHistory.setCompleted(false);
                    newHistory.setLastWatchedAt(LocalDateTime.now());
                    return historyRepository.save(newHistory);
                });

        history.setProgress(progress);
        history.setLastWatchedAt(LocalDateTime.now());
        return convertToDTO(historyRepository.save(history));
    }

    @Override
    @Transactional
    public WatchHistoryDTO markAsCompleted(String userId, String videoId) {
        WatchHistory history = historyRepository.findByUserIdAndVideoId(userId, videoId)
                .orElseThrow(() -> new RuntimeException("History not found"));

        history.setCompleted(true);
        history.setLastWatchedAt(LocalDateTime.now());
        return convertToDTO(historyRepository.save(history));
    }

    private WatchHistoryDTO convertToDTO(WatchHistory history) {
        WatchHistoryDTO dto = new WatchHistoryDTO();
        dto.setHistoryId(history.getHistoryId());
        dto.setUserId(history.getUser().getUserId());
        dto.setVideoId(history.getVideo().getVideoId());
        dto.setProgress(history.getProgress());
        dto.setCompleted(history.getCompleted());
        dto.setLastWatchedAt(history.getLastWatchedAt());
        return dto;
    }
} 