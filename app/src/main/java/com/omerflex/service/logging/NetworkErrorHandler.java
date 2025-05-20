package com.omerflex.service.logging;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Specialized error handler for network-related errors.
 * Provides methods for handling common network issues like timeouts, connection errors,
 * and server errors.
 */
public class NetworkErrorHandler {
    private static final String TAG = "NetworkErrorHandler";
    
    // Network error subtypes
    public static final int CONNECTION_ERROR = 101;
    public static final int TIMEOUT_ERROR = 102;
    public static final int SERVER_ERROR = 103;
    public static final int UNKNOWN_HOST_ERROR = 104;
    public static final int GENERAL_NETWORK_ERROR = 105;
    
    // Default timeout values
    private static final int DEFAULT_CONNECT_TIMEOUT = 15;
    private static final int DEFAULT_READ_TIMEOUT = 30;
    private static final int DEFAULT_WRITE_TIMEOUT = 30;
    
    // Maximum number of retry attempts
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    /**
     * Handle a network error with appropriate logging and user feedback
     * @param context The context
     * @param message The error message
     * @param throwable The exception (can be null)
     */
    public static void handleNetworkError(Context context, String message, @Nullable Throwable throwable) {
        int networkErrorSubtype = determineNetworkErrorSubtype(throwable);
        int severity = determineSeverity(networkErrorSubtype);
        
        // Log and handle the error using the main ErrorHandler
        ErrorHandler.handleError(context, ErrorHandler.NETWORK_ERROR, severity, 
                formatNetworkErrorMessage(message, networkErrorSubtype), throwable);
    }
    
    /**
     * Determine the specific type of network error based on the exception
     * @param throwable The exception
     * @return The network error subtype
     */
    private static int determineNetworkErrorSubtype(@Nullable Throwable throwable) {
        if (throwable == null) {
            return GENERAL_NETWORK_ERROR;
        }
        
        if (throwable instanceof SocketTimeoutException) {
            return TIMEOUT_ERROR;
        } else if (throwable instanceof ConnectException) {
            return CONNECTION_ERROR;
        } else if (throwable instanceof UnknownHostException) {
            return UNKNOWN_HOST_ERROR;
        } else if (throwable instanceof IOException) {
            // Check if it's a server error (could be more specific with HTTP status codes)
            return SERVER_ERROR;
        }
        
        return GENERAL_NETWORK_ERROR;
    }
    
    /**
     * Determine the severity level based on the network error subtype
     * @param networkErrorSubtype The network error subtype
     * @return The severity level
     */
    private static int determineSeverity(int networkErrorSubtype) {
        switch (networkErrorSubtype) {
            case CONNECTION_ERROR:
            case UNKNOWN_HOST_ERROR:
                // These are likely due to no internet connection, which is a high severity issue
                return ErrorHandler.SEVERITY_HIGH;
                
            case TIMEOUT_ERROR:
                // Timeouts might be temporary, medium severity
                return ErrorHandler.SEVERITY_MEDIUM;
                
            case SERVER_ERROR:
                // Server errors might be temporary, medium severity
                return ErrorHandler.SEVERITY_MEDIUM;
                
            case GENERAL_NETWORK_ERROR:
            default:
                // Default to medium severity
                return ErrorHandler.SEVERITY_MEDIUM;
        }
    }
    
    /**
     * Format a network error message with additional information
     * @param message The original error message
     * @param networkErrorSubtype The network error subtype
     * @return The formatted error message
     */
    @NonNull
    private static String formatNetworkErrorMessage(String message, int networkErrorSubtype) {
        String errorTypeStr = getNetworkErrorSubtypeString(networkErrorSubtype);
        return String.format("[%s] %s", errorTypeStr, message);
    }
    
    /**
     * Get a string representation of the network error subtype
     * @param networkErrorSubtype The network error subtype
     * @return The string representation
     */
    @NonNull
    private static String getNetworkErrorSubtypeString(int networkErrorSubtype) {
        switch (networkErrorSubtype) {
            case CONNECTION_ERROR:
                return "Connection Error";
            case TIMEOUT_ERROR:
                return "Timeout";
            case SERVER_ERROR:
                return "Server Error";
            case UNKNOWN_HOST_ERROR:
                return "Unknown Host";
            case GENERAL_NETWORK_ERROR:
            default:
                return "Network Error";
        }
    }
    
    /**
     * Check if the device has an active network connection
     * @param context The context
     * @return True if connected, false otherwise
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }
        
        ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return false;
        }
        
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    /**
     * Execute a network request with automatic retry for certain error types
     * @param request The OkHttp request to execute
     * @param context The context (for error handling)
     * @return The response, or null if the request failed after retries
     */
    @Nullable
    public static Response executeWithRetry(Request request, Context context) {
        return executeWithRetry(request, context, MAX_RETRY_ATTEMPTS);
    }
    
    /**
     * Execute a network request with automatic retry for certain error types
     * @param request The OkHttp request to execute
     * @param context The context (for error handling)
     * @param maxRetries Maximum number of retry attempts
     * @return The response, or null if the request failed after retries
     */
    @Nullable
    public static Response executeWithRetry(Request request, Context context, int maxRetries) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build();
        
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                // Check for network connectivity before attempting the request
                if (!isNetworkAvailable(context)) {
                    Logger.w(TAG, "No network connection available, waiting before retry...");
                    Thread.sleep(1000); // Wait a second before checking again
                    attempts++;
                    continue;
                }
                
                // Execute the request
                Response response = client.newCall(request).execute();
                
                // If successful, return the response
                if (response.isSuccessful()) {
                    return response;
                } else {
                    // Handle server errors (4xx, 5xx)
                    handleNetworkError(context, 
                            "Server returned error code: " + response.code(), 
                            new IOException("HTTP " + response.code() + " " + response.message()));
                    
                    // For 5xx errors, retry; for 4xx errors, don't retry (client error)
                    if (response.code() >= 500 && response.code() < 600) {
                        Logger.w(TAG, "Server error, retrying... (" + (attempts + 1) + "/" + maxRetries + ")");
                        attempts++;
                        Thread.sleep(1000 * attempts); // Exponential backoff
                        continue;
                    } else {
                        // Don't retry for 4xx errors
                        return response;
                    }
                }
            } catch (SocketTimeoutException e) {
                // Handle timeout errors
                handleNetworkError(context, "Request timed out", e);
                Logger.w(TAG, "Timeout, retrying... (" + (attempts + 1) + "/" + maxRetries + ")");
                attempts++;
                try {
                    Thread.sleep(1000 * attempts); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    handleNetworkError(context, "Retry interrupted", ie);
                    return null;
                }
            } catch (IOException e) {
                // Handle other IO errors
                handleNetworkError(context, "Network error: " + e.getMessage(), e);
                
                // Retry for connection errors and unknown host errors
                if (e instanceof ConnectException || e instanceof UnknownHostException) {
                    Logger.w(TAG, "Connection error, retrying... (" + (attempts + 1) + "/" + maxRetries + ")");
                    attempts++;
                    try {
                        Thread.sleep(1000 * attempts); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        handleNetworkError(context, "Retry interrupted", ie);
                        return null;
                    }
                } else {
                    // Don't retry for other IO errors
                    return null;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                handleNetworkError(context, "Retry interrupted", e);
                return null;
            }
        }
        
        // If we've exhausted all retries, log and return null
        handleNetworkError(context, "Request failed after " + maxRetries + " attempts", null);
        return null;
    }
}