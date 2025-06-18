package com.example.dto;

import lombok.Data;

@Data
public class VideoDTO {
    private String youtubeId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private Integer duration;
    private String channelTitle;
    private String channelId;
} 