package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class VideoLibraryDTO {
    private String libraryId;
    
    @NotBlank(message = "Library name is required")
    private String name;
    
    private String description;
    private Boolean isPublic;
    private String userId;
    private List<String> videoIds;
} 