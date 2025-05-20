package com.omerflex.service.logging;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Centralized error handling system for OmerFlex.
 * Provides consistent error handling throughout the app with support for different error types,
 * error recovery, and user feedback.
 */
public class ErrorHandler {
    private static final String TAG = "ErrorHandler";

    // Error types
    public static final int NETWORK_ERROR = 1;
    public static final int DATABASE_ERROR = 2;
    public static final int PARSING_ERROR = 3;
    public static final int PLAYBACK_ERROR = 4;
    public static final int GENERAL_ERROR = 5;

    // Error severity levels
    public static final int SEVERITY_LOW = 1;    // Non-critical, can continue
    public static final int SEVERITY_MEDIUM = 2; // Important but not fatal
    public static final int SEVERITY_HIGH = 3;   // Critical, needs immediate attention

    /**
     * Handle an error with default severity (MEDIUM)
     * @param context The context
     * @param errorType The type of error
     * @param message The error message
     * @param throwable The exception (can be null)
     */
    public static void handleError(Context context, int errorType, String message, @Nullable Throwable throwable) {
        handleError(context, errorType, SEVERITY_MEDIUM, message, throwable);
    }

    /**
     * Handle an error with specified severity
     * @param context The context
     * @param errorType The type of error
     * @param severity The severity level
     * @param message The error message
     * @param throwable The exception (can be null)
     */
    public static void handleError(Context context, int errorType, int severity, String message, @Nullable Throwable throwable) {
        // Log the error
        if (throwable != null) {
            Logger.e(TAG, formatErrorMessage(errorType, severity, message), throwable);
        } else {
            Logger.e(TAG, formatErrorMessage(errorType, severity, message));
        }

        // Show user feedback based on severity
        if (context != null) {
            showUserFeedback(context, errorType, severity, message);
        }

        // Perform recovery actions based on error type and severity
        performRecoveryActions(context, errorType, severity, throwable);
    }

    /**
     * Format the error message with type and severity information
     * @param errorType The type of error
     * @param severity The severity level
     * @param message The error message
     * @return The formatted error message
     */
    private static String formatErrorMessage(int errorType, int severity, String message) {
        String errorTypeStr = getErrorTypeString(errorType);
        String severityStr = getSeverityString(severity);
        return String.format("[%s][%s] %s", errorTypeStr, severityStr, message);
    }

    /**
     * Get a string representation of the error type
     * @param errorType The error type
     * @return The string representation
     */
    private static String getErrorTypeString(int errorType) {
        switch (errorType) {
            case NETWORK_ERROR:
                return "Network";
            case DATABASE_ERROR:
                return "Database";
            case PARSING_ERROR:
                return "Parsing";
            case PLAYBACK_ERROR:
                return "Playback";
            case GENERAL_ERROR:
            default:
                return "General";
        }
    }

    /**
     * Get a string representation of the severity level
     * @param severity The severity level
     * @return The string representation
     */
    private static String getSeverityString(int severity) {
        switch (severity) {
            case SEVERITY_LOW:
                return "Low";
            case SEVERITY_MEDIUM:
                return "Medium";
            case SEVERITY_HIGH:
                return "High";
            default:
                return "Unknown";
        }
    }

    /**
     * Show user feedback based on error type and severity
     * @param context The context
     * @param errorType The type of error
     * @param severity The severity level
     * @param message The error message
     */
    private static void showUserFeedback(Context context, int errorType, int severity, String message) {
        // Only show user feedback for medium and high severity errors
        if (severity >= SEVERITY_MEDIUM) {
            String userMessage = getUserFriendlyMessage(errorType, message);
            
            // Show a toast message
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                activity.runOnUiThread(() -> 
                    Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show()
                );
            } else {
                Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Get a user-friendly error message based on the error type
     * @param errorType The type of error
     * @param message The original error message
     * @return A user-friendly error message
     */
    @NonNull
    private static String getUserFriendlyMessage(int errorType, String message) {
        switch (errorType) {
            case NETWORK_ERROR:
                return "Network connection issue. Please check your internet connection and try again.";
            case DATABASE_ERROR:
                return "There was a problem accessing data. Please try again.";
            case PARSING_ERROR:
                return "There was a problem processing content. Please try again.";
            case PLAYBACK_ERROR:
                return "There was a problem playing the video. Please try again.";
            case GENERAL_ERROR:
            default:
                // For general errors, use the original message if it's user-friendly
                if (message != null && !message.contains("Exception") && !message.contains("Error")) {
                    return message;
                } else {
                    return "An unexpected error occurred. Please try again.";
                }
        }
    }

    /**
     * Perform recovery actions based on error type and severity
     * @param context The context
     * @param errorType The type of error
     * @param severity The severity level
     * @param throwable The exception (can be null)
     */
    private static void performRecoveryActions(Context context, int errorType, int severity, @Nullable Throwable throwable) {
        // Implement recovery strategies based on error type and severity
        switch (errorType) {
            case NETWORK_ERROR:
                // For network errors, we might want to retry the operation
                if (severity == SEVERITY_HIGH) {
                    // For high severity network errors, we might want to show a retry dialog
                    // This would be implemented in the calling code
                }
                break;
                
            case DATABASE_ERROR:
                // For database errors, we might want to try to repair the database
                if (severity == SEVERITY_HIGH) {
                    // For high severity database errors, we might want to reset the database
                    // This would be implemented in the calling code
                }
                break;
                
            case PLAYBACK_ERROR:
                // For playback errors, we might want to try a different player or source
                // This would be implemented in the calling code
                break;
                
            case PARSING_ERROR:
            case GENERAL_ERROR:
            default:
                // For other errors, we might just log and notify the user
                break;
        }
    }
}