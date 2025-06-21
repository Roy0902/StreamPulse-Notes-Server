package com.example.service.impl;

import com.example.dto.VideoLibraryDTO;
import com.example.entity.User;
import com.example.entity.Video;
import com.example.entity.VideoLibrary;
import com.example.repository.UserRepository;
import com.example.repository.VideoLibraryRepository;
import com.example.repository.VideoRepository;
import com.example.service.VideoLibraryService;
import com.github.rholder.fauxflake.IdGenerators;
import com.github.rholder.fauxflake.api.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoLibraryServiceImpl implements VideoLibraryService {
    private final VideoLibraryRepository libraryRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final IdGenerator snowflake = IdGenerators.newSnowflakeIdGenerator();

    @Override
    public List<VideoLibraryDTO> getAllLibraries() {
        return libraryRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public VideoLibraryDTO getLibraryById(String libraryId) {
        VideoLibrary library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library not found"));
        return convertToDTO(library);
    }

    @Override
    @Transactional
    public VideoLibraryDTO createLibrary(VideoLibraryDTO libraryDTO) {
        User user = userRepository.findById(libraryDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        VideoLibrary library = new VideoLibrary();
        try {
            library.setLibraryId(snowflake.generateId(1000).asString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to generate library ID", e);
        }
        library.setName(libraryDTO.getName());
        library.setDescription(libraryDTO.getDescription());
        library.setIsPublic(libraryDTO.getIsPublic() != null ? libraryDTO.getIsPublic() : false);
        library.setUser(user);

        return convertToDTO(libraryRepository.save(library));
    }

    @Override
    @Transactional
    public VideoLibraryDTO updateLibrary(String libraryId, VideoLibraryDTO libraryDTO) {
        VideoLibrary library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library not found"));

        library.setName(libraryDTO.getName());
        library.setDescription(libraryDTO.getDescription());
        if (libraryDTO.getIsPublic() != null) {
            library.setIsPublic(libraryDTO.getIsPublic());
        }

        return convertToDTO(libraryRepository.save(library));
    }

    @Override
    @Transactional
    public void deleteLibrary(String libraryId) {
        libraryRepository.deleteById(libraryId);
    }

    @Override
    public List<VideoLibraryDTO> getLibrariesByUserId(String userId) {
        return libraryRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VideoLibraryDTO> getPublicLibraries() {
        return libraryRepository.findByIsPublicTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VideoLibraryDTO addVideoToLibrary(String libraryId, String videoId) {
        VideoLibrary library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library not found"));
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        if (!library.getVideos().contains(video)) {
            library.getVideos().add(video);
            libraryRepository.save(library);
        }

        return convertToDTO(library);
    }

    @Override
    @Transactional
    public VideoLibraryDTO removeVideoFromLibrary(String libraryId, String videoId) {
        VideoLibrary library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library not found"));
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        library.getVideos().remove(video);
        libraryRepository.save(library);

        return convertToDTO(library);
    }

    private VideoLibraryDTO convertToDTO(VideoLibrary library) {
        VideoLibraryDTO dto = new VideoLibraryDTO();
        dto.setLibraryId(library.getLibraryId());
        dto.setName(library.getName());
        dto.setDescription(library.getDescription());
        dto.setIsPublic(library.getIsPublic());
        dto.setUserId(library.getUser().getUserId());
        dto.setVideoIds(library.getVideos().stream()
                .map(Video::getVideoId)
                .collect(Collectors.toList()));
        return dto;
    }
} 