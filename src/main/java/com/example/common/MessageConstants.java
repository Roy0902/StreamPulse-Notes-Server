package com.example.common;

public final class MessageConstants {
    private MessageConstants() {
        // Prevent instantiation
    }

    // Common messages
    public static final String SUCCESS = "Success";
    public static final String NOT_FOUND = "Resource not found";
    public static final String SERVER_ERROR = "Internal server error";
    public static final String BAD_REQUEST = "Invalid request";

    // Authentication messages
    public static final String UNAUTHORIZED = "Unauthorized";
    public static final String FORBIDDEN = "Forbidden";
    public static final String AUTHENTICATION_REQUIRED = "Authentication required";
    public static final String ACCESS_DENIED = "Access denied";
    public static final String INVALID_TOKEN = "Invalid or expired token";
    public static final String MALFORMED_TOKEN = "Malformed token";
    public static final String USER_NOT_FOUND_AUTH = "User not found";
    
    // Specific authentication messages
    public static final String INVALID_EMAIL_OR_PASSWORD = "Invalid email or password";
    public static final String LOGIN_SUCCESSFUL = "Login successful";
    public static final String LOGIN_SUCCESSFUL_REMEMBER_ME = "Login successful - Remember me enabled";
    public static final String REGISTRATION_SUCCESSFUL = "Registration successful";
    public static final String EMAIL_ALREADY_EXISTS = "Email already exists";
    public static final String REGISTRATION_FAILED = "Registration failed";

    // User messages
    public static final String USER_CREATED = "User created successfully";
    public static final String USER_UPDATED = "User updated successfully";
    public static final String USER_DELETED = "User deleted successfully";
    public static final String USER_NOT_FOUND = "User not found with ID: %s";
    public static final String USER_CREATE_ERROR = "Failed to create user: %s";
    public static final String USER_UPDATE_ERROR = "Failed to update user: %s";
    public static final String USER_DELETE_ERROR = "Failed to delete user: %s";
    public static final String USER_RETRIEVE_ERROR = "Failed to retrieve users: %s";
    public static final String USER_EMAIL_VERIFICATION_FAILED = "User email verification failed";
    public static final String EMAIL_VERIFICATION_FAILED = "Email verification failed";
    public static final String EMAIL_NOT_VERIFIED = "Please verify your email address before logging in.";
    public static final String ACCOUNT_REVOKED = "Your account has been revoked. Please contact support.";

    // Video messages
    public static final String VIDEO_CREATED = "Video created successfully";
    public static final String VIDEO_UPDATED = "Video updated successfully";
    public static final String VIDEO_DELETED = "Video deleted successfully";
    public static final String VIDEO_NOT_FOUND = "Video not found with ID: %s";
    public static final String VIDEO_YOUTUBE_NOT_FOUND = "Video not found with YouTube ID: %s";
    public static final String VIDEO_CREATE_ERROR = "Failed to create video: %s";
    public static final String VIDEO_UPDATE_ERROR = "Failed to update video: %s";
    public static final String VIDEO_DELETE_ERROR = "Failed to delete video: %s";
    public static final String VIDEO_RETRIEVE_ERROR = "Failed to retrieve videos: %s";
    public static final String VIDEO_CHANNEL_RETRIEVE_ERROR = "Failed to retrieve videos for channel: %s";

    // Library messages
    public static final String LIBRARY_ADDED = "Video added to library successfully";
    public static final String LIBRARY_REMOVED = "Video removed from library successfully";
    public static final String LIBRARY_STATUS_UPDATED = "Video status updated successfully";
    public static final String LIBRARY_PROGRESS_UPDATED = "Video progress updated successfully";
    public static final String LIBRARY_NOT_FOUND = "Library entry not found with ID: %s";
    public static final String LIBRARY_RETRIEVE_ERROR = "Failed to retrieve user library: %s";
    public static final String LIBRARY_ADD_ERROR = "Failed to add video to library: %s";
    public static final String LIBRARY_STATUS_ERROR = "Failed to update video status: %s";
    public static final String LIBRARY_PROGRESS_ERROR = "Failed to update video progress: %s";

    // History messages
    public static final String HISTORY_ADDED = "Video added to history successfully";
    public static final String HISTORY_REMOVED = "Video removed from history successfully";
    public static final String HISTORY_CLEARED = "User history cleared successfully";
    public static final String HISTORY_NOT_FOUND = "History entry not found with ID: %s";
    public static final String HISTORY_RETRIEVE_ERROR = "Failed to retrieve user history: %s";
    public static final String HISTORY_ADD_ERROR = "Failed to add video to history: %s";
    public static final String HISTORY_CLEAR_ERROR = "Failed to clear user history: %s";

    //Log Header
    public static final String HEADER_LOGIN_FAILED = "LOGIN_FAILED";    
    public static final String HEADER_REDIS_CONNECTION_ERROR = "REDIS_CONNECTION_ERROR";
} 