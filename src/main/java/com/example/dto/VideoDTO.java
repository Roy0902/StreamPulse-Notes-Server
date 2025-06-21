package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VideoDTO {
    private String videoId;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotBlank(message = "URL is required")
    private String url;
    
    private String thumbnailUrl;
    private Integer duration;
    private Long viewCount;
    private String userId;
} 