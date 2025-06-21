package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "watch_history")
@Data
@EntityListeners(AuditingEntityListener.class)
public class WatchHistory {
    @Id
    @Column(name = "history_id", length = 30)
    private String historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "progress", nullable = false)
    private Integer progress = 0; // in seconds

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "last_watched_at", nullable = false)
    private LocalDateTime lastWatchedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
} 