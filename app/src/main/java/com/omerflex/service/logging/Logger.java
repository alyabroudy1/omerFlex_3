package com.omerflex.service.logging;

import android.util.Log;

/**
 * Centralized logging system for OmerFlex.
 * Provides consistent logging throughout the app with support for different log levels,
 * exception logging, and log filtering.
 */
public class Logger {
    // Log levels
    public static final int VERBOSE = 1;
    public static final int DEBUG = 2;
    public static final int INFO = 3;
    public static final int WARN = 4;
    public static final int ERROR = 5;
    public static final int NONE = 6;

    // Current log level - can be changed at runtime
    private static int sLogLevel = DEBUG;

    // Whether to include the calling method and line number in the log
    private static boolean sIncludeCallerInfo = true;

    /**
     * Set the minimum log level. Messages below this level will not be logged.
     * @param logLevel The minimum log level
     */
    public static void setLogLevel(int logLevel) {
        sLogLevel = logLevel;
    }

    /**
     * Set whether to include caller information (method name and line number) in logs
     * @param includeCallerInfo Whether to include caller information
     */
    public static void setIncludeCallerInfo(boolean includeCallerInfo) {
        sIncludeCallerInfo = includeCallerInfo;
    }

    /**
     * Log a verbose message
     * @param tag The log tag
     * @param message The message to log
     */
    public static void v(String tag, String message) {
        if (sLogLevel <= VERBOSE) {
            Log.v(tag, formatMessage(message));
        }
    }

    /**
     * Log a debug message
     * @param tag The log tag
     * @param message The message to log
     */
    public static void d(String tag, String message) {
        if (sLogLevel <= DEBUG) {
            Log.d(tag, formatMessage(message));
        }
    }

    /**
     * Log an info message
     * @param tag The log tag
     * @param message The message to log
     */
    public static void i(String tag, String message) {
        if (sLogLevel <= INFO) {
            Log.i(tag, formatMessage(message));
        }
    }

    /**
     * Log a warning message
     * @param tag The log tag
     * @param message The message to log
     */
    public static void w(String tag, String message) {
        if (sLogLevel <= WARN) {
            Log.w(tag, formatMessage(message));
        }
    }

    /**
     * Log an error message
     * @param tag The log tag
     * @param message The message to log
     */
    public static void e(String tag, String message) {
        if (sLogLevel <= ERROR) {
            Log.e(tag, formatMessage(message));
        }
    }

    /**
     * Log an exception with an error message
     * @param tag The log tag
     * @param message The message to log
     * @param throwable The exception to log
     */
    public static void e(String tag, String message, Throwable throwable) {
        if (sLogLevel <= ERROR) {
            Log.e(tag, formatMessage(message), throwable);
        }
    }

    /**
     * Log an exception with a warning message
     * @param tag The log tag
     * @param message The message to log
     * @param throwable The exception to log
     */
    public static void w(String tag, String message, Throwable throwable) {
        if (sLogLevel <= WARN) {
            Log.w(tag, formatMessage(message), throwable);
        }
    }

    /**
     * Format the log message to include caller information if enabled
     * @param message The message to format
     * @return The formatted message
     */
    private static String formatMessage(String message) {
        if (!sIncludeCallerInfo) {
            return message;
        }

        // Get the stack trace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        
        // Find the calling method (index 4 is the method that called this logger)
        if (stackTrace.length >= 5) {
            StackTraceElement caller = stackTrace[4];
            String className = caller.getClassName();
            String methodName = caller.getMethodName();
            int lineNumber = caller.getLineNumber();
            
            // Format the message with caller information
            return String.format("[%s.%s:%d] %s", 
                    className.substring(className.lastIndexOf('.') + 1), 
                    methodName, 
                    lineNumber, 
                    message);
        }
        
        return message;
    }
}