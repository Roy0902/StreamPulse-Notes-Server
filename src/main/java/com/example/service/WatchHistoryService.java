package com.example.service;

import com.example.dto.WatchHistoryDTO;

import java.util.List;

public interface WatchHistoryService {
    List<WatchHistoryDTO> getAllHistory();
    WatchHistoryDTO getHistoryById(String historyId);
    WatchHistoryDTO createHistory(WatchHistoryDTO historyDTO);
    WatchHistoryDTO updateHistory(String historyId, WatchHistoryDTO historyDTO);
    void deleteHistory(String historyId);
    List<WatchHistoryDTO> getHistoryByUserId(String userId);
    WatchHistoryDTO updateProgress(String userId, String videoId, Integer progress);
    WatchHistoryDTO markAsCompleted(String userId, String videoId);
} 