package com.example.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WatchHistoryDTO {
    private String historyId;
    private String userId;
    private String videoId;
    private Integer progress;
    private Boolean completed;
    private LocalDateTime lastWatchedAt;
} 