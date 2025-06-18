package com.example.dto;

import lombok.Data;

@Data
public class VideoLibraryDTO {
    private VideoDTO video;
    private String status;
    private Integer progress;
} 