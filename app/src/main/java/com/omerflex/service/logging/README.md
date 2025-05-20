# OmerFlex Logging and Error Handling System

This document provides an overview of the logging and error handling system implemented in the OmerFlex application. The system is designed to provide consistent logging and error handling throughout the app, making it easier to track issues, debug problems, and provide appropriate user feedback.

## Table of Contents

1. [Logging System](#logging-system)
2. [Error Handling System](#error-handling-system)
3. [Network Error Handling](#network-error-handling)
4. [Usage Examples](#usage-examples)
5. [Best Practices](#best-practices)

## Logging System

The logging system is implemented in the `Logger` class and provides a centralized way to log messages with different log levels. It wraps Android's `Log` class and adds additional features like log filtering, log levels, and log formatting.

### Log Levels

The following log levels are available:

- `VERBOSE`: Detailed information for debugging
- `DEBUG`: Debugging information
- `INFO`: General information
- `WARN`: Warning messages
- `ERROR`: Error messages
- `NONE`: No logging

### Key Features

- **Log Filtering**: Only messages with a log level equal to or higher than the current log level are logged.
- **Caller Information**: Logs can include the calling method and line number for easier debugging.
- **Consistent Formatting**: All logs follow a consistent format for easier parsing and analysis.

### Usage

```java
// Log a debug message
Logger.d(TAG, "This is a debug message");

// Log an info message
Logger.i(TAG, "This is an info message");

// Log a warning message
Logger.w(TAG, "This is a warning message");

// Log an error message
Logger.e(TAG, "This is an error message");

// Log an exception with an error message
Logger.e(TAG, "An error occurred", exception);
```

## Error Handling System

The error handling system is implemented in the `ErrorHandler` class and provides a centralized way to handle errors with proper logging and user feedback.

### Error Types

The following error types are available:

- `NETWORK_ERROR`: Network-related errors
- `DATABASE_ERROR`: Database-related errors
- `PARSING_ERROR`: Data parsing errors
- `PLAYBACK_ERROR`: Video playback errors
- `GENERAL_ERROR`: Other general errors

### Severity Levels

The following severity levels are available:

- `SEVERITY_LOW`: Non-critical errors that don't affect the user experience
- `SEVERITY_MEDIUM`: Important errors that affect some functionality
- `SEVERITY_HIGH`: Critical errors that prevent the app from functioning properly

### Key Features

- **Consistent Error Handling**: All errors are handled in a consistent way throughout the app.
- **User Feedback**: Appropriate user feedback is provided based on the error type and severity.
- **Error Recovery**: Recovery actions are performed based on the error type and severity.
- **Detailed Logging**: All errors are logged with detailed information for debugging.

### Usage

```java
// Handle a general error
ErrorHandler.handleError(context, ErrorHandler.GENERAL_ERROR, "An error occurred", exception);

// Handle a network error with high severity
ErrorHandler.handleError(context, ErrorHandler.NETWORK_ERROR, ErrorHandler.SEVERITY_HIGH, 
        "Failed to connect to the server", exception);
```

## Network Error Handling

The network error handling system is implemented in the `NetworkErrorHandler` class and provides specialized handling for network-related errors.

### Network Error Subtypes

The following network error subtypes are available:

- `CONNECTION_ERROR`: Connection-related errors
- `TIMEOUT_ERROR`: Timeout errors
- `SERVER_ERROR`: Server-related errors
- `UNKNOWN_HOST_ERROR`: Unknown host errors
- `GENERAL_NETWORK_ERROR`: Other network errors

### Key Features

- **Automatic Retry**: Network operations can be automatically retried with exponential backoff.
- **Network Connectivity Checking**: Network connectivity is checked before attempting network operations.
- **Detailed Error Information**: Network errors include detailed information about the specific error type.
- **User-Friendly Messages**: User-friendly error messages are provided based on the specific error type.

### Usage

```java
// Handle a network error
NetworkErrorHandler.handleNetworkError(context, "Failed to load data", exception);

// Execute a network request with automatic retry
Response response = NetworkErrorHandler.executeWithRetry(request, context);
```

## Usage Examples

### Basic Logging

```java
private static final String TAG = "MyClass";

public void myMethod() {
    Logger.d(TAG, "Starting myMethod");
    
    try {
        // Do something
        Logger.i(TAG, "Operation completed successfully");
    } catch (Exception e) {
        Logger.e(TAG, "Error in myMethod", e);
    }
}
```

### Error Handling

```java
private static final String TAG = "MyClass";

public void loadData() {
    Logger.d(TAG, "Loading data");
    
    try {
        // Load data from network
        Logger.i(TAG, "Data loaded successfully");
    } catch (IOException e) {
        ErrorHandler.handleError(context, ErrorHandler.NETWORK_ERROR, 
                "Failed to load data", e);
    } catch (Exception e) {
        ErrorHandler.handleError(context, ErrorHandler.GENERAL_ERROR, 
                "An unexpected error occurred", e);
    }
}
```

### Network Error Handling

```java
private static final String TAG = "MyClass";

public void fetchDataFromNetwork() {
    Logger.d(TAG, "Fetching data from network");
    
    try {
        // Create request
        Request request = new Request.Builder()
                .url("https://example.com/api/data")
                .build();
        
        // Execute request with automatic retry
        Response response = NetworkErrorHandler.executeWithRetry(request, context);
        
        if (response != null && response.isSuccessful()) {
            Logger.i(TAG, "Data fetched successfully");
            // Process response
        } else {
            Logger.w(TAG, "Failed to fetch data");
        }
    } catch (Exception e) {
        NetworkErrorHandler.handleNetworkError(context, 
                "Failed to fetch data from network", e);
    }
}
```

## Best Practices

1. **Use Appropriate Log Levels**: Use the appropriate log level for each message to ensure that logs are useful and not too verbose.
2. **Include Contextual Information**: Include relevant contextual information in log messages to make them more useful for debugging.
3. **Handle Errors at the Appropriate Level**: Handle errors at the level where you have enough context to provide meaningful recovery actions and user feedback.
4. **Provide User-Friendly Error Messages**: Always provide user-friendly error messages that help the user understand what went wrong and what they can do about it.
5. **Log All Exceptions**: Always log exceptions with detailed information to make debugging easier.
6. **Use Try-Catch Blocks Appropriately**: Use try-catch blocks to handle exceptions at the appropriate level, but don't catch exceptions that you can't handle properly.
7. **Check Network Connectivity**: Always check network connectivity before attempting network operations to provide a better user experience.
8. **Implement Retry Mechanisms**: Implement retry mechanisms for operations that might fail temporarily, like network operations.