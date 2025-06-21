package com.example.service;

import com.example.dto.VideoLibraryDTO;

import java.util.List;

public interface VideoLibraryService {
    List<VideoLibraryDTO> getAllLibraries();
    VideoLibraryDTO getLibraryById(String libraryId);
    VideoLibraryDTO createLibrary(VideoLibraryDTO libraryDTO);
    VideoLibraryDTO updateLibrary(String libraryId, VideoLibraryDTO libraryDTO);
    void deleteLibrary(String libraryId);
    List<VideoLibraryDTO> getLibrariesByUserId(String userId);
    List<VideoLibraryDTO> getPublicLibraries();
    VideoLibraryDTO addVideoToLibrary(String libraryId, String videoId);
    VideoLibraryDTO removeVideoFromLibrary(String libraryId, String videoId);
} 