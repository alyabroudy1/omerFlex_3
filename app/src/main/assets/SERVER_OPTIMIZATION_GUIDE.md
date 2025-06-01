# OmerFlex Server Optimization Guide

## Overview

This guide explains how to optimize server implementations in OmerFlex to improve performance,
reduce code duplication, and make maintenance easier.

## Key Components

### 1. AbstractServer

The base class with common functionality for all servers including:

- Document fetching and caching
- Error handling
- Threading and concurrency management
- Helper methods for common tasks

### 2. ServerOptimizer

Centralized utility for efficient network operations:

- Connection pooling
- Response caching
- Redirect handling
- Shared resources

### 3. ServerUtility

Helper methods for URL and domain manipulation:

- Domain extraction
- URL resolution
- Protocol handling

## Best Practices

### Use Caching

```java
// Use the ServerOptimizer for document fetching
Document doc = ServerOptimizer.getDocumentWithCache(url, getConfig());
```

### Handle Security Checks

```java
// Use the helper method for security check detection
if (handleSecurityCheck(doc, movie, activityCallback)) {
    return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE, movie);
}
```

### Safe Sublist Management

```java
// Add items safely to movie sublists
safeAddToSublist(parentMovie, childMovie);
```

### JavaScript Injection

```java
// Create data extraction scripts consistently
String script = createDataExtractionScript(movie, ".selector", 
    "let extractedUrl = '';" +
    "if(elements.length > 0){" +
    "    extractedUrl = elements[0].getAttribute('src');" +
    "}");
```

### Thread Management

```java
// Let AbstractServer handle threading
@Override
protected boolean shouldExecuteAsynchronously(int action) {
    // Return true for CPU-intensive or network operations
    return action != Movie.COOKIE_STATE;
}
```

## Performance Tips

1. **Minimize Network Requests**
    - Cache results when possible
    - Use OkHttp for connection pooling
    - Share connections between similar requests

2. **Optimize HTML Parsing**
    - Use CSS selectors instead of traversing the DOM
    - Only extract the data you need
    - Cache parsed results

3. **Reduce Memory Usage**
    - Release resources after use
    - Use weak references for non-critical caches
    - Clear expired cache entries

4. **Improve Responsiveness**
    - Execute heavy operations asynchronously
    - Show feedback while loading
    - Prioritize UI thread work

## Example: Creating a New Server

```java
public class MyCustomServer extends AbstractServer {

    public MyCustomServer() {
        initialize(ServerConfigManager.getContext());
    }
    
    @Override
    protected String getSearchUrl(String query) {
        return "https://example.com/search?q=" + query;
    }
    
    @Override
    protected ArrayList<Movie> getSearchMovieList(Document doc) {
        ArrayList<Movie> results = new ArrayList<>();
        // Extract movies from document
        Elements items = doc.select(".movie-item");
        for (Element item : items) {
            Movie movie = new Movie();
            movie.setTitle(item.select(".title").text());
            movie.setVideoUrl(item.select("a").attr("href"));
            results.add(movie);
        }
        return results;
    }
    
    // Implement other required methods
}
```

## Troubleshooting

### Common Issues

1. **Network Failures**
    - Check device connectivity
    - Verify server URL is correct
    - Ensure headers and cookies are properly set

2. **Parsing Errors**
    - Check if HTML structure changed
    - Update selectors to match new structure
    - Log sample responses for debugging

3. **Memory Leaks**
    - Avoid static references to activities
    - Clear caches when appropriate
    - Use WeakReferences for contexts

### Monitoring

Use the built-in monitoring tools:

```java
Map<String, Object> stats = ServerOptimizer.getStatistics();
Logger.d(TAG, "Cache stats: " + stats);
```

## Conclusion

By following these guidelines, server implementations will be more performant, reliable, and easier
to maintain. Always test thoroughly after making optimizations to ensure everything works as
expected.