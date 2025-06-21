package com.example.controller;

import com.example.common.ApiResponse;
import com.example.dto.VideoLibraryDTO;
import com.example.service.VideoLibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/libraries")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VideoLibraryController {
    private final VideoLibraryService libraryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VideoLibraryDTO>>> getAllLibraries() {
        List<VideoLibraryDTO> libraries = libraryService.getAllLibraries();
        return ResponseEntity.ok(ApiResponse.success("Libraries retrieved successfully", libraries));
    }

    @GetMapping("/{libraryId}")
    public ResponseEntity<ApiResponse<VideoLibraryDTO>> getLibraryById(@PathVariable String libraryId) {
        try {
            VideoLibraryDTO library = libraryService.getLibraryById(libraryId);
            return ResponseEntity.ok(ApiResponse.success("Library retrieved successfully", library));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VideoLibraryDTO>> createLibrary(@Valid @RequestBody VideoLibraryDTO libraryDTO) {
        VideoLibraryDTO createdLibrary = libraryService.createLibrary(libraryDTO);
        return ResponseEntity.ok(ApiResponse.success("Library created successfully", createdLibrary));
    }

    @PutMapping("/{libraryId}")
    public ResponseEntity<ApiResponse<VideoLibraryDTO>> updateLibrary(@PathVariable String libraryId, @Valid @RequestBody VideoLibraryDTO libraryDTO) {
        try {
            VideoLibraryDTO updatedLibrary = libraryService.updateLibrary(libraryId, libraryDTO);
            return ResponseEntity.ok(ApiResponse.success("Library updated successfully", updatedLibrary));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{libraryId}")
    public ResponseEntity<ApiResponse<Void>> deleteLibrary(@PathVariable String libraryId) {
        try {
            libraryService.deleteLibrary(libraryId);
            return ResponseEntity.ok(ApiResponse.success("Library deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<VideoLibraryDTO>>> getLibrariesByUserId(@PathVariable String userId) {
        List<VideoLibraryDTO> libraries = libraryService.getLibrariesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("User libraries retrieved successfully", libraries));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<VideoLibraryDTO>>> getPublicLibraries() {
        List<VideoLibraryDTO> libraries = libraryService.getPublicLibraries();
        return ResponseEntity.ok(ApiResponse.success("Public libraries retrieved successfully", libraries));
    }

    @PostMapping("/{libraryId}/videos/{videoId}")
    public ResponseEntity<ApiResponse<VideoLibraryDTO>> addVideoToLibrary(@PathVariable String libraryId, @PathVariable String videoId) {
        try {
            VideoLibraryDTO library = libraryService.addVideoToLibrary(libraryId, videoId);
            return ResponseEntity.ok(ApiResponse.success("Video added to library successfully", library));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{libraryId}/videos/{videoId}")
    public ResponseEntity<ApiResponse<VideoLibraryDTO>> removeVideoFromLibrary(@PathVariable String libraryId, @PathVariable String videoId) {
        try {
            VideoLibraryDTO library = libraryService.removeVideoFromLibrary(libraryId, videoId);
            return ResponseEntity.ok(ApiResponse.success("Video removed from library successfully", library));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
} 